package org.nlogo

import java.io.File

import sbt.{ AutoPlugin, taskKey, Runtime }
import sbt.Keys._

import play.Play
import play.api.Mode
import play.api.mvc.RequestHeader
import play.api.mvc.EssentialAction
import play.api.libs.iteratee.Iteratee
import play.api.DefaultApplication
import play.core.classloader.{ ApplicationClassLoaderProvider, DelegatingClassLoader }

import scala.util.Success
import scala.util.Random
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

import StartServer.simpleGetRequest

object Scraper extends AutoPlugin {
  object autoImport {
    val scrapePlay = taskKey[Unit]("scrape play")
  }

  import autoImport._

  class ScraperDelegatingClassLoader(
    playLoader: ClassLoader,
    buildLoader: ClassLoader,
    loaderProvider: ApplicationClassLoaderProvider)
  extends DelegatingClassLoader(playLoader, buildLoader, loaderProvider) {
    val scraperClassNames: Seq[String] = Seq(
      "play.api.Application",
      "org.nlogo.StartServer"
    )

    override def loadClass(name: String): Class[_] = {
      println(name)
      if (scraperClassNames.contains(name))
        buildLoader.loadClass(name)
      else
        super.loadClass(name)
    }
  }

  val buildLoader = Scraper.getClass.getClassLoader
  val scraperLocation = Scraper.getClass.getProtectionDomain.getCodeSource.getLocation

  override val projectSettings = Seq(
    scrapePlay := {
      import Play._
      val commonClassLoader = playCommonClassloader.value
      var appLoader: Option[ClassLoader] = None
      val delegatingLoader = new ScraperDelegatingClassLoader(commonClassLoader, buildLoader, new ApplicationClassLoaderProvider {
        def get = {
          playReloaderClassLoader.value("reloader", (playReloaderClasspath.value.files.map(_.toURI.toURL) :+ scraperLocation).toArray, appLoader.get)
        }
      })
    val loader = playDependencyClassLoader.value("PlayDependencyClassLoader", (playDependencyClasspath.value.files.map(_.toURI.toURL) :+ scraperLocation).toArray, delegatingLoader)

    val assetLoader = playAssetsClassLoader.value(loader)
    appLoader = Some(assetLoader)

    val serverStarter = loader.loadClass("org.nlogo.StartServer$")
    println(serverStarter.getFields.foreach((f: java.lang.reflect.Field) => println(f.toString)))
    val ssInstance = serverStarter.getFields.head.get(null)
    val ssApply = serverStarter.getDeclaredMethod("apply", classOf[java.io.File], classOf[ClassLoader])
    ssApply.invoke(ssInstance, baseDirectory.value, assetLoader)
      /*
      val defaultAppConstructor = loader.loadClass("play.api.DefaultApplication").getDeclaredConstructors.head
      val mode = loader.loadClass("play.api.Mode")
      val devMode = mode.getMethod("Dev").invoke(mode)
      val app = defaultAppConstructor.newInstance(baseDirectory.value, assetLoader, null, devMode).asInstanceOf[play.api.Application]
      val rts = app.routes.get
      val indexRequest = simpleGetRequest("/")
      val action = rts.routes(indexRequest).asInstanceOf[EssentialAction]
      val consumer = Iteratee.getChunks[Array[Byte]]
      action(indexRequest).run.onComplete {
        res => println(res)
      }
      */
    })
}
