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
  def simpleGetRequest(_path: String): RequestHeader = {
    new RequestHeader {
      def headers = new play.api.mvc.Headers {
        val data = Seq()
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
    // val app = new DefaultApplication(baseDirectory, loader, null, play.api.Mode.Dev).asInstanceOf[play.api.Application]
    println(app)
    println(app.configuration.getString("application.router"))
    println(app.routes)
    val indexRequest = simpleGetRequest("/")
    /*
    val action = rts.routes(indexRequest).asInstanceOf[EssentialAction]
    val consumer = Iteratee.getChunks[Array[Byte]]
    action(indexRequest).run.onComplete {
      res => println(res)
    }
    */
  }
}
