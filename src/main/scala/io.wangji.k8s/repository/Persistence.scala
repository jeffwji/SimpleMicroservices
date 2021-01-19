package io.wangji.k8s.repository

import doobie.hikari.HikariTransactor
import io.wangji.k8s.configuration.Configuration.DSL._

import scala.concurrent.ExecutionContext
import cats.effect.{Blocker, Resource}
import io.wangji.k8s.configuration.HasConfiguration
import zio.{Managed, Task, _}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import doobie.Transactor
import doobie.util.ExecutionContexts
import zio.blocking.Blocking
import zio.interop.catz._

object Persistence extends StrictLogging{
    private def getTransactor(
                               conf: Config,
                               connectEC: ExecutionContext,
                               blockerEC: ExecutionContext
                             ): Managed[Throwable, Transactor[Task]] = {
        val resource: Resource[Task, Transactor[Task]] = HikariTransactor.newHikariTransactor[Task](
            conf.getString("driver"),
            conf.getString("url"),
            conf.getString("user"),
            conf.getString("password"),
            connectEC,                                // await connection here
            Blocker.liftExecutionContext(blockerEC)   // execute JDBC operations here
        )

        resource.toManagedZIO
    }

    def live(implicit connectEC: ExecutionContext): ZLayer[HasConfiguration with Blocking, Throwable, HasTransactor] =
        ZLayer.fromManaged{
            for {
                blockerEC <- ExecutionContexts.cachedThreadPool[Task].toManagedZIO

                tnx <- load.toManaged_
                  .flatMap((conf: Config) =>
                      getTransactor(conf.getConfig("db.source"), connectEC, blockerEC)
                  )
            } yield tnx
        }

    object DSL {
        def transactor: ZIO[HasTransactor, Throwable, Transactor[Task]] = ZIO.access(_.get)
    }
}

