package io.wangji.k8s

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives.withRequestTimeout
import akka.stream.Materializer
import cats.effect.Resource
import zio.{ExitCode, Task, ZEnv, ZIO}
import io.wangji.k8s.configuration.{AkkaActorSystem, Configuration, HasActorSystem}
import io.wangji.k8s.repository.{HasTransactor, Persistence}
import io.wangji.k8s.routes.{HasStorageController, StorageController}
import io.wangji.k8s.services.StorageService
import com.typesafe.scalalogging.StrictLogging
import doobie.util.ExecutionContexts
import zio.interop.catz._
import zio.blocking.Blocking

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object MainEntry extends zio.App with StrictLogging{
    import AkkaActorSystem.DSL._
    import Persistence.DSL._
    import StorageController.DSL._

    implicit val databaseExecutionContextResource: Resource[Task, ExecutionContext] = ExecutionContexts.fixedThreadPool[Task](5)

    override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
        val program:ZIO[HasActorSystem with HasTransactor with HasStorageController, Throwable, ServerBinding] = for {
            tnx <- transactor   // Supports HasTransactor (Persistent)
            sys <- actorSystem
            handle: ServerBinding <- homeRoute(StorageService.live(tnx), sys) >>= { route =>
                implicit val s: ActorSystem = sys
                implicit val mat: Materializer = Materializer(sys)
                Task.fromFuture{ _ =>
                    Http().newServerAt("0.0.0.0", 9000).bind{
                        withRequestTimeout(Duration.Inf){
                            route
                        }
                    }
                }
            }
            _ <- Task.never
        } yield {
            handle
        }

        databaseExecutionContextResource.use(implicit ec =>
            program.provideLayer( AkkaActorSystem.live ++ (
              (Configuration.live ++ Blocking.live) >>> Persistence.live(ec) ++ StorageController.live
            ) )
        ).map(handle => {handle.unbind()}).exitCode
    }
}
