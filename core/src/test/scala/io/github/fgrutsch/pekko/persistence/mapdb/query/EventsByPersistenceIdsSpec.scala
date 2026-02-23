package io.github.fgrutsch.pekko.persistence.mapdb.query

import io.github.fgrutsch.pekko.persistence.mapdb.query.TestPersistenceActor.{Command, Event}

import scala.concurrent.duration.DurationInt

class EventsByPersistenceIdsSpec extends MapDbReadJournalSpec {

  test("not terminate stream if there are no events for given persistenceId") { _ =>
    withEventsByPersistenceId()("pid-1", 0, Long.MaxValue) { tp =>
      tp.request(1)
      tp.expectNoMessage(100.millis)
      tp.cancel()
      tp.expectNoMessage(100.millis)
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

    withEventsByPersistenceId()("pid-1", 0, 1) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-1", 1, Event.Untagged("1"))
        .request(1)
        .expectComplete()
        .cancel()
    }

    withEventsByPersistenceId()("pid-1", 0, 2) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-1", 1, Event.Untagged("1"))
        .expectNextEventEnvelope("pid-1", 2, Event.Untagged("2"))
        .request(1)
        .expectComplete()
        .cancel()
    }

    withEventsByPersistenceId()("pid-1", 1, 2) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-1", 1, Event.Untagged("1"))
        .expectNextEventEnvelope("pid-1", 2, Event.Untagged("2"))
        .request(1)
        .expectComplete()
        .cancel()
    }

    withEventsByPersistenceId()("pid-1", 2, 3) { tp =>
      tp.request(Long.MaxValue)
        .expectNextEventEnvelope("pid-1", 2, Event.Untagged("2"))
        .expectNextEventEnvelope("pid-1", 3, Event.Untagged("3"))
        .request(1)
        .expectComplete()
        .cancel()
    }
  }

  test("returns events if there are any and not complete stream") { f =>
    f.actor1 ! Command.PersistEvent("1")
    f.actor1 ! Command.PersistEvent("2")
    f.actor1 ! Command.PersistEvent("3")

    f.actor2 ! Command.PersistEvent("1")
    f.actor2 ! Command.PersistEvent("2")
    f.actor2 ! Command.PersistEvent("3")

    verifyJournalCount(6)

    eventually {
      withEventsByPersistenceId()("pid-1", 0, Long.MaxValue) { tp =>
        tp.request(Long.MaxValue)
          .expectNextEventEnvelope("pid-1", 1, Event.Untagged("1"))
          .expectNextEventEnvelope("pid-1", 2, Event.Untagged("2"))
          .expectNextEventEnvelope("pid-1", 3, Event.Untagged("3"))
          .expectNoMessage(100.millis)
      }

      withEventsByPersistenceId()("pid-1", 2, Long.MaxValue) { tp =>
        tp.request(Long.MaxValue)
          .expectNextEventEnvelope("pid-1", 2, Event.Untagged("2"))
          .expectNextEventEnvelope("pid-1", 3, Event.Untagged("3"))
          .request(Long.MaxValue)
          .expectNoMessage(100.millis)
      }

      withEventsByPersistenceId()("pid-1", 3, Long.MaxValue) { tp =>
        tp.request(Long.MaxValue)
          .expectNextEventEnvelope("pid-1", 3, Event.Untagged("3"))
          .request(Long.MaxValue)
          .expectNoMessage(100.millis)
      }

      withEventsByPersistenceId()("pid-1", 4, Long.MaxValue) { tp =>
        tp.request(Long.MaxValue)
          .expectNoMessage(100.millis)
      }

      withEventsByPersistenceId()("pid-1", 4, Long.MaxValue) { tp =>
        tp.request(Long.MaxValue)
          .expectNoMessage(100.millis)

        f.actor1 ! Command.PersistEvent("4")

        tp
          .expectNextEventEnvelope("pid-1", 4, Event.Untagged("4"))
          .expectNoMessage(100.millis)
      }
    }
  }

}
