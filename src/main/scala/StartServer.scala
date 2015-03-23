package org.nlogo

import play.core.StaticApplication
import play.api.Mode
import play.api.mvc.RequestHeader
import play.api.mvc.EssentialAction
import play.api.libs.iteratee.Iteratee
import play.api.DefaultApplication
import play.core.classloader.{ ApplicationClassLoaderProvider, DelegatingClassLoader }

import scala.util.Success
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

  def apply(baseDirectory: java.io.File, loader: ClassLoader): Unit = {
    println(baseDirectory)
    val appProvider = new StaticApplication(baseDirectory)
    val Success(app) = appProvider.get
    println(app.routes)
    val rts = app.routes.get
    /*
    val indexRequest = simpleGetRequest("/")
    val action = rts.routes(indexRequest).asInstanceOf[EssentialAction]
    action(indexRequest).run.onComplete {
      res => println(res)
    }
    */
    val klassStylesheetRh = simpleGetRequest("/assets/lib/jquery/jquery.js", "text/javascript")
    println(loader.getResource("public/stylesheets/classes.css"))
    val ksaction = rts.routes(klassStylesheetRh).asInstanceOf[EssentialAction]
    ksaction(klassStylesheetRh).run.onComplete {
      res => println(res)
    }
  }
}
