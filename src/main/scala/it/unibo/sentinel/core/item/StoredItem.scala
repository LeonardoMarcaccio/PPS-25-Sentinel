package it.unibo.sentinel.core.item

import it.unibo.sentinel.core.warehouse.Position

/** Represents an [[Item]] positioned in the [[Warehouse]].
  *
  * @param item
  *   the stored item
  * @param at
  *   the position where the item is stored
  */
final case class StoredItem(item: Item, at: Position)
