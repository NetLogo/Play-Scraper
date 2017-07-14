import
  javax.inject.{ Inject, Singleton }

import
  play.api.cache.SyncCacheApi

import scala.concurrent.ExecutionContext

import
  com.google.inject.AbstractModule

import
  scala.concurrent.Future

@Singleton
class Startup @Inject() (cache: SyncCacheApi)(implicit ec: ExecutionContext) {
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
