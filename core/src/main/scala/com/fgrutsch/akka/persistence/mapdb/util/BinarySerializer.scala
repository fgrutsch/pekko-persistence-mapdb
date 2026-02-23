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

package com.fgrutsch.akka.persistence.mapdb.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

private[mapdb] object BinarySerializer {

  def serialize[T](obj: T): Array[Byte] = {
    val byteOut = new ByteArrayOutputStream()
    val objOut  = new ObjectOutputStream(byteOut)
    objOut.writeObject(obj)
    objOut.close()
    byteOut.close()
    byteOut.toByteArray
  }

  def deserialize[T](bytes: Array[Byte]): T = {
    val byteIn = new ByteArrayInputStream(bytes)
    val objIn  = new ObjectInputStream(byteIn)
    val obj    = objIn.readObject().asInstanceOf[T]
    byteIn.close()
    objIn.close()
    obj
  }

}
