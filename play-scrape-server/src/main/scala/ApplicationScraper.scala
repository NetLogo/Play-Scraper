package org.nlogo

import akka.stream.scaladsl.{ FileIO, Sink }
import akka.stream.IOResult
import akka.util.ByteString

import java.io.{ File, FileOutputStream, OutputStream }

import java.util.{ List => JList }

import play.api.Application
import play.api.http.HeaderNames.{ HOST, ACCEPT }
import play.api.mvc.{ RequestHeader, EssentialAction, Session }
import play.api.mvc.request.{ RemoteConnection, RequestTarget, RequestAttrKey, Cell }
import play.api.libs.concurrent.MaterializerProvider
import play.api.DefaultApplication
import play.api.libs.typedmap.{ TypedMap, TypedEntry }

import scala.util.Try
import scala.util.Random
import scala.jdk.CollectionConverters._

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationScraper(routesToScrape: Seq[String], targetDirectory: File, absoluteHost: Option[String]) {
  def this(routesToScrape: JList[String], targetDirectory: File, absoluteHost: String) =
    this(routesToScrape.asScala.toSeq, targetDirectory, Option(absoluteHost))

  lazy val additionalHeaders: Seq[(String, String)] =
    absoluteHost.map(HOST -> _).map(Seq(_)).getOrElse(Seq())

  def scrape(app: Application): Future[Seq[IOResult]] = {
    val contextualizedRoutes = routesToScrape.map(contextualizePath(app))
    Future.traverse(contextualizedRoutes)(scrapeRoute(app, targetDirectory))
  }

  def scrapeRoute(app: Application, targetDirectory: File)(path: String): Future[IOResult] = {
    renderPage(app, path, pathToFile(targetDirectory.getPath, path))
  }

  def simpleGetRequest(_path: String): RequestHeader = {
    new RequestHeader {
      override val connection = RemoteConnection("0.0.0.0", false, None)
      override val method     = "GET"
      override val target     = RequestTarget(_path, _path, Map[String, Seq[String]]())
      override val version    = "HTTP/1.1"
      override val headers    = new play.api.mvc.Headers((ACCEPT -> "*/*") +: additionalHeaders)
      override val attrs      = TypedMap(
        new TypedEntry[Long](RequestAttrKey.Id, Random.nextInt()),
        new TypedEntry[Cell[Session]](RequestAttrKey.Session, Cell(new Session()))
      )
    }
  }

  private def contextualizePath(app: Application)(requestedPath: String) =
    app.configuration.get[Option[String]]("play.http.context")
      .map(context => s"$context$requestedPath".replaceAll("//", "/"))
      .getOrElse(requestedPath)

  private def renderPage(app: Application, path: String, file: File): Future[IOResult] = {
    val req = simpleGetRequest(path)
    val (_, handler) = app.requestHandler.handlerForRequest(req)
    val action = handler.asInstanceOf[EssentialAction]
    val fileWritingIteratee = FileIO.toPath(file.toPath)
    implicit val materializer = app.materializer
    action(req).run().flatMap(_.body.dataStream.runWith(fileWritingIteratee))
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
