package com.fgrutsch.akka.persistence.mapdb.query

import com.fgrutsch.akka.persistence.mapdb.query.TestPersistenceActor._
import org.apache.pekko.actor.Status.{Failure, Success}
import org.apache.pekko.actor.{ActorRef, Stash}
import org.apache.pekko.persistence.{DeleteMessagesFailure, DeleteMessagesSuccess, PersistentActor}
import testing.PekkoSerializable

object TestPersistenceActor {
  sealed trait Event extends PekkoSerializable
  object Event {
    final case class Untagged(msg: String)            extends Event
    final case class Tagged(msg: String, tag: String) extends Event
  }

  sealed trait Command
  object Command {
    case object GetState                                          extends Command
    final case class PersistEvent(msg: String)                    extends Command
    final case class PersistTaggedEvent(msg: String, tag: String) extends Command
    final case class Delete(toSeqNr: Long)                        extends Command
  }
}

class TestPersistenceActor(id: Int) extends PersistentActor with Stash {

  private var count: Int = 0

  override def persistenceId: String = s"pid-$id"

  override def receiveRecover: Receive = {
    case _: Event.Tagged   => updateState()
    case _: Event.Untagged => updateState()
  }

  override def receiveCommand: Receive = default

  private def default: Receive = {
    case Command.GetState => sender() ! count

    case Command.PersistEvent(msg) =>
      persist(Event.Untagged(msg)) { e =>
        updateState()
        sender() ! Success(e)
      }

    case Command.PersistTaggedEvent(msg, tag) =>
      persist(Event.Tagged(msg, tag)) { e =>
        updateState()
        sender() ! Success(e)
      }

    case Command.Delete(toSeqNr) =>
      deleteMessages(toSeqNr)
      context.become(deleting(sender()))
  }

  private def deleting(originalSender: ActorRef): Receive = {
    case DeleteMessagesSuccess(toSeqNr) =>
      originalSender ! Success("delete-success")
      unstashAll()
      context.become(default)

    case DeleteMessagesFailure(t, _) =>
      originalSender ! Failure(t)
      unstashAll()
      context.become(default)

    case _ => stash()
  }

  private def updateState(): Unit = {
    count = count + 1
  }

}
