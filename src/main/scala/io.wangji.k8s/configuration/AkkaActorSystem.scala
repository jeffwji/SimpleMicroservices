package io.wangji.k8s.configuration

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import zio.{Has, Managed, Task, ZIO, ZLayer}

object AkkaActorSystem extends StrictLogging {
    val live: ZLayer[Any, Throwable, Has[ActorSystem]] = ZLayer.fromManaged{
        Managed.make{
            Task.effect {
                logger.info("ActorSystem acquisition")
                val actorSystem = ActorSystem("my-system")
                actorSystem.registerOnTermination(() =>
                    logger.info("ActorSystem is terminating...")
                )
                actorSystem
            }
        }{
            sys => Task.fromFuture(_ => {
                logger.info("ActorSystem release")
                sys.terminate
            }).either
        }
    }

    object DSL {
        def actorSystem:ZIO[Has[ActorSystem], Throwable, ActorSystem] =
            ZIO.access(s => s.get)
    }
}
