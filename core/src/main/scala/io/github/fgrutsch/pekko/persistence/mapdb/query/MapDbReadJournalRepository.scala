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

package io.github.fgrutsch.pekko.persistence.mapdb.query

import io.github.fgrutsch.pekko.persistence.mapdb.journal.{JournalRow, JournalRowMapDbSerializer}
import io.github.fgrutsch.pekko.persistence.mapdb.query.MapDbReadJournalRepository.FlowControl
import io.github.fgrutsch.pekko.persistence.mapdb.query.MapDbReadJournalRepository.FlowControl.{Continue, ContinueDelayed, Stop}
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.mapdb.DB

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object MapDbReadJournalRepository {
  sealed trait FlowControl
  object FlowControl {
    case object Continue        extends FlowControl
    case object ContinueDelayed extends FlowControl
    case object Stop            extends FlowControl
  }
}

class MapDbReadJournalRepository(db: DB, conf: ReadJournalConfig.DbConfig)(implicit system: ActorSystem) {

  implicit private val ec: ExecutionContext = system.dispatcher

  private val journals = db.hashSet[JournalRow](conf.name, JournalRowMapDbSerializer()).createOrOpen()

  def allPersistenceIds(): Source[String, NotUsed] = {
    val stream = journals.stream().map(_.persistenceId).distinct()
    Source.fromJavaStream(() => stream)
  }

  def highestOrdering(): Future[Long] = {
    val stream = journals
      .stream()
      .sorted(JournalRow.orderingComparator)

    Source
      .fromJavaStream(() => stream)
      .map(_.ordering)
      .runWith(Sink.lastOption)
      .map(_.getOrElse(0))
  }

  def list(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long): Source[JournalRow, NotUsed] = {
    val streamSorted = journals
      .stream()
      .filter(_.persistenceId == persistenceId)
      .filter(_.sequenceNr >= fromSequenceNr)
      .filter(_.sequenceNr <= toSequenceNr)
      .filter(_.deleted == false)
      .sorted(JournalRow.seqNrComparator)

    Source
      .fromJavaStream(() => streamSorted)
      .take(max)
  }

  def list(tag: String, offset: Long, max: Long): Source[JournalRow, NotUsed] = {
    val streamSorted = journals
      .stream()
      .filter(_.tags.contains(tag))
      .sorted(JournalRow.orderingComparator)
      .filter(_.ordering > offset)
      .filter(_.deleted == false)

    Source
      .fromJavaStream(() => streamSorted)
      .take(max)
  }

  def listBatched(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      batchSize: Int,
      refreshInterval: Option[(FiniteDuration, Scheduler)]): Source[JournalRow, NotUsed] = {
    Source
      .unfoldAsync[(Long, FlowControl), Seq[JournalRow]]((Math.max(1, fromSequenceNr), Continue)) {
        case (from, control) =>
          val retrieveNextBatch = () => {
            list(persistenceId, from, toSequenceNr, batchSize).runWith(Sink.seq).map { xs =>
              val hasMoreEvents = xs.size == batchSize
              val hasLastEvent  = xs.exists(_.sequenceNr >= toSequenceNr)
              val nextControl   =
                if (hasLastEvent || from > toSequenceNr) Stop
                else if (hasMoreEvents) Continue
                else if (refreshInterval.isEmpty) Stop
                else ContinueDelayed

              val nextFrom = xs.lastOption match {
                case Some(row) => row.sequenceNr + 1
                case None      => from
              }
              Some(((nextFrom, nextControl), xs))
            }
          }

          control match {
            case Stop            => Future.successful(None)
            case Continue        => retrieveNextBatch()
            case ContinueDelayed =>
              val (delay, scheduler) = refreshInterval.get
              org.apache.pekko.pattern.after(delay, scheduler)(retrieveNextBatch())
          }
      }
      .mapConcat(identity)
  }

  def listBatched(
      tag: String,
      offset: Long,
      terminateAfterOrdering: Option[Long],
      batchSize: Int,
      refreshInterval: (FiniteDuration, Scheduler)): Source[JournalRow, NotUsed] = {
    Source
      .unfoldAsync[(Long, FlowControl), Seq[JournalRow]]((offset, Continue)) { case (from, control) =>
        val retrieveNextBatch = () => {
          list(tag, from, batchSize).runWith(Sink.seq).map { xs =>
            val hasMoreEvents = xs.size == batchSize
            val nextControl   = terminateAfterOrdering match {
              case Some(value) if !hasMoreEvents                 => Stop
              case Some(value) if xs.exists(_.ordering >= value) => Stop
              case _                                             => if (hasMoreEvents) Continue else ContinueDelayed
            }
            val nextFrom = if (xs.isEmpty) from else xs.map(_.ordering).max

            Some(((nextFrom, nextControl), xs))
          }
        }

        control match {
          case Stop            => Future.successful(None)
          case Continue        => retrieveNextBatch()
          case ContinueDelayed =>
            val (delay, scheduler) = refreshInterval
            org.apache.pekko.pattern.after(delay, scheduler)(retrieveNextBatch())
        }
      }
      .mapConcat(identity)
  }

}
