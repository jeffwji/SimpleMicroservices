package io.wangji.k8s.services

import java.io.{File, FileInputStream, FilterInputStream}
import java.security.MessageDigest

import akka.http.scaladsl.server.directives.FileInfo
import io.wangji.k8s.repository.{EntityId, Item, StorageRepository}
import io.wangji.k8s.configuration.{Configuration, HasConfiguration}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import doobie.Transactor
import zio.{Layer, Task, ZIO, ZLayer}
import doobie.implicits._
import zio.blocking.Blocking

object StorageService extends StrictLogging {
    import zio.interop.catz.taskConcurrentInstance

    trait Service[T[_]] {
        def find(id: EntityId): T[Item]
        def findByUserAndProject(username:String, project:String, repository:String): T[List[Item]]
        def create(user: Item): T[Int]
    }

    private def apply(tnx: Transactor[Task]): StorageService.Service[Task] = {
        import io.wangji.k8s.repository.StorageRepository.DSL._
        import io.wangji.k8s.configuration.Configuration.DSL._

        logger.info("Create UserService.Service")
        new Service[Task] {
            override def find(id: EntityId): Task[Item] = for{
                u <- select(id).provideLayer(StorageRepository.live) >>= { file =>
                    file.flatMap(_.option
                      .transact(tnx)
                      .foldM(
                          err => Task.fail(err),
                          (maybeFile: Option[Item]) => Task.require(new NoSuchElementException(s"$id"))(Task.succeed(maybeFile))
                      ))
                }
            } yield u

            override def findByUserAndProject(username:String, project:String, repository:String): Task[List[Item]] = for{
                u <- selectByUserAndProject(username, project, repository).provideLayer(StorageRepository.live) >>= { files =>
                    files.flatMap(_
                      .transact(tnx)
                      .foldM(
                          err => Task.fail(err),
                          (maybeFiles: List[Item]) => Task.require(new NoSuchElementException(s"$username, $project"))(Task.succeed(Option(maybeFiles)))
                      ))
                }
            } yield u

            override def create(file: Item): Task[EntityId] = for{
                u <- insert(file).provideLayer(StorageRepository.live) >>= { userId =>
                    userId.flatMap(_.transact(tnx)
                      .foldM(
                          err => Task.fail(err),
                          Task.succeed(_)
                      ))
                }
            } yield u
        }
    }

    def live(implicit tnx:Transactor[Task]): Layer[Throwable, HasStorageService] = {
        ZLayer.succeed{
            StorageService(tnx)
        }
    }

    object DSL {
        def create(user:Item): ZIO[HasStorageService, Throwable, Int] =
            ZIO.accessM(f => f.get.create(user))
        def find(id: EntityId): ZIO[HasStorageService, Throwable, Item] =
            ZIO.accessM(f => f.get.find(id))
        def findByUserAndProject(username:String, project:String, repository:String): ZIO[HasStorageService, Throwable, List[Item]] =
            ZIO.accessM(f => f.get.findByUserAndProject(username, project, repository))
    }
}
