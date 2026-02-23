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

import io.github.fgrutsch.pekko.persistence.mapdb.util.BinarySerializer
import org.mapdb.{DataInput2, DataOutput2, Serializer}

private[snapshot] object SnapshotRowMapDbSerializer {
  def apply(): Serializer[SnapshotRow] = new SnapshotRowMapDbSerializer
}

private[snapshot] class SnapshotRowMapDbSerializer extends Serializer[SnapshotRow] {

  private val byteArraySerializer = Serializer.BYTE_ARRAY

  override def serialize(out: DataOutput2, value: SnapshotRow): Unit = {
    val serialized = BinarySerializer.serialize(value)
    byteArraySerializer.serialize(out, serialized)
  }

  override def deserialize(input: DataInput2, available: Int): SnapshotRow = {
    val deserialized = byteArraySerializer.deserialize(input, available)
    BinarySerializer.deserialize(deserialized)
  }

  override def isTrusted: Boolean = true

  override def equals(first: SnapshotRow, second: SnapshotRow): Boolean = {
    val firstSerialized  = BinarySerializer.serialize(first)
    val secondSerialized = BinarySerializer.serialize(second)
    byteArraySerializer.equals(firstSerialized, secondSerialized)
  }

  override def hashCode(o: SnapshotRow, seed: Int): Int = {
    val serialized = BinarySerializer.serialize(o)
    byteArraySerializer.hashCode(serialized, seed)
  }

  override def compare(first: SnapshotRow, second: SnapshotRow): Int = {
    val firstSerialized  = BinarySerializer.serialize(first)
    val secondSerialized = BinarySerializer.serialize(second)
    byteArraySerializer.compare(firstSerialized, secondSerialized)
  }

}
