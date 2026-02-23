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

package com.fgrutsch.akka.persistence.mapdb.journal

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.mapdb.DB

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.jdk.CollectionConverters._

class MapDbJournalRepository(db: DB, conf: JournalConfig.DbConfig)(implicit system: ActorSystem) {

  implicit private val ec: ExecutionContext = system.dispatcher

  private[journal] val journals = db.hashSet[JournalRow](conf.name, JournalRowMapDbSerializer()).createOrOpen()

  def insert(rows: Seq[JournalRow]): Future[Unit] = {
    val addAll = (currentHighestOrdering: Long) =>
      Future {
        blocking {
          val prepared = rows
            .sortBy(_.sequenceNr)
            .zip(LazyList.from(1).map(currentHighestOrdering + _))
            .map { case (row, ordering) => row.copy(ordering = ordering) }

          journals.addAll(prepared.asJava)
          if (conf.commitRequired) db.commit()
        }
      }

    for {
      ordering <- highestOrdering()
      _        <- addAll(ordering)
    } yield ()
  }

  def list(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long): Source[JournalRow, NotUsed] = {
    val streamSorted = journals
      .stream()
      .filter(_.deleted == false)
      .filter(_.persistenceId == persistenceId)
      .filter(_.sequenceNr >= fromSequenceNr)
      .filter(_.sequenceNr <= toSequenceNr)
      .sorted(JournalRow.seqNrComparator)

    Source
      .fromJavaStream(() => streamSorted)
      .take(max)
  }

  def highestSequenceNr(persistenceId: String): Future[Long] = {
    val stream = journals
      .stream()
      .filter(_.persistenceId == persistenceId)
      .sorted(JournalRow.seqNrComparator)

    Source
      .fromJavaStream(() => stream)
      .map(_.sequenceNr)
      .runWith(Sink.lastOption)
      .map(_.getOrElse(0L))
  }

  def delete(persistenceId: String, toSequenceNr: Long): Future[Unit] = {
    Source
      .fromJavaStream(() => journals.stream())
      .filter(_.persistenceId == persistenceId)
      .filter(_.sequenceNr <= toSequenceNr)
      .mapAsync(1) { row =>
        for {
          _ <- Future(blocking(journals.remove(row)))
          _ <- Future(blocking(journals.add(row.copy(deleted = true))))
        } yield {
          if (conf.commitRequired) db.commit()
        }
      }
      .runWith(Sink.ignore)
      .map(_ => ())
  }

  def clear(): Future[Unit] = {
    Future(blocking(journals.clear()))
      .map(_ => if (conf.commitRequired) db.commit())
  }

  private def highestOrdering(): Future[Long] = {
    val stream = journals
      .stream()
      .sorted(JournalRow.orderingComparator)

    Source
      .fromJavaStream(() => stream)
      .map(_.ordering)
      .runWith(Sink.lastOption)
      .map(_.getOrElse(0L))
  }

}
