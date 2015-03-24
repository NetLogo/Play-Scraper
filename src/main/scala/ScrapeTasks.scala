package org.nlogo

import java.io.File
import java.util.{ List => JList }
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

  def scrapeSpecifiedRoutes(applicationDirectory: File, targetDirectory: File, loader: ClassLoader, routesToScrape: Seq[String]) = {
    import scala.collection.JavaConverters.seqAsJavaListConverter
    val (ssApply, ssInstance) = startServerMethod(loader, "apply",
      classOf[java.io.File], classOf[java.io.File], classOf[ClassLoader], classOf[JList[String]])
    ssApply.invoke(ssInstance, applicationDirectory, targetDirectory, loader, routesToScrape.asJava)
  }

  def scrapeAssets(playAssets: Seq[(String, File)], targetDirectory: File, loader: ClassLoader) = {
    val (ssPathForAsset, ssInstance) = startServerMethod(loader, "pathForAsset", classOf[String])
    val lookupAssetPath = ((s: String) => ssPathForAsset.invoke(ssInstance, s).asInstanceOf[String])
    copyFiles(
      playAssets flatMap {
        case (displayPath, assetsDir) =>
          (assetsDir ***).get.flatMap(f =>
              relativeTo(assetsDir)(f).map(assetPath =>
                  (f, targetDirectory / lookupAssetPath(assetPath))))
      })
  }
}
