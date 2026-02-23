package io.github.fgrutsch.pekko.persistence.mapdb.snapshot

class FileDbSnapshotRepositorySpec              extends SnapshotRepositorySpec("file-db.conf")
class FileDbNoTransactionSnapshotRepositorySpec extends SnapshotRepositorySpec("file-db-no-transaction.conf")

class TempFileSnapshotRepositorySpec              extends SnapshotRepositorySpec("temp-file-db.conf")
class TempFileNoTransactionSnapshotRepositorySpec extends SnapshotRepositorySpec("temp-file-db-no-transaction.conf")

class MemoryDbSnapshotRepositorySpec              extends SnapshotRepositorySpec("memory-db.conf")
class MemoryDbNoTransactionSnapshotRepositorySpec extends SnapshotRepositorySpec("memory-db-no-transaction.conf")
