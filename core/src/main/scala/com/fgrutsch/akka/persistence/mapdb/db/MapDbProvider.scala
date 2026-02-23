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

import com.typesafe.config.Config
import org.mapdb._

private[db] object MapDbProvider {

  class BaseConfig(config: Config) {
    val mode: String                = config.getString("mode")
    val transactionEnable: Boolean  = config.getBoolean("transaction-enable")
    val closeOnJvmShutdown: Boolean = config.getBoolean("close-on-jvm-shutdown")
    val fileDbConfig: FileDbConfig  = new FileDbConfig(config)
    override def toString: String   =
      s"${getClass.getSimpleName}($mode,$transactionEnable,$closeOnJvmShutdown,$fileDbConfig)"
  }

  class FileDbConfig(config: Config) {
    val path: String              = config.getString("file.path")
    val deleteAfterClose: Boolean = config.getBoolean("file.delete-after-close")
    override def toString: String = s"${getClass.getSimpleName}($path,$deleteAfterClose)"
  }

}

private[db] class MapDbProvider(config: Config) {

  private val baseDbConf = new MapDbProvider.BaseConfig(config.getConfig("pekko-persistence-mapdb.db"))
  private val fileDbConf = baseDbConf.fileDbConfig

  def setup(): DB = {
    val maker = baseDbConf.mode match {
      case "memory"    => DBMaker.memoryDB()
      case "file"      => DBMaker.fileDB(fileDbConf.path).fileDeleteAfterClose(fileDbConf.deleteAfterClose)
      case "temp-file" => DBMaker.tempFileDB()
    }

    maker
      .transactionEnable(baseDbConf.transactionEnable)
      .closeOnJvmShutdown(baseDbConf.closeOnJvmShutdown)
      .make()
  }

  implicit private class DBMakerOps(maker: DBMaker.Maker) {
    def transactionEnable(value: Boolean): DBMaker.Maker    = if (value) maker.transactionEnable() else maker
    def closeOnJvmShutdown(value: Boolean): DBMaker.Maker   = if (value) maker.closeOnJvmShutdown() else maker
    def fileDeleteAfterClose(value: Boolean): DBMaker.Maker = if (value) maker.fileDeleteAfterClose() else maker
  }

}
