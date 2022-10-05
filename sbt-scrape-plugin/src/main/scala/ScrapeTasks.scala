package org.nlogo

import java.io.File
import java.util.{ List => JList, Map => JMap }
import java.lang.reflect.Method

import scala.collection.JavaConversions._

import scala.language.postfixOps
import scala.util.Try

import sbt.{ fileToRichFile, io, Path },
  io.{ CopyOptions, IO, Path => IOPath, PathFinder },
    IO.{ copy => copyFiles },
  Path._

object ScrapeTasks {
  def startServerMethod(loader: ClassLoader, methodName: String, paramClasses: Class[_]*): (Method, AnyRef) = {
    val serverStarter = loader.loadClass("org.nlogo.StartServer$")
    val ssInstance = serverStarter.getField("MODULE$").get(null)
    (serverStarter.getDeclaredMethod(methodName, paramClasses: _*), ssInstance)
  }

  def buildApplicationScraper[T](appScraperClass: Class[_ <: T], routesToScrape: Seq[String], targetDirectory: File, absoluteURL: Option[String]): T = {
    import scala.collection.JavaConverters.seqAsJavaListConverter

    appScraperClass.getDeclaredConstructor(classOf[JList[String]], classOf[File], classOf[String])
      .newInstance(routesToScrape.asJava, targetDirectory, absoluteURL.orNull)
      .asInstanceOf[T]
  }

  def scrapeSpecifiedRoutes(applicationDirectory: File, targetDirectory: File, loader: ClassLoader, routesToScrape: Seq[String], scrapeDelay: Int, config: Map[String, String], absoluteURL: Option[String]) = {
    import scala.collection.JavaConverters.mapAsJavaMapConverter

    val applicationScraperClass = loader.loadClass("org.nlogo.ApplicationScraper")
    val (ssApply, ssInstance) = startServerMethod(
      loader,
      "apply",
      classOf[File],
      classOf[ClassLoader],
      classOf[JMap[String, String]],
      classOf[java.lang.Integer],
      applicationScraperClass)

    val applicationScraper = buildApplicationScraper(applicationScraperClass, routesToScrape, targetDirectory, absoluteURL)

    ssApply.invoke(
      ssInstance,
      applicationDirectory,
      loader,
      config.asJava,
      Int.box(scrapeDelay),
      applicationScraper.asInstanceOf[java.lang.Object])
  }

  def listAssetsToScrape(playAssets: Seq[(String, File)], jarFile: File): Seq[(File, String)] = {
    val tempDirectory = IO.createTemporaryDirectory
    IO.unzip(jarFile, tempDirectory)
    // playAssets.head._1 will typically be "public/".
    // We strip off the tailing slash, because that screws up PathFinder.
    // Currently, we don't deal with having more than one path in playAssets.
    // -- RG 3/12/18
    val displayPath = playAssets.head._1.dropRight(1)
    val assetsDir = tempDirectory / displayPath
    val assetFiles = ((PathFinder(tempDirectory) / displayPath).allPaths).get
    assetFiles.map( (assetFile) => {
      val relativePath = relativeTo(assetsDir)(assetFile).get
      (assetFile, relativePath)
    })
  }

  // path desired: /assets/javascripts/....js
  // path in jar: /public/javascripts/....js
  // playAllAssets: (public/, file::path/to/public/main)
  def scrapeAssets(assetsToScrape: Seq[(File, String)], applicationDirectory: File, targetDirectory: File, loader: ClassLoader, customSettings: Map[String, String]) = {
    val (ssPathForAsset, ssInstance) = startServerMethod(loader, "pathForAsset", classOf[File], classOf[ClassLoader], classOf[String])
    def lookupAssetPath(s: String): Option[String] = {
      Try(ssPathForAsset.invoke(ssInstance, applicationDirectory, loader, s).asInstanceOf[String]).toOption
    }
    val filesToCopy =
      for {
        (asset, relativePath) <- assetsToScrape
        assetPath <- lookupAssetPath(relativePath)
        context = customSettings.getOrElse("play.http.context", "")
      } yield (asset, targetDirectory / context / assetPath)
    copyFiles(filesToCopy, CopyOptions().withOverwrite(true))
  }
}
