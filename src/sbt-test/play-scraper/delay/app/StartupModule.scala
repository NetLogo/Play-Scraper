import
  javax.inject.{ Inject, Singleton }

import
  play.api.cache.CacheApi

import
  play.api.libs.concurrent.Execution.Implicits._

import
  com.google.inject.AbstractModule

import
  scala.concurrent.Future

@Singleton
class Startup @Inject() (cache: CacheApi) {
  Future {
    Thread.sleep(500)
    cache.set("worker.finished", "finished waiting")
  }
}

class StartupModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Startup]).asEagerSingleton
  }
}
