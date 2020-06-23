import play.sbt.PlayScala

name := """robot-rumble"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .dependsOn(scalaElo)
lazy val scalaElo = ProjectRef(
  uri("git://github.com/robot-rumble/scala-elo.git#v1.0.3"),
  "scala-elo"
)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.11"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
libraryDependencies += "com.github.t3hnar" %% "scala-bcrypt" % "4.1"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.2.8",
  "com.twitter" %% "finagle-core" % "20.5.0",
  "com.twitter" %% "finagle-init" % "20.5.0",
  "io.getquill" %% "quill-jasync-postgres" % "3.5.3-SNAPSHOT",
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
  """.trim.stripMargin
