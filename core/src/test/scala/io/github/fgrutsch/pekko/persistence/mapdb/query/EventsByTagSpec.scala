package io.github.fgrutsch.pekko.persistence.mapdb.query

import io.github.fgrutsch.pekko.persistence.mapdb.query.TestPersistenceActor.{Command, Event}
import org.apache.pekko.pattern.ask
import org.apache.pekko.persistence.query.Sequence

import scala.concurrent.duration.DurationInt

class EventsByTagSpec extends MapDbReadJournalSpec {

  test("not terminate stream if there are no events for given tag") { _ =>
    withEventsByTag()("tag", 0) { tp =>
      tp.request(1)
      tp.expectNoMessage(100.millis)
      tp.cancel()
      tp.expectNoMessage(100.millis)
    }
  }

  test("returns events if there are any and not complete stream") { f =>
    (f.actor1 ? Command.PersistTaggedEvent("1", "tag2")).futureValue
    (f.actor1 ? Command.PersistTaggedEvent("2", "tag")).futureValue
    (f.actor1 ? Command.PersistTaggedEvent("3", "tag")).futureValue

    (f.actor2 ? Command.PersistTaggedEvent("1", "tag")).futureValue
    (f.actor2 ? Command.PersistTaggedEvent("2", "tag2")).futureValue
    (f.actor2 ? Command.PersistTaggedEvent("3", "tag2")).futureValue

    verifyJournalCount(6)

    eventually {
      withEventsByTag()("tag", 0) { tp =>
        tp.request(Long.MaxValue)
          .expectNextEventEnvelope("pid-1", 2, Event.Tagged("2", "tag"))
          .expectNextEventEnvelope("pid-1", 3, Event.Tagged("3", "tag"))
          .expectNextEventEnvelope("pid-2", 1, Event.Tagged("1", "tag"))
          .expectNoMessage(100.millis)
      }

      withEventsByTag()("tag", 2) { tp =>
        tp.request(Long.MaxValue)
          .expectNextEventEnvelope("pid-1", Sequence(3), Event.Tagged("3", "tag"))
          .expectNextEventEnvelope("pid-2", Sequence(4), Event.Tagged("1", "tag"))
          .expectNoMessage(100.millis)
      }

      withEventsByTag()("tag", 3) { tp =>
        tp.request(Long.MaxValue)
          .expectNextEventEnvelope("pid-2", Sequence(4), Event.Tagged("1", "tag"))
          .expectNoMessage(100.millis)
      }

      withEventsByTag()("tag", 4) { tp =>
        tp.request(Long.MaxValue)
          .expectNoMessage(100.millis)
      }

      withEventsByTag()("tag", 4) { tp =>
        tp.request(Long.MaxValue)
          .expectNoMessage(100.millis)

        (f.actor1 ? Command.PersistTaggedEvent("4", "tag")).futureValue
        (f.actor2 ? Command.PersistTaggedEvent("4", "tag2")).futureValue

        tp
          .expectNextEventEnvelope("pid-1", Sequence(7), Event.Tagged("4", "tag"))
          .expectNoMessage(100.millis)
      }
    }
  }

}
