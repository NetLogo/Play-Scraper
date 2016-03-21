package org.nlogo

import
  com.amazonaws.auth.{ AWSCredentialsProvider, EnvironmentVariableCredentialsProvider, profile },
    profile.ProfileCredentialsProvider

import java.io.File
import java.net.URL
import java.util.{ List => JList }

import sbt.{ AutoPlugin, Def, IO, taskKey, settingKey, Project, Compile, State, Path }, Path._
import sbt.Keys._

import play.core.Build
import play.sbt.Play
import play.sbt.run.PlayReload
import play.sbt.PlayInternalKeys.{ playAllAssets, playAssetsClassLoader, playCommonClassloader, playCompileEverything,
  playDependencyClassLoader, playDependencyClasspath, playReload, playReloaderClassLoader, playReloaderClasspath }
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
    val scrapePublishDistributionID = settingKey[Option[String]]("scrape publication distribution ID")
  }

  import autoImport._

  override def requires = Play && sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  val buildLoader = PlayScrapePlugin.getClass.getClassLoader

  private def urls(files: Seq[File]): Seq[URL] = files.map(_.toURI.toURL)

  private def delegateLoader(commonClassLoader: ClassLoader, buildLoader: ClassLoader, reloaderThunk: () => ClassLoader): ClassLoader =
    new DelegatingClassLoader(commonClassLoader, Build.sharedClasses, buildLoader, new ApplicationClassLoaderProvider { def get = reloaderThunk() })

  private def compilePlay(currentState: State): CompileResult = {
    import Play._
    Project.runTask(playCompileEverything, currentState)
    PlayReload.compile(
      () => Project.runTask(playReload, currentState).map(_._2).get,
      () => Project.runTask(playReloaderClasspath, currentState).map(_._2).get,
      () => Project.runTask(streamsManager, currentState).map(_._2).get.toEither.right.toOption)
  }

  object credentials {
    def fromEnvironmentVariables =
      new EnvironmentVariableCredentialsProvider()

    def fromCredentialsProfile(name: String) =
      new ProfileCredentialsProvider(name)
  }

  import sbt._

  override val projectSettings = Seq(
    resolvers += Resolver.bintrayRepo("content/netlogo", "play-scraper"),
    allDependencies += "org.nlogo" %% "play-scrape-server" % {
      val plugins = buildStructure.value.units(thisProjectRef.value.build).unit.plugins.pluginData.classpath
      val module = plugins
        .flatMap(_.get(AttributeKey[sbt.ModuleID]("moduleId")))
        .filter(m => m.organization == "org.nlogo" && m.name == "play-scraper")
      module.map(_.revision).head
    },
    scrapeTarget        := target.value / "play-scrape",
    scrapeUpload        <<= Def.taskDyn {
      (for {
        bucketID <- scrapePublishBucketID.value
        distributionID = scrapePublishDistributionID.value
      } yield (Def.task {
          StaticSiteUploader.deploy(
            scrapePublishCredential.value,
            scrapeTarget.value,
            bucketID,
            distributionID,
            scrapeAbsoluteURL.value
          )
        })).getOrElse(Def.task {
          streams.value.log.warn("*** Warning: NO UPLOAD PERFORMED ***")
          streams.value.log.warn("Skipping upload since scrapePublishBucketID not set")
        })
    },
    scrapeContext       := "",
    scrapeAbsoluteURL   := None,
    scrapeDelay         := 0,
    scrapeRoutes        := Seq("/"),
    cleanFiles          <+= scrapeTarget,
    scrapePublishCredential     := credentials.fromEnvironmentVariables,
    scrapePublishBucketID       := None,
    scrapePublishDistributionID := None,
    scrapeLoader        := {
      import Play._
      compilePlay(state.value) match {
        case CompileSuccess(sources, classpath) =>
          // For more information, see
          // https://github.com/playframework/playframework/blob/bc38516056b458bdc41818e7395815366c5e119d/framework/src/run-support/src/main/scala/play/runsupport/Reloader.scala#L127-L167
          val scraperLocation = (fullClasspath in Compile).value
            .filter(f =>
              f.get(AttributeKey[ModuleID]("moduleId"))
                .exists(m => m.organization == "org.nlogo" && m.name.startsWith("play-scrape-server")))
            .map(_.data.toURI.toURL).head
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
      val customSettings: Map[String, String] = if (scrapeContext.value == "") Map() else Map("application.context" -> scrapeContext.value)
      IO.delete(scrapeTarget.value)
      IO.createDirectory(scrapeTarget.value)
      scrapeAssets(playAllAssets.value, scrapeTarget.value, scrapeLoader.value, customSettings)
      scrapeSpecifiedRoutes(baseDirectory.value, scrapeTarget.value, scrapeLoader.value, scrapeRoutes.value, scrapeDelay.value, customSettings, scrapeAbsoluteURL.value)
    })
}
