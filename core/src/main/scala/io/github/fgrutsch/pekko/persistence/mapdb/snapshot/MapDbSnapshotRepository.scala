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

package io.github.fgrutsch.pekko.persistence.mapdb.snapshot

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.mapdb.DB

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.jdk.FunctionConverters._

object MapDbSnapshotRepository {
  final case class FindFilter(maxSequenceNr: Option[Long] = None, maxTimestamp: Option[Long] = None)

  final case class DeleteFilter(
      maxSequenceNr: Option[Long] = None,
      maxTimestamp: Option[Long] = None,
      sequenceNr: Option[Long] = None
  )
}

class MapDbSnapshotRepository(db: DB, conf: SnapshotConfig.DbConfig)(implicit system: ActorSystem) {

  implicit private val ec: ExecutionContext = system.dispatcher

  private[snapshot] val snapshots = db.hashSet[SnapshotRow](conf.name, SnapshotRowMapDbSerializer()).createOrOpen()

  def save(row: SnapshotRow): Future[Unit] = {
    val filter     = MapDbSnapshotRepository.FindFilter()
    val findResult = find(row.persistenceId, filter)

    val insert = () => {
      Future {
        blocking {
          snapshots.add(row)
          if (conf.commitRequired) db.commit()
        }
      }
    }

    val update = () => {
      for {
        _ <- Future(
          blocking(snapshots.removeIf(r => r.persistenceId == row.persistenceId && r.sequenceNr == row.sequenceNr)))
        _ <- Future(blocking(snapshots.add(row)))
      } yield {
        if (conf.commitRequired) db.commit()
      }
    }

    findResult.flatMap {
      case Some(_) => update()
      case None    => insert()
    }
  }

  def find(persistenceId: String, filter: MapDbSnapshotRepository.FindFilter): Future[Option[SnapshotRow]] = {
    val streamSorted = snapshots
      .stream()
      .filter(_.persistenceId == persistenceId)
      .filter { r =>
        filter.maxSequenceNr.forall(r.sequenceNr <= _)
      }
      .filter { r =>
        filter.maxTimestamp.forall(r.created <= _)
      }
      .sorted(SnapshotRow.reverseSequenceNrComparator)

    Source
      .fromJavaStream(() => streamSorted)
      .runWith(Sink.headOption)
  }

  def delete(persistenceId: String, filter: MapDbSnapshotRepository.DeleteFilter): Future[Unit] = {
    val query = (s: SnapshotRow) => {
      s.persistenceId == persistenceId &&
      filter.sequenceNr.forall(s.sequenceNr == _) &&
      filter.maxSequenceNr.forall(s.sequenceNr <= _) &&
      filter.maxTimestamp.forall(s.created <= _)
    }

    Future {
      blocking {
        snapshots.removeIf(query.asJavaPredicate)
        if (conf.commitRequired) db.commit()
      }
    }
  }

  def clear(): Future[Unit] = {
    Future(blocking(snapshots.clear())).map { _ =>
      if (conf.commitRequired) db.commit()
    }
  }

}
