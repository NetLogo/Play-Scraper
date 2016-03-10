sbtPlugin := true

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

name := "play-scraper"

organization := "org.nlogo"

version := "0.6.3"

isSnapshot := true

licenses += ("Public Domain", url("http://creativecommons.org/licenses/publicdomain/"))

scalaVersion := "2.10.6"

scalacOptions ++= Seq("-feature", "-deprecation")

addSbtPlugin(("com.typesafe.play" % "sbt-plugin" % "2.4.0").extra("scalaVersion" -> "2.10"))

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.4.0",
  "com.amazonaws" % "aws-java-sdk-cloudfront" % "1.10.59",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.10.59"
)

bintrayRepository := "play-scraper"

bintrayOrganization := Some("netlogo")
