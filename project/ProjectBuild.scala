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

  object Compile {
    val config        = "com.typesafe"             % "config"               % TypesafeConfigVer
    val ssh           = "com.decodified"          %% "scala-ssh"            % ScalaSshVer
    val logback       = "ch.qos.logback"           % "logback-classic"      % LogbackVer
    val bouncy        = "org.bouncycastle"         % "bcprov-jdk16"         % "1.46"
    val jcraft        = "com.jcraft"               % "jzlib"                % "1.1.3"
  }

  object Test {
    val scalatest     = "org.scalatest"           %% "scalatest"            % ScalaTestVer      % "test"
    val scalacheck    = "org.scalacheck"          %% "scalacheck"           % ScalaCheckVer     % "test"
    val junit         = "junit"                    % "junit"                % JunitVer          % "test"

//    val abideExtra    = "com.typesafe"             % "abide-extra_2.11"     % AbideExtraVer     % "abide,test"
  }

  import Compile._

  val test = Seq(Test.scalatest, Test.scalacheck, Test.junit)

  /** Module deps */

  val tasksDsl = Seq(config, ssh, logback, bouncy, jcraft) ++ test
}
