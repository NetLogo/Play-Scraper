package controllers

import
  javax.inject.Inject

import
  play.api.{ cache, mvc },
    cache.SyncCacheApi,
    mvc.{ Action, BaseController, ControllerComponents }

import scala.reflect.ClassTag

class Application @Inject() (cache: SyncCacheApi, val controllerComponents: ControllerComponents) extends BaseController {

  def index = Action({
    Thread.sleep(1000)
    val g = cache.get[String]("worker.finished")
    val m = g.getOrElse("startup worker not finished!")
    Ok(views.html.index(m))
  })

}
