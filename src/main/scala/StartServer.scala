package org.nlogo

import play.core.StaticApplication
import play.api.Mode
import play.api.mvc.RequestHeader
import play.api.mvc.EssentialAction
import play.api.libs.iteratee.Iteratee
import play.api.DefaultApplication
import play.core.classloader.{ ApplicationClassLoaderProvider, DelegatingClassLoader }

import scala.util.{ Success, Failure }
import scala.util.Random
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

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

  def apply(baseDirectory: java.io.File, loader: ClassLoader, targetDirectory: java.io.File): Unit = {
    val appProvider = new StaticApplication(baseDirectory)
    val Success(app) = appProvider.get
    val routes = app.routes.get

    def contentFor(path: String): Unit = {
      val req = simpleGetRequest(path)
      val action = routes.routes(req).asInstanceOf[EssentialAction]
      action(req).run.onComplete {
        case Success(res) =>
          val consumer = Iteratee.getChunks[Array[Byte]]
          Iteratee.flatten(res.body(consumer)).run.onComplete
          {
            case Success(body) =>
              val text = new String(body.reduceLeft(_ ++ _), "UTF-8")
              println(res.header)
              println(text.slice(0, 100))
            case Failure(f) =>
              println(s"FAILURE getting body of $path")
          }
        case Failure(f) =>
          println(s"FAILURE retrieving $path")
          println(f)
      }
    }

    contentFor("/")
    contentFor("/create-standalone")
    contentFor("/tortoise")
    contentFor("/model/list.json")
    contentFor("/model/statuses.json")
    contentFor("/netlogo-engine.js")
    contentFor("/netlogo-agentmodel.js")
  }
}
