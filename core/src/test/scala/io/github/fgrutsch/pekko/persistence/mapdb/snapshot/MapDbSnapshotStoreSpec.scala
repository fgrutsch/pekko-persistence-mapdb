package io.github.fgrutsch.pekko.persistence.mapdb.snapshot

import com.typesafe.config.ConfigFactory
import org.apache.pekko.persistence.snapshot.SnapshotStoreSpec

abstract class MapDbSnapshotStoreSpec(configName: String) extends SnapshotStoreSpec(ConfigFactory.load(configName))

class FileDbSnapshotStoreSpec              extends MapDbSnapshotStoreSpec("file-db.conf")
class FileDbNoTransactionSnapshotStoreSpec extends MapDbSnapshotStoreSpec("file-db-no-transaction.conf")

class TempFileDbSnapshotStoreSpec              extends MapDbSnapshotStoreSpec("temp-file-db.conf")
class TempFileDbNoTransactionSnapshotStoreSpec extends MapDbSnapshotStoreSpec("temp-file-db-no-transaction.conf")

class MemoryDbSnapshotStoreSpec              extends MapDbSnapshotStoreSpec("memory-db.conf")
class MemoryDbNoTransactionSnapshotStoreSpec extends MapDbSnapshotStoreSpec("memory-db-no-transaction.conf")
