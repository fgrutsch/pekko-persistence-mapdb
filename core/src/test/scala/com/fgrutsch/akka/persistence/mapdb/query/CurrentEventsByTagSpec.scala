package com.fgrutsch.akka.persistence.mapdb.query

import com.fgrutsch.akka.persistence.mapdb.query.TestPersistenceActor.{Command, Event}
import org.apache.pekko.pattern.ask
import org.apache.pekko.persistence.query.Sequence

class CurrentEventsByTagSpec extends MapDbReadJournalSpec {

  test("don't return any events if there are none") { _ =>
    withCurrentEventsByTag()("tag", 0) { tp =>
      tp.request(1)
        .expectComplete()
    }
  }

  test("returns events if there are any") { f =>
    (f.actor1 ? Command.PersistTaggedEvent("1", "tag2")).futureValue
    (f.actor1 ? Command.PersistTaggedEvent("2", "tag")).futureValue
    (f.actor1 ? Command.PersistTaggedEvent("3", "tag")).futureValue

    (f.actor2 ? Command.PersistTaggedEvent("1", "tag")).futureValue
    (f.actor2 ? Command.PersistTaggedEvent("2", "tag2")).futureValue
    (f.actor2 ? Command.PersistTaggedEvent("3", "tag2")).futureValue

    verifyJournalCount(6)

    withCurrentEventsByTag()("tag", 0) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-1", 2, Event.Tagged("2", "tag"))
        .expectNextEventEnvelope("pid-1", 3, Event.Tagged("3", "tag"))
        .expectNextEventEnvelope("pid-2", 1, Event.Tagged("1", "tag"))
        .expectComplete()
    }

    withCurrentEventsByTag()("tag", 2) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-1", Sequence(3), Event.Tagged("3", "tag"))
        .expectNextEventEnvelope("pid-2", Sequence(4), Event.Tagged("1", "tag"))
        .expectComplete()
    }

    withCurrentEventsByTag()("tag", 3) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-2", Sequence(4), Event.Tagged("1", "tag"))
        .expectComplete()
    }

    withCurrentEventsByTag()("tag", 4) { tp =>
      tp.request(Long.MaxValue)
        .expectComplete()
    }
  }

}
