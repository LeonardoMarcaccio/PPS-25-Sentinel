package it.unibo.sentinel.core.warehouse

import it.unibo.sentinel.core.simulation.Tick

/** Represents a tile in the warehouse.
  */
sealed trait Tile

object Tile:
  /** A tile that can be walked on, parameterized by traversal cost in Ticks.
    */
  trait Walkable extends Tile:
    def cost: Tick

  /** A tile that can store items.
    */
  trait Storage extends Tile

  /** Represents a Floor tile.
    */
  case class Floor(override val cost: Tick = Tick.unit) extends Walkable

  /** Represents a storage Shelf.
    */
  case class Shelf() extends Storage

  /** Represents a Loading, which is both walkable and used for storage.
    */
  case class LoadingBay(override val cost: Tick = Tick.unit) extends Walkable with Storage