package com.fgrutsch.akka.persistence.mapdb.journal

import com.fgrutsch.akka.persistence.mapdb.db.MapDbExtension
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.stream.scaladsl.Sink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import testing.TestActorSystem

import scala.jdk.CollectionConverters._

abstract class JournalRepositorySpec(configName: String)
    extends AnyFunSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers
    with TestActorSystem {

  private val journalConfig = new JournalConfig(actorSystem.settings.config.getConfig("mapdb-journal"))
  private val db            = MapDbExtension(actorSystem).database
  private val repo          = new MapDbJournalRepository(db, journalConfig.db)

  test("insert adds all rows to the journal") {
    val rows = Seq(
      testRow(Long.MinValue, "p-1", 3),
      testRow(Long.MinValue, "p-1", 1),
      testRow(Long.MinValue, "p-1", 2)
    )

    val result = repo.insert(rows)

    whenReady(result) { _ =>
      assert(rowsInDb)(
        testRow(1, "p-1", 1),
        testRow(2, "p-1", 2),
        testRow(3, "p-1", 3)
      )
    }
  }

  test("insert adds no rows to the journal") {
    val rows   = Seq.empty
    val result = repo.insert(rows)

    whenReady(result) { _ =>
      assert(rowsInDb)()
    }
  }

  test("list returns all rows for the given persistenceId and filtering") {
    val rowsPid1 = Seq(
      testRow(Long.MinValue, "p-1", 1),
      testRow(Long.MinValue, "p-1", 2)
    )

    val rowsPid2 = Seq(
      testRow(Long.MinValue, "p-2", 1),
      testRow(Long.MinValue, "p-2", 2),
      testRow(Long.MinValue, "p-2", 3),
      testRow(Long.MinValue, "p-2", 4),
      testRow(Long.MinValue, "p-2", 5),
      testRow(Long.MinValue, "p-2", 6)
    )

    val result = for {
      _ <- repo.insert(rowsPid1)
      _ <- repo.insert(rowsPid2)
      r <- repo.list("p-2", 2, 5, 3).runWith(Sink.seq)
    } yield r

    whenReady(result) { actual =>
      assert(actual)(
        testRow(4, "p-2", 2),
        testRow(5, "p-2", 3),
        testRow(6, "p-2", 4)
      )
    }
  }

  test("list returns no rows for the given persistenceId") {
    val rows = Seq(
      testRow(Long.MinValue, "p-1", 1),
      testRow(Long.MinValue, "p-1", 2)
    )

    val result = for {
      _ <- repo.insert(rows)
      r <- repo.list("p-2", 0, Long.MaxValue, Long.MaxValue).runWith(Sink.seq)
    } yield r

    whenReady(result) { actual =>
      assert(actual)()
    }
  }

  test("highestSequenceNr gets highest sequenceNr for persistenceId") {
    val rowsPid1 = Seq(
      testRow(Long.MinValue, "p-1", 1),
      testRow(Long.MinValue, "p-1", 2)
    )

    val rowsPid2 = Seq(
      testRow(Long.MinValue, "p-2", 1),
      testRow(Long.MinValue, "p-2", 2),
      testRow(Long.MinValue, "p-2", 3),
      testRow(Long.MinValue, "p-2", 4),
      testRow(Long.MinValue, "p-2", 5),
      testRow(Long.MinValue, "p-2", 6)
    )

    val result = for {
      _ <- repo.insert(rowsPid1)
      _ <- repo.insert(rowsPid2)
      r <- repo.highestSequenceNr("p-2")
    } yield r

    whenReady(result) { actual =>
      actual mustBe 6
    }
  }

  test("highestSequenceNr gets 0 for non existing persistenceId") {
    val rows = Seq(
      testRow(Long.MinValue, "p-1", 1),
      testRow(Long.MinValue, "p-1", 2)
    )

    val result = for {
      _ <- repo.insert(rows)
      r <- repo.highestSequenceNr("p-2")
    } yield r

    whenReady(result) { actual =>
      actual mustBe 0
    }
  }

  test("delete marks all rows as deleted for persistenceId and filtering") {
    val rowsPid1 = Seq(
      testRow(Long.MinValue, "p-1", 1),
      testRow(Long.MinValue, "p-1", 2)
    )

    val rowsPid2 = Seq(
      testRow(Long.MinValue, "p-2", 1),
      testRow(Long.MinValue, "p-2", 2),
      testRow(Long.MinValue, "p-2", 3),
      testRow(Long.MinValue, "p-2", 4),
      testRow(Long.MinValue, "p-2", 5),
      testRow(Long.MinValue, "p-2", 6)
    )

    val result = for {
      _ <- repo.insert(rowsPid1)
      _ <- repo.insert(rowsPid2)
      r <- repo.delete("p-2", 5)
    } yield r

    whenReady(result) { _ =>
      assert(rowsInDb)(
        testRow(1, "p-1", 1),
        testRow(2, "p-1", 2),
        testRow(3, "p-2", 1, deleted = true),
        testRow(4, "p-2", 2, deleted = true),
        testRow(5, "p-2", 3, deleted = true),
        testRow(6, "p-2", 4, deleted = true),
        testRow(7, "p-2", 5, deleted = true),
        testRow(8, "p-2", 6)
      )
    }
  }

  test("delete marks nothing as deleted for non existing persistenceId") {
    val rows = Seq(
      testRow(Long.MinValue, "p-1", 1),
      testRow(Long.MinValue, "p-1", 2)
    )

    val result = for {
      _ <- repo.insert(rows)
      r <- repo.delete("p-2", Long.MaxValue)
    } yield r

    whenReady(result) { _ =>
      assert(rowsInDb)(
        testRow(1, "p-1", 1),
        testRow(2, "p-1", 2)
      )
    }
  }

  test("clear removes all rows") {
    val rowsPid1 = Seq(
      testRow(Long.MinValue, "p-1", 1),
      testRow(Long.MinValue, "p-1", 2)
    )

    val rowsPid2 = Seq(
      testRow(Long.MinValue, "p-2", 1),
      testRow(Long.MinValue, "p-2", 2),
      testRow(Long.MinValue, "p-2", 3),
      testRow(Long.MinValue, "p-2", 4),
      testRow(Long.MinValue, "p-2", 5),
      testRow(Long.MinValue, "p-2", 6)
    )

    val result = for {
      _ <- repo.insert(rowsPid1)
      _ <- repo.insert(rowsPid2)
      r <- repo.clear()
    } yield r

    whenReady(result) { _ =>
      assert(rowsInDb)()
    }
  }

  test("clear removes nothing if there are no rows") {
    val result = repo.clear()

    whenReady(result) { _ =>
      assert(rowsInDb)()
    }
  }

  protected def rowsInDb: Seq[JournalRow] = repo.journals.asScala.toList

  protected def assert(actual: Seq[JournalRow])(expected: JournalRow*): Unit = {
    actual must have size expected.size

    expected.foreach { e =>
      actual.find(_.ordering == e.ordering) match {
        case Some(actual) =>
          actual must have(
            Symbol("ordering")(e.ordering),
            Symbol("deleted")(e.deleted),
            Symbol("persistenceId")(e.persistenceId),
            Symbol("sequenceNr")(e.sequenceNr),
            Symbol("writer")(e.writer),
            Symbol("timestamp")(e.timestamp),
            Symbol("manifest")(e.manifest),
            Symbol("eventSerId")(e.eventSerId),
            Symbol("eventSerManifest")(e.eventSerManifest),
            Symbol("tags")(e.tags)
          )

        case None =>
          fail(s"Expected journal to contain element with ordering=${e.ordering}")
      }
    }
  }

  override protected def systemConfig: Config = ConfigFactory.load(configName)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repo.clear().futureValue
  }

  override def afterAll(): Unit = {
    super.afterAll()
    db.close()
  }

  implicit override def patienceConfig: PatienceConfig = PatienceConfig(Span(3, Seconds))

  protected def testRow(ordering: Long, pid: String, seqNr: Long, deleted: Boolean = false): JournalRow = {
    JournalRow(ordering, deleted, pid, seqNr, "", 0L, "", Array.emptyByteArray, 1, "", Set.empty)
  }

}
