package it.unibo.sentinel.core.item

enum Item(val weight: ItemWeight):
  case Computer   extends Item(ItemWeight(1))
  case Table      extends Item(ItemWeight(10))
  case Fridge     extends Item(ItemWeight(50))
  case Dishwasher extends Item(ItemWeight(50))