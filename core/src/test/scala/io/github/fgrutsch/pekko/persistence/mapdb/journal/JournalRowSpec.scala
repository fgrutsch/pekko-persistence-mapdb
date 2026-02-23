package io.github.fgrutsch.pekko.persistence.mapdb.journal

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import scala.math.Ordering.comparatorToOrdering

class JournalRowSpec extends AnyFunSuite with Matchers {

  test("orderingComparator orders by ordering field asc") {
    val unsorted = List(
      testRow(100, 3),
      testRow(50, 1),
      testRow(75, 2)
    )

    val result = unsorted.sorted(using comparatorToOrdering(using JournalRow.orderingComparator))

    result mustBe List(
      testRow(50, 1),
      testRow(75, 2),
      testRow(100, 3)
    )
  }

  test("seqNrComparator orders by sequenceNr field asc") {
    val unsorted = List(
      testRow(100, 3),
      testRow(50, 1),
      testRow(75, 2)
    )

    val result = unsorted.sorted(using comparatorToOrdering(using JournalRow.orderingComparator))

    result mustBe List(
      testRow(50, 1),
      testRow(75, 2),
      testRow(100, 3)
    )
  }

  private def testRow(ordering: Long, seqNr: Long): JournalRow = {
    JournalRow(ordering, deleted = false, "pid", seqNr, "", 0L, "", Array.emptyByteArray, 1, "", Set.empty)
  }

}
