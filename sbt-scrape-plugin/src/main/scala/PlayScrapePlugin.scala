package org.nlogo

import com.amazonaws.auth.{ AWSCredentialsProvider, EnvironmentVariableCredentialsProvider }
import com.amazonaws.auth.profile.ProfileCredentialsProvider

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.stage

import java.io.File
import java.net.{ URL, URLClassLoader }
import java.util.{ List => JList }

import sbt.{
  AttributeKey
, AutoPlugin
, Compile
, Def
, fileToRichFile
, Keys
, Project
, Path
, Resolver
, Scope
, settingKey
, State
, stringToOrganization
, taskKey
}
import sbt.Path._
import sbt.Keys._
import sbt.io.{ FileFilter, IO, PathFinder, RichFile, syntax }
import sbt.io.FileFilter.globFilter

import play.core.Build
import play.sbt.{ Play, PlayWeb }
import play.sbt.run.PlayReload
import play.sbt.PlayImport.PlayKeys.playPackageAssets
import play.sbt.PlayInternalKeys.{
  playAllAssets
, playAssetsClassLoader
, playCommonClassloader
, playCompileEverything
, playDependencyClassLoader
, playDependencyClasspath
, playReload
, playReloaderClassLoader
, playReloaderClasspath
}
import play.runsupport.NamedURLClassLoader
import play.runsupport.Reloader.{ CompileSuccess, CompileResult, CompileFailure }
import play.runsupport.classloader.{ ApplicationClassLoaderProvider, DelegatingClassLoader }

import scala.collection.JavaConversions._

import ScrapeTasks._

object PlayScrapePlugin extends AutoPlugin {
  object autoImport {
    val scrapePlay                  = taskKey[Unit]("scrape play")
    val scrapeUpload                = taskKey[Unit]("upload scraped site")
    val scrapeDelay                 = taskKey[Int]("number of seconds to delay after startup before scraping")
    val scrapeLoader                = taskKey[ClassLoader]("classLoader to use when scraping play")
    val scrapeContext               = settingKey[String]("subdirectory in which to put generated files")
    val scrapeTarget                = settingKey[File]("directory to scrape static site into")
    val scrapeRoutes                = settingKey[Seq[String]]("routes to be scraped")
    val scrapeAbsoluteURL           = settingKey[Option[String]]("absolute URL to scrape against")
    val scrapePublishCredential     = settingKey[AWSCredentialsProvider]("scrape publication credential")
    val scrapePublishBucketID       = settingKey[Option[String]]("scrape publication bucket ID")
    val scrapePublishRegion         = settingKey[Option[String]]("scrape publication region name")
    val scrapePublishDistributionID = settingKey[Option[String]]("scrape publication distribution ID")
  }

  import autoImport._

  override def requires = PlayWeb && sbt.plugins.JvmPlugin
  override def trigger  = allRequirements

  val buildLoader = PlayScrapePlugin.getClass.getClassLoader

  private def urls(files: Seq[File]): Seq[URL] =
    files.map(_.toURI.toURL)

  object credentials {
    def fromEnvironmentVariables =
      new EnvironmentVariableCredentialsProvider()

    def fromCredentialsProfile(name: String) =
      new ProfileCredentialsProvider(name)
  }

  override val projectSettings = Seq(
    resolvers += Resolver.bintrayRepo("content/netlogo", "play-scraper"),
    allDependencies += "org.nlogo" %% "play-scrape-server" % {
      val plugins = buildStructure.value.units(thisProjectRef.value.build).unit.plugins.pluginData.classpath
      val module  = plugins
        .flatMap(_.get(AttributeKey[sbt.ModuleID]("moduleID")))
        .filter( (m) => m.organization == "org.nlogo" && m.name == "play-scraper" )
      module.map(_.revision).head
    },
    scrapeTarget := (target.value: RichFile) / "play-scrape",
    scrapeUpload := {
      val bucketID   = scrapePublishBucketID.value
      val streamsLog = streams.value.log
      if (bucketID.isEmpty) {
        streamsLog.warn("*** Warning: NO UPLOAD PERFORMED ***")
        streamsLog.warn("Skipping upload since scrapePublishBucketID not set")
      } else {
        val bucket         = bucketID.get
        val distributionID = scrapePublishDistributionID.value
        StaticSiteUploader.deploy(
          scrapePublishCredential.value,
          scrapePublishRegion.value.getOrElse("us-east-1"),
          scrapeTarget.value,
          bucket,
          distributionID,
          scrapeAbsoluteURL.value
        )
      }
    },
    scrapeContext     := "",
    scrapeAbsoluteURL := None,
    scrapeDelay       := 0,
    scrapeRoutes      := Seq("/"),
    cleanFiles        += scrapeTarget.value,
    scrapePublishCredential     := credentials.fromEnvironmentVariables,
    scrapePublishBucketID       := None,
    scrapePublishRegion         := None,
    scrapePublishDistributionID := None,
    scrapeLoader := {
      val state = Keys.state.value
      val scope = resolvedScoped.value.scope

      Project.runTask(playCompileEverything, state)
      val compileResult = PlayReload.compile(
        () => Project.runTask(scope / playReload, state).map(_._2).get,
        () => Project.runTask(scope / playReloaderClasspath, state).map(_._2).get,
        () => Project.runTask(scope / streamsManager, state).map(_._2).get.toEither.right.toOption,
        state,
        scope
      )
      compileResult match {
        case CompileSuccess(sources, classpath) =>
          val confDirectory      = (Compile / resourceDirectory).value.toURI.toURL
          val fullStageDirectory = stage.value
          val allJars            = urls((PathFinder(fullStageDirectory) / "lib" ** "*.jar").get)
          new NamedURLClassLoader("playDependencyClassloader", (allJars :+ confDirectory).toArray, playCommonClassloader.value)

        case CompileFailure(playException) =>
          throw playException
      }
    },
    (Compile / scrapePlay) := {
      val customSettings: Map[String, String] = if (scrapeContext.value == "") Map() else Map("play.http.context" -> scrapeContext.value)
      IO.delete(scrapeTarget.value)
      IO.createDirectory(scrapeTarget.value)
      val assetsToScrape = listAssetsToScrape(playAllAssets.value, playPackageAssets.value)
      scrapeAssets(assetsToScrape, baseDirectory.value, scrapeTarget.value, scrapeLoader.value, customSettings)
      scrapeSpecifiedRoutes(baseDirectory.value, scrapeTarget.value, scrapeLoader.value, scrapeRoutes.value, scrapeDelay.value, customSettings, scrapeAbsoluteURL.value)
      ()
    }
  )
}
