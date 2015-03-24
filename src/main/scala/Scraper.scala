package org.nlogo

import java.io.File
import java.net.URL
import java.util.{ List => JList }

import sbt.{ AutoPlugin, taskKey, settingKey, Project, Compile, Path, PathExtra }, Path._
import sbt.Keys._

import play.Play
import play.PlayReload
import play.runsupport.Reloader.CompileSuccess
import play.core.classloader.{ ApplicationClassLoaderProvider, DelegatingClassLoader }

import scala.util.Success
import scala.collection.JavaConversions._
import scala.collection.JavaConverters.seqAsJavaListConverter

import scala.language.postfixOps

import StartServer.simpleGetRequest

object Scraper extends AutoPlugin {
  object autoImport {
    val scrapePlay       = taskKey[Unit]("scrape play")
    val scrapeStaticSite = settingKey[File]("directory to scrape static site into")
    val scrapeRoutes     = settingKey[Seq[String]]("routes to be scraped")
  }

  import autoImport._

  val buildLoader = Scraper.getClass.getClassLoader
  val scraperLocation = Scraper.getClass.getProtectionDomain.getCodeSource.getLocation

  private def urls(files: Seq[File]): Seq[URL] = files.map(_.toURI.toURL)

  override val projectSettings = Seq(
    scrapeStaticSite := target.value / "play-scrape",
    scrapeRoutes     := Seq("/"),
    cleanFiles <+= scrapeStaticSite,
    (scrapePlay in Compile) := {
      import Play._
      val ignore = playCompileEverything.value

      // compile application
      // TODO: match on the result of this and only scrape if compilation succeeds
      PlayReload.compile(
        () => Project.runTask(playReload, state.value).map(_._2).get,
        () => Project.runTask(playReloaderClasspath, state.value).map(_._2).get,
        () => Project.runTask(streamsManager, state.value).map(_._2).get.toEither.right.toOption
      ) match {
        case CompileSuccess(sources, classpath) =>
          // setup for StartServer
          val fullClassPath = urls(playDependencyClasspath.value.files) ++ urls(classpath) :+ scraperLocation
          val commonClassLoader = playCommonClassloader.value
          var appLoader: Option[ClassLoader] = None
          val delegatingLoader = new DelegatingClassLoader(commonClassLoader, buildLoader, new ApplicationClassLoaderProvider {
            def get = {
              playReloaderClassLoader.value("reloader", fullClassPath.toArray, appLoader.get)
            }
          })
        val loader = playDependencyClassLoader.value("PlayDependencyClassLoader", fullClassPath.toArray, delegatingLoader)

        val assetLoader = playAssetsClassLoader.value(loader)
        appLoader = Some(assetLoader)

        // start scraping!
        val serverStarter = assetLoader.loadClass("org.nlogo.StartServer$")
        val ssInstance = serverStarter.getFields.head.get(null)
        val ssApply = serverStarter.getDeclaredMethod("apply", classOf[java.io.File], classOf[ClassLoader], classOf[java.io.File], classOf[JList[String]])
        ssApply.invoke(ssInstance, baseDirectory.value, assetLoader, scrapeStaticSite.value, scrapeRoutes.value.asJava)

        // copy assets
        val lookupAssetPath = ((s: String) => serverStarter.getDeclaredMethod("pathForAsset", classOf[String]).invoke(ssInstance, s).asInstanceOf[String])
        sbt.IO.copy(
          playAllAssets.value flatMap {
            case (displayPath, assetsDir) =>
              (assetsDir ***).get.flatMap(f =>
                relativeTo(assetsDir)(f).map(assetPath =>
                  (f, scrapeStaticSite.value / lookupAssetPath(assetPath))))
          })
        case other =>
          println("ReloadCompile FAILED!")
          println(other)
      }
    })
}
