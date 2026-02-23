/*
 * Copyright 2026 pekko-persistence-mapdb contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.fgrutsch.pekko.persistence.mapdb

import org.apache.pekko.persistence.query._

package object query {
  implicit class OffsetOps(val underlying: Offset) extends AnyVal {
    def value = {
      underlying match {
        case Sequence(offsetValue) => offsetValue
        case NoOffset              => 0L
        case _ => throw new IllegalArgumentException(s"${underlying.getClass.getName} offset type not supported.")
      }
    }
  }
}
