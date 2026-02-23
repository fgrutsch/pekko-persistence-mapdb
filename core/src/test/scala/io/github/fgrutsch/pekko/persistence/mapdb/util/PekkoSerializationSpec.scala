package io.github.fgrutsch.pekko.persistence.mapdb.util

import com.typesafe.config.{Config, ConfigFactory}
import io.github.fgrutsch.pekko.persistence.mapdb.journal.JournalRow
import io.github.fgrutsch.pekko.persistence.mapdb.snapshot.SnapshotRow
import io.github.fgrutsch.pekko.persistence.mapdb.util.PekkoSerializationSpec.TestMessage
import org.apache.pekko.persistence.{PersistentRepr, SnapshotMetadata}
import org.apache.pekko.serialization.SerializationExtension
import org.scalatest.TryValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import testing.{PekkoSerializable, TestActorSystem}

import java.time.Instant

object PekkoSerializationSpec {
  final case class TestMessage(name: String, birthYear: Int) extends PekkoSerializable
}

class PekkoSerializationSpec extends AnyFunSuite with Matchers with TryValues with TestActorSystem {

  private val serialization = SerializationExtension(actorSystem)

  test("serialize encodes a journal message") {
    val msg    = TestMessage("mapdb", 2021)
    val result = PekkoSerialization.serialize(serialization)(msg)

    val actual = result.success.value
    actual.serId mustBe 33
    actual.serManifest mustBe "io.github.fgrutsch.pekko.persistence.mapdb.util.PekkoSerializationSpec$TestMessage"
    actual.payload must not be empty
  }

  test("deserialize decodes a journal message") {
    val msg           = TestMessage("mapdb", 2021)
    val msgSerialized = PekkoSerialization.serialize(serialization)(msg)

    val row = JournalRow(
      1,
      deleted = false,
      "pid",
      1,
      "writer",
      Instant.EPOCH.toEpochMilli,
      "TestMessage",
      msgSerialized.success.value.payload,
      msgSerialized.success.value.serId,
      msgSerialized.success.value.serManifest,
      Set.empty
    )

    val result           = PekkoSerialization.fromJournalRow(serialization)(row)
    val (repr, ordering) = result.success.value
    repr mustBe PersistentRepr(msg, 1, "pid", writerUuid = "writer")
    ordering mustBe 1
  }

  test("serialize encodes a snapshot message") {
    val msg    = TestMessage("mapdb", 2021)
    val result = PekkoSerialization.serialize(serialization)(msg)

    val actual = result.success.value
    actual.serId mustBe 33
    actual.serManifest mustBe "io.github.fgrutsch.pekko.persistence.mapdb.util.PekkoSerializationSpec$TestMessage"
    actual.payload must not be empty
  }

  test("deserialize decodes a snapshot message") {
    val msg           = TestMessage("mapdb", 2021)
    val msgSerialized = PekkoSerialization.serialize(serialization)(msg)

    val row = SnapshotRow(
      "pid",
      1,
      Instant.EPOCH.toEpochMilli,
      msgSerialized.success.value.payload,
      msgSerialized.success.value.serId,
      msgSerialized.success.value.serManifest
    )

    val result = PekkoSerialization.fromSnapshotRow(serialization)(row)
    val actual = result.success.value
    actual.metadata mustBe SnapshotMetadata("pid", 1, Instant.EPOCH.toEpochMilli)
    actual.snapshot mustBe a[TestMessage]

  }

  override protected def systemConfig: Config = {
    ConfigFactory.parseString("""
        |pekko.actor {
        |  serialization-bindings {
        |    "testing.PekkoSerializable" = jackson-cbor
        |  }
        |}
        |""".stripMargin)

  }

}
