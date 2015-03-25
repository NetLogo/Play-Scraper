package org.nlogo

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.{ List => JList, Map => JMap }

import play.core.StaticApplication
import play.api.{ Mode, Play, Configuration, Application }
import play.api.mvc.{ RequestHeader, EssentialAction }
import play.api.libs.iteratee.Iteratee
import play.api.DefaultApplication
import play.core.classloader.{ ApplicationClassLoaderProvider, DelegatingClassLoader }

import scala.util.{ Random, Success, Failure }
import scala.collection.JavaConversions._
import scala.collection.immutable.HashMap
import scala.concurrent.{ duration, ExecutionContext, Await, Future },
  ExecutionContext.Implicits.global,
  duration.Duration

object StartServer {
  def simpleGetRequest(_path: String, _accept: String = "*/*"): RequestHeader = {
    new RequestHeader {
      def headers = new play.api.mvc.Headers {
        import play.api.http.HeaderNames._
        val data = Seq[(String, Seq[String])](ACCEPT -> Seq(_accept))
      }
      def id = Random.nextInt
      def method = "GET"
      def path = _path
      def queryString = Map[String, Seq[String]]()
      def remoteAddress = "0.0.0.0"
      def secure = false
      def tags = Map[String, String]()
      def uri = _path
      def version = "HTTP/1.1"
    }
  }

  def apply(baseDirectory: File, targetDirectory: File, loader: ClassLoader, routesToScrape: JList[String], additionalConfig: JMap[String, String]): Unit = {
    val app = new DefaultApplication(baseDirectory, loader, None, Mode.Prod) {
      override def configuration = super.configuration ++ Configuration.from(HashMap(additionalConfig.toSeq: _*))
    }
    Play.start(app)
    Await.ready(
      Future.traverse(routesToScrape.toSeq.map(contextualizePath(app)))(
        path => renderPage(app, path).map(body => writeToFile(pathToFile(targetDirectory.getPath, path), body))),
      Duration.Inf)
  }

  private def contextualizePath(app: Application)(requestedPath: String) =
    app.configuration.getString("application.context")
      .map(context => s"$context$requestedPath".replaceAll("//", "/"))
      .getOrElse(requestedPath)

  private def renderPage(app: Application, path: String): Future[String] = {
    val req = simpleGetRequest(path)
    val action = app.routes.get.routes(req).asInstanceOf[EssentialAction]
    val fileWritingIteratee = Iteratee
      .fold[Array[Byte], Array[Byte]](Array[Byte]())(_ ++ _)
      .map(new String(_, "UTF-8"))
    action(req).flatMapM(result => result.body(fileWritingIteratee)).run
  }

  private def pathToFile(parentDirPath: String, path: String): File = {
    def toFile(parentDirPath: String, path: String): File =
      path.span(_ != '/') match {
        case (filename, "") =>
          val file = new File(parentDirPath, filename)
          if (file.isDirectory) new File(file.getPath, "index.html") else file
        case (filename, rest) =>
          val dir = new File(parentDirPath + File.separatorChar + filename)
          if (! dir.isDirectory) dir.mkdir
          toFile(dir.getPath, rest.drop(1))
      }
    toFile(parentDirPath, path.drop(1))
  }

  private def writeToFile(file: File, text: String): Unit = {
    val fileOutputStream = new FileOutputStream(file)
    val writer = new OutputStreamWriter(fileOutputStream, "UTF-8")
    writer.write(text, 0, text.length)
    writer.flush()
    writer.close()
    fileOutputStream.close()
  }

  def pathForAsset(assetName: String): String = {
    val assetRouterClass = getClass.getClassLoader.loadClass("controllers.ReverseAssets")
    val assetRouterInstance = getClass.getClassLoader.loadClass("controllers.routes").getField("Assets").get(null)
    assetRouterClass.getDeclaredMethod("at", classOf[String]).invoke(assetRouterInstance, assetName).asInstanceOf[play.api.mvc.Call].url
  }
}
