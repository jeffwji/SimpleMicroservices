package io.wangji.k8s

import zio.{Has, Task}

package object routes {
    type HasStorageController = Has[StorageController.Service[Task]]
}

case class ErrorMessage(message:String)