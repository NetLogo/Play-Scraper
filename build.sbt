sbtPlugin := true

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

name := "play-scraper"

organization := "org.nlogo"

version := "0.1"

isSnapshot := true

licenses += ("Public Domain", url("http://creativecommons.org/licenses/publicdomain/"))

scalaVersion := "2.10.4"

addSbtPlugin(("com.typesafe.play" % "sbt-plugin" % "2.3.8").extra("scalaVersion" -> "2.10"))
