name := """discord-efl-bot"""
organization := "nl.egulden"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(
    DockerPlugin,
    PlayScala,
  )
  .disablePlugins(
    PlayLayoutPlugin,
  )

scalaVersion := "2.13.1"

dockerRepository := Some("registry.gitlab.com/electronic-gulden-foundation/discord-efl-bot")
dockerBaseImage := "openjdk:11-jre"
dockerExposedPorts := Seq(9000)

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

  // Bitcoin JSON Client
  "wf.bitcoin" % "bitcoin-rpc-client" % "1.1.1",

  // CLI options parsing
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",

  // QR Codes
  "com.github.kenglxn.QRGen" % "javase" % "2.6.0",

  // Test dependencies
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
  "org.scalamock" %% "scalamock" % "4.4.0" % Test
)

resolvers += Resolver.JCenterRepository
resolvers += "jitpack" at "https://jitpack.io"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "nl.egulden.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "nl.egulden.binders._"
