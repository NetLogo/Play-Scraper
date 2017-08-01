package org.nlogo

import java.io.File
import java.util.{ List => JList, Map => JMap }
import java.lang.reflect.Method

import scala.collection.JavaConversions._

import scala.language.postfixOps

import sbt.{ Path, IO },
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

  // path desired: /assets/javascripts/....js
  // path in jar: /public/javascripts/....js
  // playAllAssets: (public/, file::path/to/public/main)
  def scrapeAssets(playAssets: Seq[(String, File)], jarFile: File, applicationDirectory: File, targetDirectory: File, loader: ClassLoader, customSettings: Map[String, String]) = {
    val (ssPathForAsset, ssInstance) = startServerMethod(loader, "pathForAsset", classOf[File], classOf[ClassLoader], classOf[String])
    val lookupAssetPath = ((s: String) => ssPathForAsset.invoke(ssInstance, applicationDirectory, loader, s).asInstanceOf[String])
    val tempDirectory = IO.createTemporaryDirectory
    IO.unzip(jarFile, tempDirectory)
    val displayPath = playAssets.head._1 // this will typically be /public. Not sure how to deal with having more than one path
    val allAssets = (tempDirectory / displayPath ***).get.map(assetFile => (tempDirectory / displayPath, assetFile))
    val filesToCopy =
      allAssets.flatMap {
        case (assetsDir, asset) =>
          relativeTo(assetsDir)(asset).map(assetPath =>
              (asset, targetDirectory / customSettings.getOrElse("play.http.context", "") / lookupAssetPath(assetPath)))
      }
    copyFiles(filesToCopy, overwrite=true)
  }
}
