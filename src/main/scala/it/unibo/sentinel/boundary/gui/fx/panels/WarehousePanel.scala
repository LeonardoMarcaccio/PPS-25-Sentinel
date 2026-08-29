package it.unibo.sentinel.boundary.gui.fx.panels

import it.unibo.sentinel.core.warehouse.{Position, Warehouse}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Label
import scalafx.scene.layout.{
  Background,
  BackgroundFill,
  Border,
  BorderStroke,
  BorderStrokeStyle,
  BorderWidths,
  ColumnConstraints,
  CornerRadii,
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
  private val dirtyCells: mutable.Set[Position] = mutable.Set.empty

  alignment = Pos.Center
  hgrow = Priority.Always
  vgrow = Priority.Always

  private val traversableBg = new Background(
    Array(
      new BackgroundFill(Color.web("#F8FAFC"), CornerRadii.Empty, Insets.Empty)
    )
  )
  private val obstacleBg = new Background(
    Array(
      new BackgroundFill(Color.web("#334155"), CornerRadii.Empty, Insets.Empty)
    )
  )

  private val traversableBorder = new Border(
    new BorderStroke(
      Color.web("#E0E6ED"),
      BorderStrokeStyle.Solid,
      CornerRadii.Empty,
      new BorderWidths(1)
    )
  )
  private val obstacleBorder = new Border(
    new BorderStroke(
      Color.web("#1C2739"),
      BorderStrokeStyle.Solid,
      CornerRadii.Empty,
      new BorderWidths(1)
    )
  )
  private val robotBorder = new Border(
    new BorderStroke(
      Color.web("#0F172A"),
      BorderStrokeStyle.Solid,
      CornerRadii.Empty,
      new BorderWidths(2)
    )
  )

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
    // 1. Reset only the cells that were changed in the previous frame
    for pos <- dirtyCells do
      cells.get(pos).foreach { (pane, label) =>
        label.text = ""
        applyStyle(pane, warehouse.isTraversable(pos))
      }
    dirtyCells.clear()

    // PASS 1: Render paths with transparency
    for
      robot <- robots
      path <- robot.path
    do
      val color = colorForRobot(robot.id.value)
      val pathColor = Color.color(color.red, color.green, color.blue, 0.35)
      showPath(path, pathColor)
      dirtyCells ++= path.positions

    // PASS 2: Render robot standing positions on top of paths
    for robot <- robots do
      val color = colorForRobot(robot.id.value)
      cells.get(robot.position).foreach { (pane, label) =>
        label.text = robot.id.value
        applyStyle(
          pane,
          warehouse.isTraversable(robot.position),
          customBgColor = Some(color),
          isRobotTile = true
        )
        dirtyCells += robot.position
      }

  private def showPath(path: Path, color: Color): Unit =
    for pos <- path.positions do
      cells.get(pos).foreach { (pane, _) =>
        applyStyle(
          pane,
          warehouse.isTraversable(pos),
          customBgColor = Some(color)
        )
      }

  private def createCellNode(
      pos: Position,
      traversable: Boolean
  ): (StackPane, Label) =
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
        style =
          "-fx-font-size: 9px; -fx-font-weight: normal; -fx-padding: 0 3px 1px 0;"

      StackPane.setAlignment(costLabel, Pos.BottomRight)
      pane.children = Seq(costLabel, robotLabel)
    else pane.children = Seq(robotLabel)

    applyStyle(pane, traversable)
    (pane, robotLabel)

  private def applyStyle(
      pane: StackPane,
      traversable: Boolean,
      customBgColor: Option[Color] = None,
      isRobotTile: Boolean = false
  ): Unit =
    pane.background = customBgColor match
      case Some(color) =>
        new Background(
          Array(new BackgroundFill(color, CornerRadii.Empty, Insets.Empty))
        )
      case None => if traversable then traversableBg else obstacleBg

    pane.border = if isRobotTile then robotBorder
    else if traversable then traversableBorder
    else obstacleBorder
