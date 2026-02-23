/*
 * Copyright 2026 pekko-persistence-mapdb contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.fgrutsch.pekko.persistence.mapdb.query.scaladsl

import com.typesafe.config.Config
import io.github.fgrutsch.pekko.persistence.mapdb.db.MapDbExtension
import io.github.fgrutsch.pekko.persistence.mapdb.query._
import io.github.fgrutsch.pekko.persistence.mapdb.util.PekkoSerialization
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.{ExtendedActorSystem, Scheduler}
import org.apache.pekko.persistence.query.scaladsl._
import org.apache.pekko.persistence.query.{EventEnvelope, Offset, Sequence}
import org.apache.pekko.persistence.{Persistence, PersistentRepr}
import org.apache.pekko.serialization.SerializationExtension
import org.apache.pekko.stream.scaladsl.Source

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object MapDbReadJournal {
  final val Identifier = "mapdb-read-journal"
}

class MapDbReadJournal(config: Config)(implicit val system: ExtendedActorSystem)
    extends ReadJournal
    with CurrentPersistenceIdsQuery
    with PersistenceIdsQuery
    with CurrentEventsByPersistenceIdQuery
    with EventsByPersistenceIdQuery
    with CurrentEventsByTagQuery
    with EventsByTagQuery {

  implicit private val ec: ExecutionContext = system.dispatcher
  private val serialization                 = SerializationExtension(system)
  private val eventAdapters                 = {
    val writePluginId = config.getString("write-plugin")
    Persistence(system).adaptersFor(writePluginId, config)
  }

  private val readJournalConfig = new ReadJournalConfig(config)
  private val db                = MapDbExtension(system).database
  private val repo              = new MapDbReadJournalRepository(db, readJournalConfig.db)
  private val delaySource = Source.tick(readJournalConfig.refreshInterval, readJournalConfig.refreshInterval, 0).take(1)

  override def currentPersistenceIds(): Source[String, NotUsed] = {
    repo.allPersistenceIds()
  }

  override def persistenceIds(): Source[String, NotUsed] = {
    Source
      .repeat(0)
      .flatMapConcat(_ => delaySource.flatMapConcat(_ => currentPersistenceIds()))
      .statefulMapConcat[String] { () =>
        var knownIds                           = Set.empty[String]
        def next(id: String): Iterable[String] = {
          val xs = Set(id).diff(knownIds)
          knownIds += id
          xs
        }
        id => next(id)
      }
  }

  override def currentEventsByPersistenceId(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long): Source[EventEnvelope, NotUsed] = {
    eventsByPersistenceIdSource(persistenceId, fromSequenceNr, toSequenceNr, None)
  }

  override def eventsByPersistenceId(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long): Source[EventEnvelope, NotUsed] = {
    eventsByPersistenceIdSource(
      persistenceId,
      fromSequenceNr,
      toSequenceNr,
      Some(readJournalConfig.refreshInterval -> system.scheduler))
  }

  override def currentEventsByTag(tag: String, offset: Offset): Source[EventEnvelope, NotUsed] = {
    val batchSize       = readJournalConfig.maxBufferSize
    val refreshInterval = readJournalConfig.refreshInterval -> system.scheduler

    Source
      .futureSource {
        repo.highestOrdering().map { highestOrderingId =>
          eventsByTagSource(tag, offset.value, Some(highestOrderingId))
        }
      }
      .mapMaterializedValue(_ => NotUsed)
  }

  override def eventsByTag(tag: String, offset: Offset): Source[EventEnvelope, NotUsed] = {
    eventsByTagSource(tag, offset.value, None)
  }

  private def eventsByPersistenceIdSource(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      refreshInterval: Option[(FiniteDuration, Scheduler)]): Source[EventEnvelope, NotUsed] = {
    val batchSize = readJournalConfig.maxBufferSize

    repo
      .listBatched(persistenceId, fromSequenceNr, toSequenceNr, batchSize, refreshInterval)
      .map(PekkoSerialization.fromJournalRow(serialization)(_))
      .mapAsync(1)(reprAndOrdering => Future.fromTry(reprAndOrdering))
      .mapConcat { case (repr, ordering) => adaptEvents(repr).map(_ -> ordering) }
      .map { case (repr, ordering) =>
        EventEnvelope(Sequence(ordering), repr.persistenceId, repr.sequenceNr, repr.payload, repr.timestamp)
      }
  }

  private def eventsByTagSource(
      tag: String,
      offset: Long,
      terminateAfterOrdering: Option[Long]
  ): Source[EventEnvelope, NotUsed] = {
    val batchSize       = readJournalConfig.maxBufferSize
    val refreshInterval = readJournalConfig.refreshInterval -> system.scheduler

    repo
      .listBatched(tag, offset, terminateAfterOrdering, batchSize, refreshInterval)
      .mapMaterializedValue(_ => NotUsed)
      .map(PekkoSerialization.fromJournalRow(serialization)(_))
      .mapAsync(1)(reprAndOrdering => Future.fromTry(reprAndOrdering))
      .mapConcat { case (repr, ordering) => adaptEvents(repr).map(_ -> ordering) }
      .map { case (repr, ordering) =>
        EventEnvelope(Sequence(ordering), repr.persistenceId, repr.sequenceNr, repr.payload, repr.timestamp)
      }
  }

  private def adaptEvents(repr: PersistentRepr): Seq[PersistentRepr] = {
    val adapter = eventAdapters.get(repr.payload.getClass)
    adapter.fromJournal(repr.payload, repr.manifest).events.map(repr.withPayload)
  }

}
