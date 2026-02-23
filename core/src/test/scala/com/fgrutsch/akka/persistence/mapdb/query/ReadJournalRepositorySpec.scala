package com.fgrutsch.akka.persistence.mapdb.query

import com.fgrutsch.akka.persistence.mapdb.db.MapDbExtension
import com.fgrutsch.akka.persistence.mapdb.journal.{JournalConfig, JournalRow, MapDbJournalRepository}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.stream.scaladsl.Sink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import testing.TestActorSystem

abstract class ReadJournalRepositorySpec(configName: String)
    extends AnyFunSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers
    with TestActorSystem {

  private val journalConfig     = new JournalConfig(actorSystem.settings.config.getConfig("mapdb-journal"))
  private val readJournalConfig = new ReadJournalConfig(actorSystem.settings.config.getConfig("mapdb-read-journal"))
  private val db                = MapDbExtension(actorSystem).database
  private val journalRepo       = new MapDbJournalRepository(db, journalConfig.db)
  private val readJournalRepo   = new MapDbReadJournalRepository(db, readJournalConfig.db)

  test("allPersistenceIds returns no rows if there aren't any") {
    val result = readJournalRepo.allPersistenceIds().runWith(Sink.seq)

    whenReady(result) { actual =>
      actual mustBe empty
    }
  }

  test("allPersistenceIds returns all persistence IDs distinct") {
    val rowsPid1 = Seq(
      testRow(Long.MinValue, "p-1", 1),
      testRow(Long.MinValue, "p-1", 2)
    )
    val rowsPid2 = Seq(
      testRow(Long.MinValue, "p-2", 1),
      testRow(Long.MinValue, "p-2", 2)
    )

    val result = for {
      _ <- journalRepo.insert(rowsPid1)
      _ <- journalRepo.insert(rowsPid2)
      r <- readJournalRepo.allPersistenceIds().runWith(Sink.seq)
    } yield r

    whenReady(result) { actual =>
      actual mustBe List("p-1", "p-2")
    }
  }

  test("highestOrdering returns 0 if there are no entries") {
    val result = readJournalRepo.highestOrdering()

    whenReady(result) { actual =>
      actual mustBe 0
    }
  }

  test("highestOrdering returns highest ordering") {
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
      _ <- journalRepo.insert(rowsPid1)
      _ <- journalRepo.insert(rowsPid2)
      r <- readJournalRepo.highestOrdering()
    } yield r

    whenReady(result) { actual =>
      actual mustBe 8
    }
  }

  test("list by persistenceId returns nothing if there are no entries") {
    val result = readJournalRepo.list("-1", 0, Long.MaxValue, Long.MaxValue).runWith(Sink.seq)

    whenReady(result) { actual =>
      actual mustBe empty
    }
  }

  test("list by persistenceId returns entries with fromSequenceNr filter") {
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
      _ <- journalRepo.insert(rowsPid1)
      _ <- journalRepo.insert(rowsPid2)
      r <- readJournalRepo.list("p-2", 3, Long.MaxValue, Long.MaxValue).runWith(Sink.seq)
    } yield r

    whenReady(result) { actual =>
      assert(actual)(
        testRow(5, "p-2", 3),
        testRow(6, "p-2", 4),
        testRow(7, "p-2", 5),
        testRow(8, "p-2", 6)
      )
    }
  }

  test("list by persistenceId returns entries with toSequenceNr filter") {
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
      _ <- journalRepo.insert(rowsPid1)
      _ <- journalRepo.insert(rowsPid2)
      r <- readJournalRepo.list("p-2", 0, 4, Long.MaxValue).runWith(Sink.seq)
    } yield r

    whenReady(result) { actual =>
      assert(actual)(
        testRow(3, "p-2", 1),
        testRow(4, "p-2", 2),
        testRow(5, "p-2", 3),
        testRow(6, "p-2", 4)
      )
    }
  }

  test("list by persistenceId returns entries with max filter") {
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
      _ <- journalRepo.insert(rowsPid1)
      _ <- journalRepo.insert(rowsPid2)
      r <- readJournalRepo.list("p-2", 0, Long.MaxValue, 4).runWith(Sink.seq)
    } yield r

    whenReady(result) { actual =>
      assert(actual)(
        testRow(3, "p-2", 1),
        testRow(4, "p-2", 2),
        testRow(5, "p-2", 3),
        testRow(6, "p-2", 4)
      )
    }
  }

  test("list by tag returns nothing if there are no entries") {
    val result = readJournalRepo.list("tag", 0, Long.MaxValue).runWith(Sink.seq)

    whenReady(result) { actual =>
      actual mustBe empty
    }
  }

  test("list by tag returns entries with offset filter") {
    val rowsPid1 = Seq(
      testRow(Long.MinValue, "p-1", 1, Some("tag")),
      testRow(Long.MinValue, "p-1", 2, Some("tag2"))
    )

    val rowsPid2 = Seq(
      testRow(Long.MinValue, "p-2", 1, Some("tag")),
      testRow(Long.MinValue, "p-2", 2, Some("tag2")),
      testRow(Long.MinValue, "p-2", 3, Some("tag")),
      testRow(Long.MinValue, "p-2", 4, Some("tag2")),
      testRow(Long.MinValue, "p-2", 5, Some("tag")),
      testRow(Long.MinValue, "p-2", 6, Some("tag2"))
    )

    val result = for {
      _ <- journalRepo.insert(rowsPid1)
      _ <- journalRepo.insert(rowsPid2)
      r <- readJournalRepo.list("tag", 3, Long.MaxValue).runWith(Sink.seq)
    } yield r

    whenReady(result) { actual =>
      assert(actual)(
        testRow(5, "p-2", 3, Some("tag")),
        testRow(7, "p-2", 5, Some("tag"))
      )
    }
  }

  test("list by tag returns entries with max filter") {
    val rowsPid1 = Seq(
      testRow(Long.MinValue, "p-1", 1, Some("tag")),
      testRow(Long.MinValue, "p-1", 2, Some("tag2"))
    )

    val rowsPid2 = Seq(
      testRow(Long.MinValue, "p-2", 1, Some("tag")),
      testRow(Long.MinValue, "p-2", 2, Some("tag2")),
      testRow(Long.MinValue, "p-2", 3, Some("tag")),
      testRow(Long.MinValue, "p-2", 4, Some("tag2")),
      testRow(Long.MinValue, "p-2", 5, Some("tag")),
      testRow(Long.MinValue, "p-2", 6, Some("tag2"))
    )

    val result = for {
      _ <- journalRepo.insert(rowsPid1)
      _ <- journalRepo.insert(rowsPid2)
      r <- readJournalRepo.list("tag", 0, 2).runWith(Sink.seq)
    } yield r

    whenReady(result) { actual =>
      assert(actual)(
        testRow(1, "p-1", 1, Some("tag")),
        testRow(3, "p-2", 1, Some("tag"))
      )
    }
  }

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
    journalRepo.clear().futureValue
  }

  override def afterAll(): Unit = {
    super.afterAll()
    db.close()
  }

  implicit override def patienceConfig: PatienceConfig = PatienceConfig(Span(3, Seconds))

  protected def testRow(ordering: Long, pid: String, seqNr: Long, tag: Option[String] = None): JournalRow = {
    JournalRow(ordering, false, pid, seqNr, "", 0L, "", Array.emptyByteArray, 1, "", tag.toSet)
  }

}
