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

package io.github.fgrutsch.pekko.persistence.mapdb.journal

import com.typesafe.config.Config
import io.github.fgrutsch.pekko.persistence.mapdb.db.MapDbExtension
import io.github.fgrutsch.pekko.persistence.mapdb.util.PekkoSerialization
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.journal.{AsyncWriteJournal, Tagged}
import org.apache.pekko.persistence.{AtomicWrite, PersistentRepr}
import org.apache.pekko.serialization.SerializationExtension

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class MapDbJournal(config: Config) extends AsyncWriteJournal {

  implicit val system: ActorSystem          = context.system
  implicit private val ec: ExecutionContext = system.dispatcher
  private val serialization                 = SerializationExtension(system)
  private val journalConf                   = new JournalConfig(config)

  private val db   = MapDbExtension(system).database
  private val repo = new MapDbJournalRepository(db, journalConf.db)

  override def asyncWriteMessages(messages: Seq[AtomicWrite]): Future[Seq[Try[Unit]]] = {
    val now                   = System.currentTimeMillis()
    val messagesWithTimestamp =
      messages.map(atomWrt => atomWrt.copy(payload = atomWrt.payload.map(pr => pr.withTimestamp(now))))

    val serializedTries = messagesWithTimestamp.map(serializeAtomicWrite)

    val rowsToWrite = for {
      serializeTry <- serializedTries
      row          <- serializeTry.getOrElse(Seq.empty)
    } yield row

    def resultWhenWriteComplete =
      if (serializedTries.forall(_.isSuccess)) Nil else serializedTries.map(_.map(_ => ()))

    repo.insert(rowsToWrite).map(_ => resultWhenWriteComplete)
  }

  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long): Future[Unit] = {
    repo.delete(persistenceId, toSequenceNr)
  }

  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(
      recoveryCallback: PersistentRepr => Unit): Future[Unit] = {
    repo
      .list(persistenceId, fromSequenceNr, toSequenceNr, max)
      .map(PekkoSerialization.fromJournalRow(serialization)(_))
      .mapAsync(1)(reprAndOrdering => Future.fromTry(reprAndOrdering))
      .runForeach { case (repr, _) => recoveryCallback(repr) }
      .map(_ => ())
  }

  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = {
    repo.highestSequenceNr(persistenceId)
  }

  private def serializeAtomicWrite(aw: AtomicWrite): Try[Seq[JournalRow]] = {
    Try(aw.payload.map(serializePersistentRepr))
  }

  private def serializePersistentRepr(repr: PersistentRepr): JournalRow = {
    val (flattenedRepr, tags) = repr.payload match {
      case Tagged(payload, tags) => (repr.withPayload(payload), tags)
      case _                     => (repr, Set.empty[String])
    }

    val serialized = PekkoSerialization.serialize(serialization)(flattenedRepr.payload).get

    JournalRow(
      Long.MinValue,
      deleted = false,
      flattenedRepr.persistenceId,
      flattenedRepr.sequenceNr,
      flattenedRepr.writerUuid,
      flattenedRepr.timestamp,
      flattenedRepr.manifest,
      serialized.payload,
      serialized.serId,
      serialized.serManifest,
      tags
    )
  }

}
