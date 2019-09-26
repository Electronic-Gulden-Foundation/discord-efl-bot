name := """discord-tip-bot"""
organization := "nl.egulden"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  guice,
  ws,

  // Database
  "mysql" %  "mysql-connector-java" % "8.0.17",
  "com.typesafe.play" %% "play-slick" % "4.0.2",
  "com.typesafe.play" %% "play-slick-evolutions" % "4.0.2",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.4.1",

  // Discord
  "net.dv8tion" % "JDA" % "4.0.0_46",

  // Utilities
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",

  // Test dependencies
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
  "org.scalamock"          %% "scalamock"          % "4.4.0" % Test
)

resolvers += Resolver.JCenterRepository

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "nl.egulden.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "nl.egulden.binders._"
