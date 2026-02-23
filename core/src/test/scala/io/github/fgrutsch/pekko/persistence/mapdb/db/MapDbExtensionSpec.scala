package io.github.fgrutsch.pekko.persistence.mapdb.db

import org.mapdb.Serializer
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import testing.TestActorSystem

import java.util.UUID
import scala.jdk.CollectionConverters._

class MapDbExtensionSpec extends AnyFunSuite with Matchers with TestActorSystem {

  test("creates a MapDB database") {
    val ext = MapDbExtension(actorSystem)

    val db  = ext.database
    val set = db.hashSet[String](UUID.randomUUID.toString, Serializer.STRING).createOrOpen()
    set.add("test-entry")

    set.asScala must contain only "test-entry"
  }

}
