package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.item.Item

enum Task:
  case Then(head: Task, tail: Task)
  case Single(action: Action)
  case Done

  def currentAction: Option[Action] = this match
    case Then(head, _) => head.currentAction 
    case Single(action) => Some(action)
    case Done           => None

  def advance: Task = this match
    case Then(_, tail) => tail
    case Single(_) => Done
    case Done      => Done

  def actions: Iterator[Action] = this match
    case Done        => Iterator.empty
    case Single(a)   => Iterator.single(a)
    case Then(h, t)  => h.actions ++ t.actions

object Task:
  def move(to: Position): Task = Single(Action.Move(to))
  def pick(item: Item, at: Position): Task = Single(Action.PickUp(item, at))
  def drop(item: Item, at: Position): Task = Single(Action.Drop(item, at))
  def pickAndDrop(item: Item, at: Position, to: Position): Task = Then(pick(item, at), drop(item, to))
