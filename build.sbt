import play.sbt.PlayScala

name := "rr"
version := "0.0.1"
maintainer := "antonoutkine@gmail.com"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.11"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
libraryDependencies += "com.github.t3hnar" %% "scala-bcrypt" % "4.1"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.1.4",
  "io.getquill" %% "quill-jasync-postgres" % "3.6.1",
)

libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.6.0",
  "com.beachape" %% "enumeratum-play" % "1.6.0",
  "com.beachape" %% "enumeratum-quill" % "1.6.0",
)

libraryDependencies ++= Seq(
  "org.flywaydb" %% "flyway-play" % "6.0.0"
)

libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "2.0.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.31",
  "com.typesafe.akka" %% "akka-http" % "10.1.11"
)

libraryDependencies += "com.github.daddykotex" %% "courier" % "2.0.0"

libraryDependencies += "io.sentry" % "sentry-logback" % "4.3.0"

libraryDependencies += "com.nixxcode.jvmbrotli" % "jvmbrotli" % "0.2.0"

libraryDependencies += "com.github.andriykuba" % "scala-glicko2" % "1.0.1"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Xfatal-warnings",
)

initialCommands in console :=
  """
    |import play.api.inject.guice.GuiceApplicationBuilder
    |import scala.concurrent.duration._
    |import scala.concurrent.{Future, Await}
    |import scala.concurrent.ExecutionContext.Implicits.global
    |import models._
    |import Schema._
    |val app = new GuiceApplicationBuilder().build()
    |val schema = app.injector.instanceOf[Schema]
    |import schema._
    |import schema.ctx._
    |val usersRepo = app.injector.instanceOf[Users]
    |val robotsRepo = app.injector.instanceOf[Robots]
    |val battlesRepo = app.injector.instanceOf[Battles]
    |val boardsRepo = app.injector.instanceOf[Boards]
    |val seasonsRepo = app.injector.instanceOf[Seasons]
    |  """.trim.stripMargin
