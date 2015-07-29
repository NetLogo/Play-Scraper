val checkDelayed = TaskKey[Unit]("checkDelayed")

checkDelayed := {
  import scala.io.Source
  val indexContents = Source.fromFile(target.value / "play-scrape" / "index.html").mkString
  if (! indexContents.contains("""href="http://netlogoweb.org/"""")) {
    println(indexContents)
    sys.error("Failed to give absolute path")
  }
}
