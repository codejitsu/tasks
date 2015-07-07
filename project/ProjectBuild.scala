import sbt._
import sbt.Keys._

object ProjectBuild extends Build {
  import Settings._

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = parentSettings,
    aggregate = Seq(tasksDsl)
  )

  lazy val tasksDsl = Project(
    id = "tasks-dsl",
    base = file("./tasks-dsl"),
    settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.tasksDsl)
  )
}

object Dependencies {
  import Versions._

  object TestDep {
    val scalatest     = "org.scalatest"  %% "scalatest" % ScalaTestVer % Test
  }

  import TestDep._

  val test = Seq(scalatest)

  /** Module deps */

  val tasksDsl = test
}
