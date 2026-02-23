package io.github.fgrutsch.pekko.persistence.mapdb.query

import io.github.fgrutsch.pekko.persistence.mapdb.query.TestPersistenceActor.{Command, Event}

class CurrentEventsByPersistenceIdsSpec extends MapDbReadJournalSpec {

  test("don't return any events if there are none") { _ =>
    withCurrentEventsByPersistenceId()("pid-1", 0, Long.MaxValue) { tp =>
      tp.request(1)
        .expectComplete()
    }
  }

  test("returns events if there are any") { f =>
    f.actor1 ! Command.PersistEvent("1")
    f.actor1 ! Command.PersistEvent("2")
    f.actor1 ! Command.PersistEvent("3")

    f.actor2 ! Command.PersistEvent("1")
    f.actor2 ! Command.PersistEvent("2")
    f.actor2 ! Command.PersistEvent("3")

    verifyJournalCount(6)

    withCurrentEventsByPersistenceId()("pid-1", 0, Long.MaxValue) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-1", 1, Event.Untagged("1"))
        .expectNextEventEnvelope("pid-1", 2, Event.Untagged("2"))
        .expectNextEventEnvelope("pid-1", 3, Event.Untagged("3"))
        .expectComplete()
    }

    withCurrentEventsByPersistenceId()("pid-1", 0, 1) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-1", 1, Event.Untagged("1"))
        .expectComplete()
    }

    withCurrentEventsByPersistenceId()("pid-1", 0, 2) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-1", 1, Event.Untagged("1"))
        .expectNextEventEnvelope("pid-1", 2, Event.Untagged("2"))
        .expectComplete()
    }

    withCurrentEventsByPersistenceId()("pid-1", 1, 2) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-1", 1, Event.Untagged("1"))
        .expectNextEventEnvelope("pid-1", 2, Event.Untagged("2"))
        .expectComplete()
    }

    withCurrentEventsByPersistenceId()("pid-1", 2, 3) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-1", 2, Event.Untagged("2"))
        .expectNextEventEnvelope("pid-1", 3, Event.Untagged("3"))
        .expectComplete()
    }
  }

}
