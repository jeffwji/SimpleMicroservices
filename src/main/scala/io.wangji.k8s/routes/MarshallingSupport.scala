package io.wangji.k8s.routes

import java.util.NoSuchElementException

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.{Route, RouteResult, StandardRoute}
import zio.internal.Platform
import zio.{IO, Runtime}

import scala.concurrent.{Future, Promise}

trait MarshallingSupport extends Runtime[Unit] { self =>
    override val environment: Unit  =  Runtime.default.environment // environment type Unit,
    override val platform: Platform = zio.internal.Platform.default

    sealed trait ErrorToHttpResponse[E] {
        def toHttpResponse(value: E): HttpResponse
    }

    private def generateHttpResponseFromError(error: Throwable) : HttpResponse = {
        error match {
            case _: NoSuchElementException =>
                HttpResponse(StatusCodes.NotFound, entity = HttpEntity("Record was not found"))
            case e@_ =>
                HttpResponse(StatusCodes.NotFound, entity = HttpEntity(e.getMessage))
        }
    }

    implicit def errorHttp: ErrorToHttpResponse[Throwable] = new ErrorToHttpResponse[Throwable] {
        override def toHttpResponse(value: Throwable): HttpResponse = {
            generateHttpResponseFromError(value)
        }
    }

    implicit val errorMarshaller: Marshaller[Throwable, HttpResponse] = {
        Marshaller { implicit ec => error   =>
            val response = generateHttpResponseFromError(error)
            PredefinedToResponseMarshallers.fromResponse(response)
        }
    }

    implicit def ioEffectToMarshallable[E, A](implicit m1: Marshaller[A, HttpResponse], m2: Marshaller[E, HttpResponse]): Marshaller[IO[E, A], HttpResponse] = {
        //Factory method for creating marshallers
        Marshaller { implicit ec =>
            effect =>
                val promise = Promise[List[Marshalling[HttpResponse]]]()
                val marshalledEffect: IO[Throwable, List[Marshalling[HttpResponse]]] = effect.foldM(
                    err => IO.fromFuture(_ => m2(err)),
                    suc => IO.fromFuture(_ => m1(suc))
                )
                self.unsafeRunAsync(marshalledEffect) { done =>
                    done.fold(
                        failed => promise.failure(failed.squash),
                        success => promise.success(success)
                    )
                }
                promise.future
        }
    }

    implicit def standardRouteToRoute[E](effect: IO[E, StandardRoute])(implicit errToHttp: ErrorToHttpResponse[E]): Route = {
        //type Route = RequestContext ⇒ Future[RouteResult]
        ctx =>
            val promise = Promise[RouteResult]()
            val foldedEffect = effect.fold(
                err => { Future.successful(Complete(errToHttp.toHttpResponse(err))) },
                suc => suc.apply(ctx)
            )

            self.unsafeRunAsync(foldedEffect) { done =>
                done.fold(
                    err => promise.failure(err.squash),
                    suc => promise.completeWith(suc)
                )
            }
            promise.future
    }
}
