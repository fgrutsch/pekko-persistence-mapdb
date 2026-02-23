package io.github.fgrutsch.pekko.persistence.mapdb.journal

class FileDbJournalRepositorySpec              extends JournalRepositorySpec("file-db.conf")
class FileDbNoTransactionJournalRepositorySpec extends JournalRepositorySpec("file-db-no-transaction.conf")

class TempFileJournalRepositorySpec              extends JournalRepositorySpec("temp-file-db.conf")
class TempFileNoTransactionJournalRepositorySpec extends JournalRepositorySpec("temp-file-db-no-transaction.conf")

class MemoryDbJournalRepositorySpec              extends JournalRepositorySpec("memory-db.conf")
class MemoryDbNoTransactionJournalRepositorySpec extends JournalRepositorySpec("memory-db-no-transaction.conf")
