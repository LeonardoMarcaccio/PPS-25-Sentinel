package it.unibo.sentinel.boundary.gui.fx.panels

import it.unibo.sentinel.core.warehouse.{Position, Warehouse}
import scalafx.geometry.Pos
import scalafx.scene.control.Label
import scalafx.scene.layout.{
  ColumnConstraints,
  GridPane,
  Priority,
  RowConstraints,
  StackPane
}
import scalafx.scene.paint.Color
import it.unibo.sentinel.core.robot.value
import it.unibo.sentinel.core.simulation.RobotSnapshot
import it.unibo.sentinel.core.routing.Path
import scala.collection.mutable

/** Panel used to display a [[Warehouse]]
  *
  * @param warehouse
  *   the [[Warehouse]] to display
  */
final class WarehousePanel(warehouse: Warehouse) extends GridPane:

  private val rows: Int = warehouse.height
  private val cols: Int = warehouse.width
  private val robotColors: mutable.Map[String, Color] = mutable.Map.empty

  alignment = Pos.Center
  hgrow = Priority.Always
  vgrow = Priority.Always


  private val cells: Map[Position, (StackPane, Label)] = (
    for
      r <- 0 until rows
      c <- 0 until cols
      pos = Position(c, r)
    yield
      val (node, label) = createCellNode(pos, warehouse.isTraversable(pos))
      add(node, c, r)
      pos -> (node, label)
  ).toMap

  columnConstraints = (0 until cols).map { _ =>
    new ColumnConstraints:
      hgrow = Priority.Always
      percentWidth = 100.0 / cols
  }
  rowConstraints = (0 until rows).map { _ =>
    new RowConstraints:
      vgrow = Priority.Always
      percentHeight = 100.0 / rows
  }

  private def colorForRobot(robotId: String): Color =
    robotColors.getOrElseUpdate(
      robotId, {
        val hue = (robotColors.size * 137.508) % 360.0
        Color.hsb(hue, 0.70, 0.90)
      }
    )

  /** Updates the robots and their paths in the grid */
  def updateRobots(robots: Seq[RobotSnapshot]): Unit =
    cells.values.foreach((_, label) => label.text = "")
    for (pos, (pane, _)) <- cells do
      applyStyle(pane, warehouse.isTraversable(pos))

    // PASS 1: Render paths with transparency
    for robot <- robots do
      val color = colorForRobot(robot.id.value)
      robot.path match
        case Some(path) => showPath(path, toRgba(color, alpha = 0.35))
        case None       => ()

    // PASS 2: Render robot standing positions on top of paths
    for robot <- robots do
      val color = colorForRobot(robot.id.value)
      cells.get(robot.position).foreach { (pane, label) =>
        label.text = robot.id.value
        applyStyle(
          pane,
          warehouse.isTraversable(robot.position),
          customBgColor = Some(toRgba(color, alpha = 1.0)),
          isRobotTile = true
        )
      }

  private def showPath(path: Path, cssColor: String): Unit =
    for pos <- path.positions do
      cells.get(pos).foreach { (pane, _) =>
        applyStyle(pane, warehouse.isTraversable(pos), customBgColor = Some(cssColor))
      }

  private def createCellNode(pos: Position, traversable: Boolean): (StackPane, Label) =
    val textColor = if traversable then "#0F172A" else "#F8FAFC"
    val robotLabel = new Label:
      textFill = Color.web(textColor)
      style = "-fx-font-weight: bold; -fx-font-size: 12px;"

    val pane = new StackPane

    if traversable then
      val costText = warehouse.traversalCost(pos).map(_.toString).getOrElse("")
      val costLabel = new Label:
        text = costText
        textFill = Color.web("#262c35")
        style = "-fx-font-size: 9px; -fx-font-weight: normal; -fx-padding: 0 3px 1px 0;"

      StackPane.setAlignment(costLabel, Pos.BottomRight)
      pane.children = Seq(costLabel, robotLabel)
    else
      pane.children = Seq(robotLabel)

    applyStyle(pane, traversable)
    (pane, robotLabel)

  private def applyStyle(
      pane: StackPane,
      traversable: Boolean,
      customBgColor: Option[String] = None,
      isRobotTile: Boolean = false
  ): Unit =
    val bgColor = customBgColor.getOrElse {
      if traversable then "#F8FAFC" else "#334155"
    }

    val borderColor = if isRobotTile then "#0F172A" else if traversable then "#E0E6ED" else "#1C2739"
    val borderWidth = if isRobotTile then "2px" else "1px"

    pane.style = s"""
      -fx-background-color: $bgColor;
      -fx-border-color: $borderColor;
      -fx-border-width: $borderWidth;
    """

  private def toRgba(c: Color, alpha: Double): String =
    s"rgba(${(c.red * 255).toInt}, ${(c.green * 255).toInt}, ${(c.blue * 255).toInt}, $alpha)"