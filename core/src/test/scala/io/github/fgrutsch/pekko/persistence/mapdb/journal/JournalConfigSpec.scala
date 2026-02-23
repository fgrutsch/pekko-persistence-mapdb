package io.github.fgrutsch.pekko.persistence.mapdb.journal

import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class JournalConfigSpec extends AnyFunSuite with Matchers {

  test("read a JournalConfig") {
    val config = ConfigFactory.parseString("""
      db.name = "test"
      db.commit-required = true
    """.stripMargin)

    val result = new JournalConfig(config)
    result.db.name mustBe "test"
    result.db.commitRequired mustBe true
  }

}
