import org.apache.http.HttpResponse
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.client.utils.DateUtils
import org.apache.http.impl.client.DefaultHttpClient
import Def.{ inputKey, spaceDelimited }
import scala.io.Source

val testSleep = inputKey[Unit]("Sleep")

val checkPublicUpload = inputKey[Unit]("Check that an online file matches a local one")

val checkUploadedMoreThan = inputKey[Unit]("Check that a file is more than x seconds old")

def fetchRemote(uri: String): HttpResponse = {
  val req    = RequestBuilder.get().setUri(uri).build
  val client = new DefaultHttpClient()
  client.execute(req)
}

testSleep := {
  Thread.sleep(spaceDelimited("<arg>").parsed(0).toInt * 1000)
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

checkUploadedMoreThan := {
  val args: Seq[String] = spaceDelimited("<arg>").parsed
  val secondsDifferent = args(0).toInt
  val lastModified = fetchRemote(args(1)).getHeaders("Last-Modified")(0).getValue
  val lmDateEpochSeconds = DateUtils.parseDate(lastModified).getTime
  val currentEpochSeconds = System.currentTimeMillis / 1000l;
  if (currentEpochSeconds - lmDateEpochSeconds > secondsDifferent) {
    println(currentEpochSeconds)
    println(lmDateEpochSeconds)
    sys.error("Unchanged file was reuploaded at " + lastModified)
  }
}
