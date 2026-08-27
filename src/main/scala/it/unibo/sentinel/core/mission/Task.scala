package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

enum Task:
  case Single(step: Step)
  case Done

  def currentStep: Option[Step] = this match
    case Single(step) => Some(step)
    case Done         => None

  def currentLocation: Option[Position] = currentStep.map(_.position)

  def advance: Task = this match
    case Single(_) => Done
    case Done      => Done

object Task:
  def move(to: Position): Task = Single(Step.Move(to))
