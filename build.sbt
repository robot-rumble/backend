import play.sbt.PlayScala

name := """robot-rumble"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .dependsOn(scalaElo)
lazy val scalaElo = ProjectRef(
  uri("git://github.com/robot-rumble/scala-elo.git#v1.0.2"),
  "scala-elo"
)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.13.0"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
libraryDependencies += "com.github.t3hnar" %% "scala-bcrypt" % "4.1"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.2.8",
  "com.typesafe.play" %% "play-slick" % "4.0.2",
  "com.github.tminglei" %% "slick-pg" % "0.19.0"
)

libraryDependencies += javaJdbc
libraryDependencies ++= Seq(
  "org.flywaydb" %% "flyway-play" % "6.0.0"
)

val AkkaVersion = "2.5.31"
val AkkaHttpVersion = "10.1.11"
libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "2.0.1",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Xfatal-warnings"
)

initialCommands in console :=
  """
    |import play.api.inject.guice.GuiceApplicationBuilder
    |import scala.concurrent.duration._
    |import scala.concurrent.{Future, Await}
    |import scala.concurrent.ExecutionContext.Implicits.global
    |import db.PostgresProfile.api._
    |import models._
    |def exec[T](program: Future[T]): T = Await.result(program, 2.seconds)
    |val app = new GuiceApplicationBuilder().build()
    |val usersRepo = app.injector.instanceOf[Users.Repo]
    |val robotsRepo = app.injector.instanceOf[Robots.Repo]
    |val publishedRobotsRepo = app.injector.instanceOf[PublishedRobots.Repo]
    |val battlesRepo = app.injector.instanceOf[Battles.Repo]
  """.trim.stripMargin
