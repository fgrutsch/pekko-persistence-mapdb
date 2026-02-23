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

package com.fgrutsch.akka.persistence.mapdb.journal

import com.fgrutsch.akka.persistence.mapdb.journal.JournalConfig.DbConfig
import com.typesafe.config.Config

object JournalConfig {

  class DbConfig(config: Config) {
    val name: String              = config.getString("name")
    val commitRequired: Boolean   = config.getBoolean("commit-required")
    override def toString: String = s"${getClass.getSimpleName}($name,$commitRequired)"
  }

}

class JournalConfig(config: Config) {
  val db: DbConfig              = new DbConfig(config.getConfig("db"))
  override def toString: String = s"${getClass.getSimpleName}($db)"
}
