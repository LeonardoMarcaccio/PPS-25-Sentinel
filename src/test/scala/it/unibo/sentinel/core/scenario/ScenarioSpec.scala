package it.unibo.sentinel.core.scenario

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Warehouse, Position, Area, Tile}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.simulation.Tick
import org.mockito.Mockito

class ScenarioSpec extends UnitTest:
  import Validation.*
  "A Scenario" when:
    val width = 5
    val height = 5
    val topCorner = Position(1, 1)
    val bottomCorner = Position(width - 2, height - 2)
    val warehouse = Warehouse
      .empty(width, height)
      .withArea(Area(topCorner, bottomCorner))(Tile.Floor())
    val s0 = Scenario.in(warehouse)

    "created" should:

      "refer to the given Warehouse" in:
        s0.warehouse shouldBe warehouse

      "should not contain any robot" in:
        s0.spawns shouldBe empty

      "should not contain any mission" in:
        s0.missions shouldBe empty

      "have a default routing policy" in:
        s0.routing shouldBe Policies.Routing.Distance

      "have a default assignment policy" in:
        s0.assignment shouldBe Policies.Assignment.Nearest

    "place a robot" should:

      "return a new scenario with the robot placed" in:
        val result =
          s0.place(Spawn(id = RobotId("R1"), at = Position(1, 1))).value
        result.spawns should contain only Spawn(
          id = RobotId("R1"),
          at = Position(1, 1)
        )

      "signal that the position is occupied" in:
        val position = Position(1, 1)
        val result =
          for
            s1 <- s0.place(Spawn(id = RobotId("R1"), at = position))
            s2 <- s1.place(Spawn(id = RobotId("R2"), at = position))
          yield s2
        result.left.value shouldBe PositionOccupied(Position(1, 1))

      "signal that the position is not a floor tile" in:
        val position = Position(0, 0)
        val result =
          s0.place(Spawn(id = RobotId("R1"), at = position))
        result.left.value shouldBe NotFloorTile(position)

      "signal that the id is already used" in:
        val id = RobotId("R1")
        val result =
          for
            s1 <- s0.place(Spawn(id = id, at = Position(1, 1)))
            s2 <- s1.place(Spawn(id = id, at = Position(1, 2)))
          yield s2
        result.left.value shouldBe RobotAlreadyExists(id)

    "load a mission" should:

      "return a new scenario with the mission added" in:
        val mission = Mission.relocate(
          id = MissionId("M1"),
          destination = Position(1, 1),
          duration = Tick(10)
        )
        val result = s0.load(mission).value
        result.missions should contain only mission

      "signal that the mission id already exists" in:
        val mission = Mission.relocate(
          id = MissionId("M1"),
          destination = Position(1, 1),
          duration = Tick(10)
        )
        val result =
          for
            s1 <- s0.load(mission)
            s2 <- s1.load(mission)
          yield s2
        result.left.value shouldBe MissionAlreadyExists(mission.id)

    "load a deliver mission" should:
      val shelfPos = Position(2, 2)
      val bayPos = Position(3, 3)
      val floorPos = Position(1, 1)
      def warehouseWith(shelfItem: Item): Warehouse =
        warehouse
          .withTile(shelfPos)(Tile.Shelf(shelfItem))
          .withTile(bayPos)(Tile.LoadingBay())

      "accept it when shelf holds the item and drop is a loading bay" in:
        val s = Scenario.in(warehouseWith(Item.Computer))
        val mission = Mission.deliver(
          id = MissionId("M1"),
          item = Item.Computer,
          from = shelfPos,
          to = bayPos,
          duration = Tick(10)
        )
        val result = s.load(mission).value
        result.missions should contain only mission

      "signal NotShelfTile when picking from a non Shelf tile" in:
        val s = Scenario.in(warehouseWith(Item.Computer))
        val mission = Mission.deliver(
          id = MissionId("M1"),
          item = Item.Computer,
          from = floorPos,
          to = bayPos,
          duration = Tick(10)
        )
        s.load(mission).left.value shouldBe NotShelfTile(floorPos)

      "signal ItemMismatch when the shelf holds a different item" in:
        val s = Scenario.in(warehouseWith(Item.Table))
        val mission = Mission.deliver(
          id = MissionId("M1"),
          item = Item.Computer,
          from = shelfPos,
          to = bayPos,
          duration = Tick(10)
        )
        s.load(mission).left.value shouldBe ItemMismatch(shelfPos, Item.Computer, Item.Table)

      "signal NotLoadingBay when dropping outside a loading bay" in:
        val s = Scenario.in(warehouseWith(Item.Computer))
        val mission = Mission.deliver(
          id = MissionId("M1"),
          item = Item.Computer,
          from = shelfPos,
          to = floorPos,
          duration = Tick(10)
        )
        s.load(mission).left.value shouldBe NotLoadingBay(floorPos)

      "report the first failing action" in:
        val s = Scenario.in(warehouseWith(Item.Computer))
        val mission = Mission.deliver(
          id = MissionId("M1"),
          item = Item.Computer,
          from = floorPos,
          to = floorPos,
          duration = Tick(10)
        )
        s.load(mission).left.value shouldBe NotShelfTile(floorPos)

    "change the routing policy" should:

      "return a new scenario with the routing policy changed" in:
        val newRouting = Mockito.mock[Policies.Routing]()
        val result = s0.withRouting(newRouting)
        result.routing shouldBe newRouting

    "change the assignment policy" should:

      "return a new scenario with the assignment policy changed" in:
        val newAssignment = Mockito.mock[Policies.Assignment]()
        val result = s0.withAssignment(newAssignment)
        result.assignment shouldBe newAssignment

    "change the collision selection policy" should:

      "return a new scenario with the selection policy changed" in:
        val newSelection = Mockito.mock[Policies.CollisionSelection]()
        val result = s0.withCollisionSelection(newSelection)
        result.collisionSelection shouldBe newSelection

    "change the collision avoidance policy" should:

      "return a new scenario with the collision avoidance policy changed" in:
        val newHandler = Mockito.mock[Policies.CollisionAvoidance]()
        val result = s0.withCollisionAvoidance(newHandler)
        result.collisionAvoidance shouldBe newHandler

    "build an environment" should:

      "produce a complete Environment containing warehouse, robots and missions" in:
        val wh = warehouse.withTile(Position(1, 1))(Tile.Floor())
        val s = Scenario.in(wh)
        val robotId = RobotId("R1")
        val spawn = Spawn(id = robotId, at = Position(1, 1))
        val mission = Mission.relocate(
          id = MissionId("M1"),
          destination = Position(1, 1),
          duration = Tick(10)
        )

        val scenario = (for
          sc1 <- s.place(spawn)
          sc2 <- sc1.load(mission)
        yield sc2).value

        val env = scenario.build

        env.warehouse shouldBe wh
        env.missions should contain only mission
        env.placements.map(p => (p.robot.id, p.at)) should contain only (robotId -> Position(1, 1))
