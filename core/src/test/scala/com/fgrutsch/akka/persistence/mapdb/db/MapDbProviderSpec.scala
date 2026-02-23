package com.fgrutsch.akka.persistence.mapdb.db

import com.typesafe.config.{Config, ConfigFactory}
import org.mapdb.Serializer
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import java.util.UUID
import scala.jdk.CollectionConverters._

class MapDbProviderSpec extends AnyFunSuite with Matchers {

  test("setup a memory MapDB database") {
    val config = ConfigFactory.parseString("""
      pekko-persistence-mapdb.db.mode = "memory"
      pekko-persistence-mapdb.db.transaction-enable = true
      pekko-persistence-mapdb.db.close-on-jvm-shutdown = true
      pekko-persistence-mapdb.db.file.path = "pekko-persistence-mapdb_test"
      pekko-persistence-mapdb.db.file.delete-after-close = true
    """.stripMargin)

    check(config)
  }

  test("setup a file MapDB database") {
    val config = ConfigFactory.parseString("""
      pekko-persistence-mapdb.db.mode = "file"
      pekko-persistence-mapdb.db.transaction-enable = true
      pekko-persistence-mapdb.db.close-on-jvm-shutdown = true
      pekko-persistence-mapdb.db.file.path = "pekko-persistence-mapdb_test"
      pekko-persistence-mapdb.db.file.delete-after-close = true
    """.stripMargin)

    check(config)
  }

  test("setup a temp file MapDB database") {
    val config = ConfigFactory.parseString("""
      pekko-persistence-mapdb.db.mode = "temp-file"
      pekko-persistence-mapdb.db.transaction-enable = true
      pekko-persistence-mapdb.db.close-on-jvm-shutdown = true
      pekko-persistence-mapdb.db.file.path = "pekko-persistence-mapdb_test"
      pekko-persistence-mapdb.db.file.delete-after-close = true
    """.stripMargin)

    check(config)
  }

  private def check(config: Config): Unit = {
    val provider = new MapDbProvider(config)

    val db  = provider.setup()
    val set = db.hashSet[String](UUID.randomUUID.toString, Serializer.STRING).createOrOpen()
    set.add("test-entry")
    set.add("test-entry2")

    set.asScala must contain only ("test-entry", "test-entry2")
    db.close()
  }

}
