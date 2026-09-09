package it.unibo.sentinel.core.scenario

import it.unibo.sentinel.core.routing.{Navigator, Metric}
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.assignment.Selector
import it.unibo.sentinel.core.collisions.SelectionPolicy
import it.unibo.sentinel.core.collisions.CollisionHandler
import it.unibo.sentinel.core.mission.Mission

/** Represents the policies that govern the behavior of the simulation.
  */
object Policies:
  /** Routing policies, i.e. how routes are determined.
    */
  enum Routing:
    /** Routes are determined based on distance.
      */
    case Distance

    /** Routes are determined based on time.
      */
    case Time

    /** @return
      *   the [[Navigator]] for the given [[Routing]] policy.
      */
    def apply()(using Warehouse): Navigator = this match
      case Distance => Navigator(Metric.Hops)
      case Time     => Navigator(Metric.Time)

  /** Assignment policies, i.e. how mission are assigned.
    */
  enum Assignment:
    /** Assignment based on distance from target.
      */
    case Nearest

    /** Round-robin assignment cycling through candidates.
      */
    case Cycle

    /** Random assignment.
      */
    case Random

    /** @param seed
      *   the seed governing random choices.
      * @return
      *   the [[Selector]] for the given [[Assignment]] policy.
      */
    def apply(seed: Long)(using nav: Navigator): Selector = this match
      case Nearest => Selector.Nearest(nav)
      case Cycle   => Selector.CycleSelector()
      case Random  => Selector.RandomSelector(seed)

  enum CollisionSelection:

    case Random
    case Deadline
    case Priority

    /** @param seed
      *   the seed governing random choices.
      * @return
      *   the [[SelectionPolicy]] for the given policy.
      */
    def apply(seed: Long)(using
        missionSupplier: => Seq[Mission]
    ): SelectionPolicy =
      this match
        case Random   => SelectionPolicy.random(seed)
        case Deadline => SelectionPolicy.closestDeadline()
        case Priority => SelectionPolicy.highestPriority()

  enum CollisionAvoidance:

    case Wait

    def apply(): CollisionHandler = this match
      case Wait => CollisionHandler.pausing()
