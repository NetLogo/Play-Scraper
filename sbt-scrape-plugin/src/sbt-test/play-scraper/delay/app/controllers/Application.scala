package controllers

import
  javax.inject.Inject

import
  play.api._,
    cache.SyncCacheApi,
    mvc._

class Application @Inject() (cache: SyncCacheApi) extends InjectedController {

  def index = Action {
    Thread.sleep(1000)
    Ok(views.html.index(cache.get("worker.finished").getOrElse("startup worker not finished!")))
  }

}
