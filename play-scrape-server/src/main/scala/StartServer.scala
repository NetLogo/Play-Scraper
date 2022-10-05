package org.nlogo

import java.io.File

import java.util.{ List => JList, Map => JMap }

import controllers.{ AssetsConfiguration, DefaultAssetsMetadata }
import play.api.{ Configuration, Environment, Mode, Play, Application }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule
import play.api.http.{ HttpConfiguration, DefaultFileMimeTypes }

import scala.util.{ Random, Success, Failure }
import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.concurrent.{ duration, ExecutionContext, Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object StartServer {
  def apply(
    baseDirectory: File,
    loader: ClassLoader,
    additionalConfig: JMap[String, String],
    scrapeDelay: java.lang.Integer,
    scraper: ApplicationScraper): Unit = {
      val extraConfig = Configuration.from(HashMap(additionalConfig.asScala.toSeq: _*))
      val app = new GuiceApplicationBuilder()
        .load((env, conf) => GuiceableModule.loadModules(env, conf))
        .loadConfig(env => extraConfig.withFallback(Configuration.load(env)))
        .in(baseDirectory)
        .in(loader)
        .in(Mode.Prod)
        .build
      Play.start(app)
      Thread.sleep(Int.unbox(scrapeDelay) * 1000)
      Await.ready(scraper.scrape(app), Duration.Inf)
      ()
    }

  def pathForAsset(baseDirectory: File, loader: ClassLoader, assetName: String): String = {
    // begin by configuring the StaticAssetsMetadata
    // reflective because this is a private[controllers] object / method
    val assetsMetadataClass = Class.forName("controllers.StaticAssetsMetadata$")
    val staticAssetsMetadataSetter = assetsMetadataClass.getDeclaredMethod("instance_$eq", classOf[Option[_]])
    val assetsMetadataObject = assetsMetadataClass.getDeclaredField("MODULE$").get(assetsMetadataClass)
    val env = Environment(baseDirectory, loader, Mode.Prod)
    val config = Configuration.load(env)
    val assetsConfig = AssetsConfiguration.fromConfiguration(config, env.mode)
    val httpConfig = HttpConfiguration.fromConfiguration(config, env)
    val mimeTypes = new DefaultFileMimeTypes(httpConfig.fileMimeTypes)
    val metadata = new DefaultAssetsMetadata(env, assetsConfig, mimeTypes)
    staticAssetsMetadataSetter.invoke(assetsMetadataObject, Some(metadata))

    val assetRouterClass = getClass.getClassLoader.loadClass("controllers.ReverseAssets")
    val assetRouterConstructor =
      assetRouterClass.getConstructor(classOf[scala.Function0[java.lang.String]])
    val assetRouterInstance = assetRouterConstructor.newInstance(() => "")
    try {
      val assetClass = getClass.getClassLoader.loadClass("controllers.Assets$Asset")
      val assetConstructor = assetClass.getConstructor(classOf[java.lang.String])
      val assetInstance = assetConstructor.newInstance(assetName).asInstanceOf[Object]
      val versioned = assetRouterClass.getDeclaredMethod("versioned", assetClass)
      val call = versioned.invoke(assetRouterInstance, assetInstance)
      call.asInstanceOf[play.api.mvc.Call].url
    } catch {
      case ex: NoSuchMethodException =>
        assetRouterClass.getDeclaredMethod("at", classOf[String]).
                         invoke(assetRouterInstance, assetName).
                         asInstanceOf[play.api.mvc.Call].url
    }
  }
}
