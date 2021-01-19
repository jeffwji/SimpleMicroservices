package io.wangji.k8s.routes

import java.io.{File, InputStream}

import akka.http.scaladsl.server.Directives._
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, get, pathPrefix}
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.server.Route
import io.wangji.k8s.repository.Item
import io.wangji.k8s.services.HasStorageService
import com.typesafe.scalalogging.StrictLogging
import zio.{Layer, Task, ZIO, ZLayer}
import io.wangji.k8s.implicits._



object StorageController {
    def tempDestination(fileInfo: FileInfo): File = File.createTempFile(fileInfo.fileName, ".tmp")

    trait Service[T[_]] {
        def apply()(implicit serverLayer: Layer[Throwable, HasStorageService], system:ActorSystem): T[Route]
    }

    object Service extends StorageController.Service[Task] with MarshallingSupport with StrictLogging{
        import io.wangji.k8s.services.StorageService.DSL._

        import io.circe.generic.auto._
        import io.circe.syntax._

        override def apply()(implicit serviceLayer: Layer[Throwable, HasStorageService], system:ActorSystem): Task[Route] = Task.effect{
            get {
                /** List task */
                pathPrefix(Segment / Segment / Segment) { (user, project, repository) =>
                    findByUserAndProject(user, project, repository).provideLayer(serviceLayer).foldM(
                        (t: Throwable) => Task.fail(t),
                        result => Task.succeed(result)
                    ).map{
                        case results:List[Item] =>
                            logger.info(results.asJson.toString())
                            HttpResponse(
                                StatusCodes.OK,
                                entity = HttpEntity(results.asJson.toString())
                            )
                        case _ => HttpResponse(StatusCodes.NotFound)
                    }.map(complete(_))
                }
            } ~ get {
                pathPrefix(IntNumber) { id =>
                    /** Get file information */
                    find(id).provideLayer(serviceLayer).foldM(
                        (t: Throwable) => Task.fail(t),
                        result => Task.succeed(result)
                    ).map{
                        case result:Item => HttpResponse(
                            StatusCodes.OK,
                            entity = HttpEntity(result.asJson.toString())
                        )
                        case _ => HttpResponse(StatusCodes.NotFound)
                    }.map(complete(_))
                }
            } ~ get {
                pathPrefix(IntNumber) { id =>
                    /** Get file information */
                    find(id).provideLayer(serviceLayer).foldM(
                        (t: Throwable) => Task.fail(t),
                        result => Task.succeed(result)
                    ).map{
                        case result:Item => HttpResponse(
                            StatusCodes.OK,
                            entity = HttpEntity(result.asJson.toString())
                        )
                        case _ => HttpResponse(StatusCodes.NotFound)
                    }.map(complete(_))
                }
            }
        }
    }

    def live: Layer[Throwable, HasStorageController] = ZLayer.succeed(Service)

    object DSL {
        def homeRoute(implicit serviceLayer: Layer[Throwable, HasStorageService],system:ActorSystem):ZIO[HasStorageController, Throwable, Route] =
            ZIO.accessM(_.get.apply())
    }
}
