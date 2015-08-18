package org.nlogo

import java.io.{ File, FileOutputStream, OutputStream }

import java.util.{ List => JList }

import play.api.Application
import play.api.http.HeaderNames.{ HOST, ACCEPT }
import play.api.mvc.{ RequestHeader, EssentialAction }
import play.api.libs.iteratee.Iteratee
import play.api.DefaultApplication

import scala.util.Random
import scala.collection.JavaConversions._

import scala.concurrent.{ ExecutionContext, Future },
  ExecutionContext.Implicits.global

class ApplicationScraper(routesToScrape: Seq[String], targetDirectory: File, absoluteHost: Option[String]) {
  def this(routesToScrape: JList[String], targetDirectory: File, absoluteHost: String) =
    this(routesToScrape: Seq[String], targetDirectory, Option(absoluteHost))

  lazy val additionalHeaders: Seq[(String, String)] =
    absoluteHost.map(HOST -> _).map(Seq(_)).getOrElse(Seq())

  def scrape(app: Application): Future[Seq[Unit]] = {
    val contextualizedRoutes = routesToScrape.map(contextualizePath(app))
    Future.traverse(contextualizedRoutes)(scrapeRoute(app, targetDirectory))
  }

  def scrapeRoute(app: Application, targetDirectory: File)(path: String): Future[Unit] = {
    val fileStream = new FileOutputStream(pathToFile(targetDirectory.getPath, path))
    renderPage(app, path, fileStream).map { _ => fileStream.close() }
  }

  def simpleGetRequest(_path: String): RequestHeader = {
    new RequestHeader {
      def headers       = new play.api.mvc.Headers((ACCEPT -> "*/*") +: additionalHeaders)
      def id            = Random.nextInt
      def method        = "GET"
      def path          = _path
      def queryString   = Map[String, Seq[String]]()
      def remoteAddress = "0.0.0.0"
      def secure        = false
      def tags          = Map[String, String]()
      def uri           = _path
      def version       = "HTTP/1.1"
    }
  }

  private def contextualizePath(app: Application)(requestedPath: String) =
    app.configuration.getString("application.context")
      .map(context => s"$context$requestedPath".replaceAll("//", "/"))
      .getOrElse(requestedPath)

  private def renderPage(app: Application, path: String, outputStream: OutputStream): Future[Unit] = {
    val req = simpleGetRequest(path)
    val (_, handler) = app.requestHandler.handlerForRequest(req)
    val action = handler.asInstanceOf[EssentialAction]
    val fileWritingIteratee =
      Iteratee.foreach[Array[Byte]](bytes => outputStream.write(bytes, 0, bytes.length))
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
}
