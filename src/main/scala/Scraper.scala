package org.nlogo

import java.io.File

import sbt.{ AutoPlugin, taskKey, settingKey, Project, Compile, Path, PathExtra }, Path._
import sbt.Keys._

import play.Play
import play.PlayReload
import play.runsupport.Reloader.CompileSuccess
import play.core.classloader.{ ApplicationClassLoaderProvider, DelegatingClassLoader }

import scala.util.Success
import scala.collection.JavaConversions._

import scala.language.postfixOps

import StartServer.simpleGetRequest

object Scraper extends AutoPlugin {
  object autoImport {
    val scrapePlay       = taskKey[Unit]("scrape play")
    val scrapeStaticSite = settingKey[File]("directory to scrape static site into")
  }

  import autoImport._

  val buildLoader = Scraper.getClass.getClassLoader
  val scraperLocation = Scraper.getClass.getProtectionDomain.getCodeSource.getLocation

  override val projectSettings = Seq(
    scrapeStaticSite := target.value / "play-scrape",
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
          // copy assets
          sbt.IO.copy(
            playAllAssets.value flatMap {
              case (displayPath, assetsDir) =>
                (assetsDir ***).get.flatMap(f =>
                  f pair rebase(assetsDir, scrapeStaticSite.value / displayPath.dropRight(1)))
            })
          // setup for StartServer
          val fullClassPath = playDependencyClasspath.value.files.map(_.toURI.toURL) ++ classpath.map(_.toURI.toURL) :+ scraperLocation
          val commonClassLoader = playCommonClassloader.value
          var appLoader: Option[ClassLoader] = None
          val delegatingLoader = new DelegatingClassLoader(commonClassLoader, buildLoader, new ApplicationClassLoaderProvider {
            def get = {
              playReloaderClassLoader.value("reloader", fullClassPath.toArray, appLoader.get)
            }
          })
        println(fullClassPath.map(_.toString).sorted.mkString("\n"))
        val loader = playDependencyClassLoader.value("PlayDependencyClassLoader", fullClassPath.toArray, delegatingLoader)

      println(playAllAssets.value)
      val assetLoader = playAssetsClassLoader.value(loader)
      appLoader = Some(assetLoader)

      // start scraping!
      val serverStarter = assetLoader.loadClass("org.nlogo.StartServer$")
      val ssInstance = serverStarter.getFields.head.get(null)
      val ssApply = serverStarter.getDeclaredMethod("apply", classOf[java.io.File], classOf[ClassLoader], classOf[java.io.File])
      ssApply.invoke(ssInstance, baseDirectory.value, assetLoader, scrapeStaticSite.value)

      case other =>
        println("ReloadCompile FAILED!")
        println(other)
    }
    })
}
