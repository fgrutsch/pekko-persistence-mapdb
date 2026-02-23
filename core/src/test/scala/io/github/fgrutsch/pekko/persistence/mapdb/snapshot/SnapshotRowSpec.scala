package io.github.fgrutsch.pekko.persistence.mapdb.snapshot

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import scala.math.Ordering.comparatorToOrdering

class SnapshotRowSpec extends AnyFunSuite with Matchers {

  test("reverseSequenceNrComparator orders by sequenceNr field desc") {
    val unsorted = List(
      testRow(2),
      testRow(3),
      testRow(1)
    )

    val result = unsorted.sorted(using comparatorToOrdering(using SnapshotRow.reverseSequenceNrComparator))

    result mustBe List(
      testRow(3),
      testRow(2),
      testRow(1)
    )
  }

  private def testRow(seqNr: Long): SnapshotRow = {
    SnapshotRow("pid", seqNr, 0L, Array.emptyByteArray, 1, "")
  }

}
