package io.github.fgrutsch.pekko.persistence.mapdb.snapshot

import com.typesafe.config.{Config, ConfigFactory}
import io.github.fgrutsch.pekko.persistence.mapdb.db.MapDbExtension
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import testing.TestActorSystem

import scala.jdk.CollectionConverters._

abstract class SnapshotRepositorySpec(configName: String)
    extends AnyFunSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers
    with OptionValues
    with TestActorSystem {

  private val snapshotConfig = new SnapshotConfig(actorSystem.settings.config.getConfig("mapdb-snapshot"))
  private val db             = MapDbExtension(actorSystem).database
  private val repo           = new MapDbSnapshotRepository(db, snapshotConfig.db)

  test("save inserts row if it doesn't exist for the given sequenceNr") {
    val row    = testRow("pid", 10)
    val result = repo.save(row)

    whenReady(result) { _ =>
      assert(rowsInDb)(row)
    }
  }

  test("save updates row if it exists for the given sequenceNr") {
    val result = for {
      _ <- repo.save(testRow("pid", 5))
      _ <- repo.save(testRow("pid", 10))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      _ <- repo.save(testRow("pid", 10, created = 100))
    } yield ()

    whenReady(result) { _ =>
      assert(rowsInDb)(
        testRow("pid", 5),
        testRow("pid", 10, created = 100),
        testRow("pid-2", 5),
        testRow("pid-2", 10)
      )
    }
  }

  test("find returns snapshot for given persistenceId") {
    val result = for {
      _ <- repo.save(testRow("pid", 5))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      r <- repo.find("pid", MapDbSnapshotRepository.FindFilter())
    } yield r

    whenReady(result) { r =>
      assert(r.value)(testRow("pid", 5))
    }
  }

  test("find returns snapshot with latest sequenceNr if there more than 1 for given persistenceId") {
    val result = for {
      _ <- repo.save(testRow("pid", 5))
      _ <- repo.save(testRow("pid", 10))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      r <- repo.find("pid", MapDbSnapshotRepository.FindFilter())
    } yield r

    whenReady(result) { r =>
      assert(r.value)(testRow("pid", 10))
    }
  }

  test("find returns snapshot for given persistenceId with maxSequenceNr filter") {
    val result = for {
      _ <- repo.save(testRow("pid", 5))
      _ <- repo.save(testRow("pid", 10))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      r <- repo.find("pid", MapDbSnapshotRepository.FindFilter(maxSequenceNr = Some(7)))
    } yield r

    whenReady(result) { r =>
      assert(r.value)(testRow("pid", 5))
    }
  }

  test("find returns snapshot for given persistenceId with maxTimestamp filter") {
    val result = for {
      _ <- repo.save(testRow("pid", 5, created = 100))
      _ <- repo.save(testRow("pid", 10, created = 500))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      r <- repo.find("pid", MapDbSnapshotRepository.FindFilter(maxTimestamp = Some(300)))
    } yield r

    whenReady(result) { r =>
      assert(r.value)(testRow("pid", 5, created = 100))
    }
  }

  test("find returns snapshot for given persistenceId with maxSequenceNr and maxTimestamp filter") {
    val result = for {
      _ <- repo.save(testRow("pid", 5, created = 100))
      _ <- repo.save(testRow("pid", 10, created = 500))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      r <- repo.find("pid", MapDbSnapshotRepository.FindFilter(Some(15), Some(300)))
    } yield r

    whenReady(result) { r =>
      assert(r.value)(testRow("pid", 5, created = 100))
    }
  }

  test("find returns no snapshot for given persistenceId") {
    val result = for {
      _ <- repo.save(testRow("pid", 5))
      _ <- repo.save(testRow("pid", 10))
      r <- repo.find("pid-2", MapDbSnapshotRepository.FindFilter())
    } yield r

    whenReady(result) { r =>
      r mustBe empty
    }
  }

  test("delete deletes snapshots for given persistenceId") {
    val result = for {
      _ <- repo.save(testRow("pid", 5))
      _ <- repo.save(testRow("pid", 10))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      _ <- repo.delete("pid", MapDbSnapshotRepository.DeleteFilter())
    } yield ()

    whenReady(result) { _ =>
      assert(rowsInDb)(
        testRow("pid-2", 5),
        testRow("pid-2", 10)
      )
    }
  }

  test("delete deletes snapshot for given persistenceId with maxSequenceNr filter") {
    val result = for {
      _ <- repo.save(testRow("pid", 5))
      _ <- repo.save(testRow("pid", 10))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      _ <- repo.delete("pid", MapDbSnapshotRepository.DeleteFilter(maxSequenceNr = Some(7)))
    } yield ()

    whenReady(result) { _ =>
      assert(rowsInDb)(
        testRow("pid", 10),
        testRow("pid-2", 5),
        testRow("pid-2", 10)
      )
    }
  }

  test("delete deletes snapshot for given persistenceId with maxTimestamp filter") {
    val result = for {
      _ <- repo.save(testRow("pid", 5, created = 100))
      _ <- repo.save(testRow("pid", 10, created = 500))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      _ <- repo.delete("pid", MapDbSnapshotRepository.DeleteFilter(maxTimestamp = Some(300)))
    } yield ()

    whenReady(result) { _ =>
      assert(rowsInDb)(
        testRow("pid", 10, created = 500),
        testRow("pid-2", 5),
        testRow("pid-2", 10)
      )
    }
  }

  test("delete deletes snapshot for given persistenceId with sequenceNr filter") {
    val result = for {
      _ <- repo.save(testRow("pid", 5))
      _ <- repo.save(testRow("pid", 10))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      _ <- repo.delete("pid", MapDbSnapshotRepository.DeleteFilter(sequenceNr = Some(10)))
    } yield ()

    whenReady(result) { _ =>
      assert(rowsInDb)(
        testRow("pid", 5),
        testRow("pid-2", 5),
        testRow("pid-2", 10)
      )
    }
  }

  test("delete deletes snapshot for given persistenceId with maxSequenceNr and maxTimestamp filter") {
    val result = for {
      _ <- repo.save(testRow("pid", 5, created = 100))
      _ <- repo.save(testRow("pid", 10, created = 500))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      _ <- repo.delete("pid", MapDbSnapshotRepository.DeleteFilter(Some(15), Some(300)))
    } yield ()

    whenReady(result) { _ =>
      assert(rowsInDb)(
        testRow("pid", 10, created = 500),
        testRow("pid-2", 5),
        testRow("pid-2", 10)
      )
    }
  }

  test("delete deletes no snapshot for given persistenceId if it doesn't exist") {
    val result = for {
      _ <- repo.save(testRow("pid", 5))
      _ <- repo.save(testRow("pid", 10))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
      _ <- repo.delete("pid-3", MapDbSnapshotRepository.DeleteFilter())
    } yield ()

    whenReady(result) { _ =>
      assert(rowsInDb)(
        testRow("pid", 5),
        testRow("pid", 10),
        testRow("pid-2", 5),
        testRow("pid-2", 10)
      )
    }
  }

  test("clear removes all rows") {
    val result = for {
      _ <- repo.save(testRow("pid", 5))
      _ <- repo.save(testRow("pid", 10))
      _ <- repo.save(testRow("pid-2", 5))
      _ <- repo.save(testRow("pid-2", 10))
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

  protected def rowsInDb: Seq[SnapshotRow] = repo.snapshots.asScala.toList

  protected def assert(actual: Seq[SnapshotRow])(expected: SnapshotRow*): Unit = {
    actual must have size expected.size

    expected.foreach { e =>
      actual.find(ac => ac.persistenceId == e.persistenceId && ac.sequenceNr == e.sequenceNr) match {
        case Some(actual) =>
          actual must have(
            Symbol("persistenceId")(e.persistenceId),
            Symbol("sequenceNr")(e.sequenceNr),
            Symbol("created")(e.created),
            Symbol("snapshotSerId")(e.snapshotSerId),
            Symbol("snapshotSerManifest")(e.snapshotSerManifest)
          )

        case None =>
          fail(s"Expected journal to contain element with persistenceId=${e.persistenceId}")
      }
    }
  }

  protected def assert(actual: SnapshotRow)(expected: SnapshotRow): Unit = {
    actual must have(
      Symbol("persistenceId")(expected.persistenceId),
      Symbol("sequenceNr")(expected.sequenceNr),
      Symbol("created")(expected.created),
      Symbol("snapshotSerId")(expected.snapshotSerId),
      Symbol("snapshotSerManifest")(expected.snapshotSerManifest)
    )
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

  protected def testRow(pid: String, seqNr: Long, created: Long = 0): SnapshotRow = {
    SnapshotRow(pid, seqNr, created, Array.emptyByteArray, 1, "")
  }

}
