package org.nlogo

import java.io.File

import java.util.{ List => JList, Map => JMap }

import play.api.{ Mode, Play, Configuration, Application }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule

import scala.util.{ Random, Success, Failure }
import scala.collection.JavaConversions._
import scala.collection.immutable.HashMap
import scala.concurrent.{ duration, ExecutionContext, Await, Future },
  ExecutionContext.Implicits.global,
  duration.Duration

object StartServer {
  def apply(
    baseDirectory: File,
    loader: ClassLoader,
    additionalConfig: JMap[String, String],
    scrapeDelay: java.lang.Integer,
    scraper: ApplicationScraper): Unit = {
    val extraConfig = Configuration.from(HashMap(additionalConfig.toSeq: _*))
    val app = new GuiceApplicationBuilder()
      .load((env, conf) => GuiceableModule.loadModules(env, conf))
      .loadConfig(env => Configuration.load(env) ++ extraConfig)
      .in(baseDirectory)
      .in(loader)
      .in(Mode.Prod)
      .build
    Play.start(app)
    Thread.sleep(Int.unbox(scrapeDelay) * 1000)
    Await.ready(scraper.scrape(app), Duration.Inf)
  }

  def pathForAsset(assetName: String): String = {
    val assetRouterClass = getClass.getClassLoader.loadClass("controllers.ReverseAssets")
    val assetRouterInstance = getClass.getClassLoader.loadClass("controllers.routes").getField("Assets").get(null)
    try
      assetRouterClass.getDeclaredMethod("versioned", classOf[controllers.Assets.Asset]).
                       invoke(assetRouterInstance, new controllers.Assets.Asset(assetName)).
                       asInstanceOf[play.api.mvc.Call].url
    catch {
      case ex: NoSuchMethodException =>
        assetRouterClass.getDeclaredMethod("at", classOf[String]).
                         invoke(assetRouterInstance, assetName).
                         asInstanceOf[play.api.mvc.Call].url
    }
  }
}
