package it.unibo.sentinel.core.scenario

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Warehouse, Position, Area, Tile}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.item.{Item, ItemId, ItemWeight, StoredItem}
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

    "place an item" should:

      "return a new scenario with the item placed" in:
        val wh = warehouse.withTile(Position(2, 2))(Tile.Shelf())
        val s = Scenario.in(wh)
        val stored = StoredItem(Item(ItemId("I1"), ItemWeight(5)), Position(2, 2))
        val result = s.placeItem(stored).value
        result.items should contain only stored

      "signal that the position is not a storage tile" in:
        val stored = StoredItem(Item(ItemId("I1"), ItemWeight(5)), Position(1, 1))
        val result = s0.placeItem(stored)
        result.left.value shouldBe NotStorageTile(Position(1, 1))

      "signal that the item id already exists" in:
        val wh = warehouse.withTile(Position(2, 2))(Tile.Shelf())
        val s = Scenario.in(wh)
        val stored = StoredItem(Item(ItemId("I1"), ItemWeight(5)), Position(2, 2))
        val result =
          for
            s1 <- s.placeItem(stored)
            s2 <- s1.placeItem(stored)
          yield s2
        result.left.value shouldBe ItemAlreadyExists(ItemId("I1"))

      "signal that the storage position is already occupied" in:
        val wh = warehouse
          .withTile(Position(2, 2))(Tile.Shelf())
          .withTile(Position(2, 3))(Tile.Shelf())
        val s = Scenario.in(wh)
        val s1 = StoredItem(Item(ItemId("I1"), ItemWeight(5)), Position(2, 2))
        val s2 = StoredItem(Item(ItemId("I2"), ItemWeight(3)), Position(2, 2))
        val result =
          for
            sc1 <- s.placeItem(s1)
            sc2 <- sc1.placeItem(s2)
          yield sc2
        result.left.value shouldBe PositionOccupied(Position(2, 2))

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

      "produce a complete Environment containing warehouse, robots, missions and stored items" in:
        val wh = warehouse
          .withTile(Position(1, 1))(Tile.Floor())
          .withTile(Position(2, 2))(Tile.Shelf())
        val s = Scenario.in(wh)
        val robotId = RobotId("R1")
        val spawn = Spawn(id = robotId, at = Position(1, 1))
        val mission = Mission.relocate(
          id = MissionId("M1"),
          destination = Position(1, 1),
          duration = Tick(10)
        )
        val stored = StoredItem(Item(ItemId("I1"), ItemWeight(5)), Position(2, 2))

        val scenario = (for
          sc1 <- s.place(spawn)
          sc2 <- sc1.load(mission)
          sc3 <- sc2.placeItem(stored)
        yield sc3).value

        val env = scenario.build

        env.warehouse shouldBe wh
        env.missions should contain only mission
        env.placements.map(p => (p.robot.id, p.at)) should contain only (robotId -> Position(1, 1))
        env.items should contain only stored
        env.item(ItemId("I1")).value shouldBe stored
        env.itemsAt(Position(2, 2)) should contain only stored
        env.itemsAt(Position(1, 1)) shouldBe empty
