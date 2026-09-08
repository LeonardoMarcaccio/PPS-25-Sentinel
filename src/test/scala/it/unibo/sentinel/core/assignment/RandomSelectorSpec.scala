package it.unibo.sentinel.core.assignment

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.*
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position

class RandomSelectorSpec extends UnitTest with SelectorBehaviors:

  private val mission =
    Mission.relocate(MissionId("M01"), Position(0, 0), Tick(10))

  private def createCandidates(): (Placement, Placement, Placement) =
    (
      Placement(mockRobot(canAccept = true), Position(1, 1)),
      Placement(mockRobot(canAccept = true), Position(2, 2)),
      Placement(mockRobot(canAccept = true), Position(3, 3))
    )

  "A RandomSelector" when:

    behave like commonSelector(Selector.RandomSelector(seed = 42L))

    "selecting among available candidates" should:

      "always return the only available candidate" in:
        val (p1, _, _) = createCandidates()
        val selector = Selector.RandomSelector(seed = 42L)

        for _ <- 1 to 10 do
          selector.choose(mission, Iterable(p1)) shouldBe Some(p1)

      "always return a candidate among the available ones" in:
        val (p1, p2, p3) = createCandidates()
        val candidates = Vector(p1, p2, p3)
        val selector = Selector.RandomSelector(seed = 42L)

        for _ <- 1 to 20 do
          selector.choose(mission, candidates).value shouldBe a[Placement]
        for _ <- 1 to 20 do
          candidates should contain(selector.choose(mission, candidates).value)

      "be deterministic given the same seed" in:
        val (p1, p2, p3) = createCandidates()
        val candidates = Iterable(p1, p2, p3)
        val first = Selector.RandomSelector(seed = 123L)
        val second = Selector.RandomSelector(seed = 123L)

        val firstRun = Vector.fill(10)(first.choose(mission, candidates))
        val secondRun = Vector.fill(10)(second.choose(mission, candidates))

        firstRun shouldBe secondRun

      "match the draw of scala.util.Random for the given seed" in:
        val (p1, p2, p3) = createCandidates()
        val candidates = Vector(p1, p2, p3)
        val seed = 7L
        val expected =
          candidates(new scala.util.Random(seed).nextInt(candidates.size))
        val selector = Selector.RandomSelector(seed = seed)

        selector.choose(mission, candidates) shouldBe Some(expected)
