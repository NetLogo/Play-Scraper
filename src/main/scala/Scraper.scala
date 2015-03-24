package org.nlogo

import java.io.File
import java.net.URL
import java.util.{ List => JList }

import sbt.{ AutoPlugin, taskKey, settingKey, Project, Compile, State, Path }, Path._
import sbt.Keys._

import play.Play
import play.PlayReload
import play.runsupport.Reloader.{ CompileSuccess, CompileResult, CompileFailure }
import play.core.classloader.{ ApplicationClassLoaderProvider, DelegatingClassLoader }

import scala.collection.JavaConversions._

import ScrapeTasks._

object Scraper extends AutoPlugin {
  object autoImport {
    val scrapePlay       = taskKey[Unit]("scrape play")
    val scrapeLoader     = taskKey[ClassLoader]("classLoader to use when scraping play")
    val scrapeStaticSite = settingKey[File]("directory to scrape static site into") // TODO: This is a bad name
    val scrapeRoutes     = settingKey[Seq[String]]("routes to be scraped")
  }

  import autoImport._

  val buildLoader = Scraper.getClass.getClassLoader
  val scraperLocation = Scraper.getClass.getProtectionDomain.getCodeSource.getLocation

  private def urls(files: Seq[File]): Seq[URL] = files.map(_.toURI.toURL)

  private def delegateLoader(commonClassLoader: ClassLoader, buildLoader: ClassLoader, reloaderThunk: () => ClassLoader): ClassLoader =
    new DelegatingClassLoader(commonClassLoader, buildLoader, new ApplicationClassLoaderProvider { def get = reloaderThunk() })

  private def compilePlay(currentState: State): CompileResult = {
    import Play._
    Project.runTask(playCompileEverything, currentState)
    PlayReload.compile(
      () => Project.runTask(playReload, currentState).map(_._2).get,
      () => Project.runTask(playReloaderClasspath, currentState).map(_._2).get,
      () => Project.runTask(streamsManager, currentState).map(_._2).get.toEither.right.toOption)
  }

  override val projectSettings = Seq(
    scrapeStaticSite := target.value / "play-scrape",
    scrapeRoutes     := Seq("/"),
    cleanFiles       <+= scrapeStaticSite,
    scrapeLoader     := {
      import Play._
      compilePlay(state.value) match {
        case CompileSuccess(sources, classpath) =>
          // For more information, see
          // https://github.com/playframework/playframework/blob/bc38516056b458bdc41818e7395815366c5e119d/framework/src/run-support/src/main/scala/play/runsupport/Reloader.scala#L127-L167
          val fullClassPath = urls(playDependencyClasspath.value.files) ++ urls(classpath) :+ scraperLocation
          lazy val commonClassLoader = playCommonClassloader.value
          lazy val delegatingLoader = delegateLoader(commonClassLoader, buildLoader,
            () => playReloaderClassLoader.value("reloader", fullClassPath.toArray, appLoader))
          lazy val depLoader = playDependencyClassLoader.value("PlayDependencyClassLoader", fullClassPath.toArray, delegatingLoader)
          lazy val appLoader: ClassLoader = playAssetsClassLoader.value(depLoader)
          appLoader
        case CompileFailure(playException) => throw playException
      }
    },
    (scrapePlay in Compile) := {
      import Play.playAllAssets
      scrapeSpecifiedRoutes(baseDirectory.value, scrapeStaticSite.value, scrapeLoader.value, scrapeRoutes.value)
      scrapeAssets(playAllAssets.value, scrapeStaticSite.value, scrapeLoader.value)
    })
}
