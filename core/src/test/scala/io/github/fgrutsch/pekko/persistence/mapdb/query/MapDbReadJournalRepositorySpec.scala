package io.github.fgrutsch.pekko.persistence.mapdb.query

class FileDbReadJournalRepositorySpec              extends ReadJournalRepositorySpec("file-db.conf")
class FileDbNoTransactionReadJournalRepositorySpec extends ReadJournalRepositorySpec("file-db-no-transaction.conf")

class TempFileReadJournalRepositorySpec extends ReadJournalRepositorySpec("temp-file-db.conf")
class TempFileNoTransactionReadJournalRepositorySpec
    extends ReadJournalRepositorySpec("temp-file-db-no-transaction.conf")

class MemoryDbReadJournalRepositorySpec              extends ReadJournalRepositorySpec("memory-db.conf")
class MemoryDbNoTransactionReadJournalRepositorySpec extends ReadJournalRepositorySpec("memory-db-no-transaction.conf")
