package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

enum Step:
  case Move(target: Position)

  def position: Position = this match
    case Move(p) => p
