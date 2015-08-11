import org.apache.http.HttpResponse
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.DefaultHttpClient
import Def.{ inputKey, spaceDelimited }
import scala.io.Source

val checkPublicUpload = inputKey[Unit]("Check that an online file matches a local one")

def fetchRemote(uri: String): HttpResponse = {
  val req    = RequestBuilder.get().setUri(uri).build
  val client = new DefaultHttpClient()
  client.execute(req)
}

checkPublicUpload := {
  val args: Seq[String] = spaceDelimited("<arg>").parsed
  val localSource = Source.fromFile(target.value / "play-scrape" / args(1)).mkString
  val remoteResponse = fetchRemote(args(0))
  val remoteSource = Source.fromInputStream(remoteResponse.getEntity.getContent).mkString
  if (localSource != remoteSource) {
    println("remote and local files differed.")
    println("remote: ")
    println(remoteSource)
    println("local: ")
    println(localSource)
    sys.error("differences between local and remote files, aborting...")
  }
}
