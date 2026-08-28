package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

enum Task:
  case Single(action: Action)
  case Done

  def currentAction: Option[Action] = this match
    case Single(action) => Some(action)
    case Done         => None

  def currentLocation: Option[Position] = currentAction.map(_.position)

  def advance: Task = this match
    case Single(_) => Done
    case Done      => Done

object Task:
  def move(to: Position): Task = Single(Action.Move(to))
