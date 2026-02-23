package io.github.fgrutsch.pekko.persistence.mapdb.query

import io.github.fgrutsch.pekko.persistence.mapdb.query.TestPersistenceActor.Command

class CurrentPersistenceIdsSpec extends MapDbReadJournalSpec {

  test("don't return any persistenceIds if there are none") { _ =>
    withCurrentPersistenceIds() { tp =>
      tp.request(1)
        .expectComplete()
    }
  }

  test("returns distinct persistenceIds if there are any") { f =>
    f.actor1 ! Command.PersistEvent("1")
    f.actor1 ! Command.PersistEvent("2")
    f.actor2 ! Command.PersistEvent("1")
    f.actor3 ! Command.PersistEvent("1")
    f.actor3 ! Command.PersistEvent("2")

    verifyJournalCount(5)

    withCurrentPersistenceIds() { tp =>
      tp.request(Long.MaxValue)
        .expectNextUnordered("pid-1", "pid-2", "pid-3")
        .expectComplete()
    }
  }

}
