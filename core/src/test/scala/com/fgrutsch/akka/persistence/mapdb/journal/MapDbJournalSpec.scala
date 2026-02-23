package com.fgrutsch.akka.persistence.mapdb.journal

import com.typesafe.config.ConfigFactory
import org.apache.pekko.persistence.CapabilityFlag
import org.apache.pekko.persistence.journal.JournalSpec

abstract class MapDbJournalSpec(configName: String) extends JournalSpec(ConfigFactory.load(configName)) {
  override protected def supportsRejectingNonSerializableObjects: CapabilityFlag = CapabilityFlag.on()
}

class FileDbJournalSpec              extends MapDbJournalSpec("file-db.conf")
class FileDbNoTransactionJournalSpec extends MapDbJournalSpec("file-db-no-transaction.conf")

class TempFileDbJournalSpec              extends MapDbJournalSpec("temp-file-db.conf")
class TempFileDbNoTransactionJournalSpec extends MapDbJournalSpec("temp-file-db-no-transaction.conf")

class MemoryDbJournalSpec              extends MapDbJournalSpec("memory-db.conf")
class MemoryDbNoTransactionJournalSpec extends MapDbJournalSpec("memory-db-no-transaction.conf")
