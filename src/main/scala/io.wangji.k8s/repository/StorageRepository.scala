package io.wangji.k8s.repository

import com.typesafe.scalalogging.StrictLogging
import zio.{Has, Layer, Task, ZIO, ZLayer}
import doobie.implicits.javasql._
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import doobie.free.connection.ConnectionIO


object StorageRepository extends StrictLogging{
    trait Service{
        def select(id: EntityId): Query0[Item]
        def selectByUserAndProject(username: String, project: String, repository:String): ConnectionIO[List[Item]]
        def insert(task: Item): ConnectionIO[Int]
        def delete(id: EntityId): Update0
    }

    object Service extends Service{
        override def select(id: EntityId): Query0[Item] = {
            logger.info("select item")
            sql"""
                 SELECT * FROM items
                 WHERE id = $id
            """.query[Item]    // doobie.implicits.javasql._
        }

        override def selectByUserAndProject(username: String, project:String, repository:String): ConnectionIO[List[Item]] = {
            logger.info("select item")
            sql"""
                 SELECT * FROM items
                 WHERE username = $username AND project = $project AND repository=$repository
            """.query[Item].to[List]
        }

        override def insert(file: Item): ConnectionIO[Int] = {
            logger.info("insert item")
            sql"""
                  INSERT INTO items(username, project, repository)
                  VALUES ( ${file.username}, ${file.project}, ${file.repository})
            """.update.withUniqueGeneratedKeys[Int]("id")
        }

        override def delete(id: EntityId): Update0 = {
            logger.info("delete item")
            sql"""
                  DELETE FROM items
                  WHERE id = ${id}
            """.update
        }
    }

    val live: Layer[Throwable, HasStorageRepository] = ZLayer.succeed(Service)

    object DSL {
        def select(id: EntityId): ZIO[HasStorageRepository, Throwable, Task[Query0[Item]]] =
            ZIO.access(f => Task.effect(f.get.select(id)))
        def selectByUserAndProject(username: String, project: String, repository:String): ZIO[HasStorageRepository, Throwable, Task[ConnectionIO[List[Item]]]] =
            ZIO.access(f => Task.effect(f.get.selectByUserAndProject(username, project, repository)))
        def insert(task: Item): ZIO[HasStorageRepository, Throwable, Task[ConnectionIO[Int] ]] =
            ZIO.access(f => Task.effect(f.get.insert(task)))
        def delete(id: EntityId): ZIO[HasStorageRepository, Throwable, Task[Update0]] =
            ZIO.access(f => Task.effect(f.get.delete(id)))
    }
}
