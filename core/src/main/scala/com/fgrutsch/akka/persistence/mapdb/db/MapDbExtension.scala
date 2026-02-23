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

package com.fgrutsch.akka.persistence.mapdb.db

import org.apache.pekko.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId}
import org.mapdb.DB

object MapDbExtension extends ExtensionId[MapDbExtensionImpl] {
  override def createExtension(system: ExtendedActorSystem): MapDbExtensionImpl = new MapDbExtensionImpl(system)
}

class MapDbExtensionImpl(system: ActorSystem) extends Extension {

  private val dbProvider = new MapDbProvider(system.settings.config)
  val database: DB       = dbProvider.setup()

  system.registerOnTermination(database.close())

}
