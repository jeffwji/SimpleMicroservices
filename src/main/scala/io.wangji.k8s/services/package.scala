package io.wangji.k8s

import zio.{Has, Task}

package object services {
    type HasStorageService = Has[StorageService.Service[Task]]
}
