package it.unibo.sentinel.core.item

/** Unique identifier for [[Item]]s
  */
opaque type ItemId = String

object ItemId:
  def apply(id: String): ItemId = id

extension (id: ItemId)
  /** @return
    *   the identifier as a String
    */
  def value: String = id
