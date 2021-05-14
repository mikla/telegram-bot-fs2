val http4sVersion = "1.0.0-M21"
val specs2Version = "4.11.0"
val log4CatsVersion = "2.1.0"
val slf4jVersion = "1.7.30"
val kindProjectorVersion = "0.11.3"
val circeVersion = "0.14.0-M5"

lazy val root = (project in file("."))
  .settings(
    organization := "ru.pavkin",
    name := "telegram-bot-fs2",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "3.0.0-RC2",
    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-Xfatal-warnings",
      "-deprecation",
      "-unchecked",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps"
    ),
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "org.typelevel" %% "log4cats-core" % log4CatsVersion,
      "org.typelevel" %% "log4cats-slf4j" % log4CatsVersion,
      "org.slf4j" % "slf4j-simple" % slf4jVersion,
//      "org.specs2" %% "specs2-core" % specs2Version % "test"
    )
  )

