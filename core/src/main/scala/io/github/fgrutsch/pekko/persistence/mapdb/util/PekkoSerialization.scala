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

package io.github.fgrutsch.pekko.persistence.mapdb.util
import io.github.fgrutsch.pekko.persistence.mapdb.journal.JournalRow
import io.github.fgrutsch.pekko.persistence.mapdb.snapshot.SnapshotRow
import org.apache.pekko.persistence.{PersistentRepr, SelectedSnapshot, SnapshotMetadata}
import org.apache.pekko.serialization.{Serialization, Serializers}

import scala.util.Try

private[mapdb] object PekkoSerialization {

  case class PekkoSerialized(serId: Int, serManifest: String, payload: Array[Byte])

  def serialize(serialization: Serialization)(payload: Any): Try[PekkoSerialized] = {
    val p2          = payload.asInstanceOf[AnyRef]
    val serializer  = serialization.findSerializerFor(p2)
    val serManifest = Serializers.manifestFor(serializer, p2)
    val serialized  = serialization.serialize(p2)
    serialized.map(payload => PekkoSerialized(serializer.identifier, serManifest, payload))
  }

  def fromJournalRow(serialization: Serialization)(row: JournalRow): Try[(PersistentRepr, Long)] = {
    serialization.deserialize(row.eventPayload, row.eventSerId, row.eventSerManifest).map { payload =>
      val persistentRepr = PersistentRepr(payload, row.sequenceNr, row.persistenceId, writerUuid = row.writer)
      (persistentRepr, row.ordering)
    }
  }

  def fromSnapshotRow(serialization: Serialization)(row: SnapshotRow): Try[SelectedSnapshot] = {
    serialization.deserialize(row.snapshotPayload, row.snapshotSerId, row.snapshotSerManifest).map { payload =>
      SelectedSnapshot(
        SnapshotMetadata(row.persistenceId, row.sequenceNr, row.created),
        payload
      )
    }
  }

}
