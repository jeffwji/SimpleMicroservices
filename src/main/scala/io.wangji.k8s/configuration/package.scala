package io.wangji.k8s

import akka.actor.ActorSystem
import zio.Has

package object configuration {
    type HasConfiguration = Has[Configuration.Service]
    type HasActorSystem = Has[ActorSystem]
}
