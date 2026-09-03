package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest

class SimpleRobotSpec extends UnitTest with RobotFixture with RobotBehavior:

  "A SimpleRobot" when:
    behave like baseRobot(Robot.drone(robotId))

    "already on a mission" should:
      val robot = Robot.drone(robotId)
      robot.accept(m1)

      "not be able to accept another one" in:
        robot.canAccept shouldBe false

      "keep its current mission when offered another" in:
        robot.accept(m2)
        robot.mission shouldBe Some(m1)
