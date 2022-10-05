name := """absolute"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, org.nlogo.PlayScrapePlugin)

// Can be removed with a future Play update, see https://github.com/playframework/playframework/releases/2.8.15
// -Jeremy B October 2022
libraryDependencies ++= Seq(
  "com.google.inject"            % "guice"                % "5.1.0",
  "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0"
)

scalaVersion := "2.12.17"

scrapeAbsoluteURL := Some("netlogoweb.org")
