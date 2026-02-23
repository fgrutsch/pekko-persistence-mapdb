package com.fgrutsch.akka.persistence.mapdb.query

import org.apache.pekko.persistence.journal.{EventAdapter, EventSeq, Tagged}

class TestEventAdapter extends EventAdapter {

  override def fromJournal(event: Any, manifest: String): EventSeq = {
    event match {
      case e: TestPersistenceActor.Event => EventSeq.single(e)
    }
  }

  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = {
    event match {
      case e: TestPersistenceActor.Event.Untagged => e
      case e: TestPersistenceActor.Event.Tagged   => Tagged(e, Set(e.tag))
    }
  }

}
