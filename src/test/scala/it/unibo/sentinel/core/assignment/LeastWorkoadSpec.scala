package it.unibo.sentinel.core.assignment

import org.mockito.Mockito.when
import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.*
import it.unibo.sentinel.core.robot.{Robot, RobotId}
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.scenario.{Placement, Policies}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position
import scala.util.Random

class LeastLoadedSpec extends UnitTest with SelectorBehaviors:

  private val mission =
    Mission.relocate(MissionId("M01"), Position(0, 0), Tick(10))

  private def mockLoadedRobot(workload: Int, canAccept: Boolean = true): Robot =
    val robot = mockRobot(canAccept)
    when(robot.workload).thenReturn(workload)
    robot

  "A LeastWorkload selector" when:

    behave like commonSelector(Selector.LeastWorkload())

    "selecting among available candidates" should:

      "choose the candidate with the lowest workload" in:
        val light = Placement(mockLoadedRobot(workload = 1), Position(1, 1))
        val heavy = Placement(mockLoadedRobot(workload = 3), Position(2, 2))
        val selector = Selector.LeastWorkload()

        selector.choose(mission, Iterable(heavy, light)) shouldBe Some(light)

      "prefer an idle robot over a loaded one" in:
        val idle = Placement(mockLoadedRobot(workload = 0), Position(1, 1))
        val loaded = Placement(mockLoadedRobot(workload = 2), Position(2, 2))
        val selector = Selector.LeastWorkload()

        selector.choose(mission, Iterable(loaded, idle)) shouldBe Some(idle)

      "break workload ties deterministically by robot id" in:
        val r1 = Robot.drone(RobotId("R1"))
        val r2 = Robot.drone(RobotId("R2"))
        val p1 = Placement(r1, Position(1, 1))
        val p2 = Placement(r2, Position(2, 2))
        val selector = Selector.LeastWorkload()

        selector
          .choose(mission, Iterable(p2, p1))
          .map(_.robot.id) shouldBe Some(RobotId("R1"))
        selector
          .choose(mission, Iterable(p1, p2))
          .map(_.robot.id) shouldBe Some(RobotId("R1"))

      "ignore busy candidates even if their workload is lower" in:
        val busy = Placement(
          mockLoadedRobot(workload = 0, canAccept = false),
          Position(9, 9)
        )
        val available = Placement(mockLoadedRobot(workload = 2), Position(1, 1))
        val selector = Selector.LeastWorkload()

        selector.choose(mission, Iterable(busy, available)) shouldBe Some(
          available
        )

      "follow the real queue depth of robots" in:
        val r1 = Robot.drone(RobotId("R1"), capacity = 3)
        val r2 = Robot.drone(RobotId("R2"), capacity = 3)
        val p1 = Placement(r1, Position(1, 1))
        val p2 = Placement(r2, Position(2, 2))
        val selector = Selector.LeastWorkload()

        selector
          .choose(mission, Iterable(p1, p2))
          .map(_.robot.id) shouldBe Some(RobotId("R1"))

        r2.accept(Mission.relocate(MissionId("MA"), Position(9, 9), Tick(10)))
        selector
          .choose(mission, Iterable(p1, p2))
          .map(_.robot.id) shouldBe Some(RobotId("R1"))

        r1.accept(Mission.relocate(MissionId("MB"), Position(9, 9), Tick(10)))
        r1.accept(Mission.relocate(MissionId("MC"), Position(9, 9), Tick(10)))
        selector
          .choose(mission, Iterable(p1, p2))
          .map(_.robot.id) shouldBe Some(RobotId("R2"))

    "resolving a policy" should:

      "build a LeastWorkload selector from Policies.Assignment.LeastWorkload" in:
        given Navigator = mock[Navigator]
        val policy: Policies.Assignment = Policies.Assignment.LeastWorkload

        policy(new Random(0)) shouldBe a[Selector.LeastWorkload]
