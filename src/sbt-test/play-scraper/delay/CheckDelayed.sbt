val checkDelayed = TaskKey[Unit]("checkDelayed")

checkDelayed := {
  import scala.io.Source
  val indexContents = Source.fromFile(target.value / "play-scrape" / "index.html").mkString
  if (! indexContents.contains("finished waiting")) {
    sys.error("failed to wait: " + indexContents)
  }
}
