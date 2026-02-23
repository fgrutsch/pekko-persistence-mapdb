package io.github.fgrutsch.pekko.persistence.mapdb.query

import io.github.fgrutsch.pekko.persistence.mapdb.query.TestPersistenceActor.Command

import scala.concurrent.duration.DurationInt

class PersistenceIdsSpec extends MapDbReadJournalSpec {

  test("not terminate stream if there are no persistenceIds") { _ =>
    withPersistenceIds() { tp =>
      tp.request(1)
        .expectNoMessage(100.millis)
        .cancel()
        .expectNoMessage(100.millis)
    }
  }

  test("return distinct persistenceIds if there are any") { f =>
    f.actor1 ! Command.PersistEvent("1")
    f.actor2 ! Command.PersistEvent("2")

    verifyJournalCount(2)

    eventually {
      withPersistenceIds() { tp =>
        tp.request(3)
          .expectNextUnordered("pid-1", "pid-2")

        tp.request(Long.MaxValue)
          .expectNoMessage(100.millis)

        f.actor1 ! Command.PersistEvent("2")
        tp.expectNoMessage(100.millis)

        f.actor3 ! Command.PersistEvent("1")
        tp.expectNext("pid-3")
          .expectNoMessage(100.millis)
      }
    }
  }

}
