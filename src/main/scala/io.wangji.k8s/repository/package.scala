package io.wangji.k8s

import java.sql.Timestamp

import zio.{Has, Task}
import doobie.Transactor

package object repository {
    type HasTransactor = Has[Transactor[Task]]
    type HasStorageRepository = Has[StorageRepository.Service]
    type EntityId = Int

    sealed trait Entity{
        val id: Option[EntityId]
    }

    final case class Item(id:Option[EntityId]
                          , username:Option[String]
                          , project:Option[String]
                          , repository:Option[String]
                          , created:Option[Timestamp]
                        ) extends Entity

    sealed trait RepoError
}
