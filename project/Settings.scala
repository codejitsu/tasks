import de.johoop.jacoco4sbt.JacocoPlugin.jacoco
import de.johoop.jacoco4sbt.Thresholds
import sbt._
import sbt.Keys._
import wartremover._
import scala.language.postfixOps
import bintray.BintrayPlugin._
import bintray.BintrayKeys._

object Settings extends Build {
  lazy val buildSettings = Seq(
    name                  := "tasks",
    normalizedName        := "tasks",
    organization          := "net.codejitsu",
    organizationHomepage  := Some(url("http://www.codejitsu.net")),
    scalaVersion          := Versions.ScalaVer,
    homepage              := Some(url("http://www.github.com/codejitsu/tasks")),

    crossScalaVersions    := Seq("2.11.6", "2.10.4")
  )

  override lazy val settings = super.settings ++ buildSettings

  lazy val publishSettings = bintrayPublishSettings ++ Seq(
    bintrayOrganization in bintray := Some("codejitsu"),
    bintrayPackageLabels in bintray := Seq("scala", "continuous deployment", "continuous integration", "shell"),
    publishMavenStyle := true,
    bintrayRepository in bintray := "maven",
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    publishTo := {
      publishTo.value /* Value set by bintray-sbt plugin */
    }
  )

  val parentSettings = buildSettings ++ publishSettings ++ Seq(
    publishArtifact := false
  )

  val scalacSettings = Seq("-encoding", "UTF-8", s"-target:jvm-${Versions.JDKVer}", "-feature", "-language:_",
    "-deprecation", "-unchecked", "-Xfatal-warnings", "-Xlint")

  val javacSettings = Seq("-encoding", "UTF-8", "-source", Versions.JDKVer,
    "-target", Versions.JDKVer, "-Xlint:deprecation", "-Xlint:unchecked")

  lazy val defaultSettings = testSettings ++ Seq(
    autoCompilerPlugins := true,
    scalacOptions       ++= scalacSettings,
    javacOptions        in Compile    ++= javacSettings,
    ivyLoggingLevel     in ThisBuild  := UpdateLogging.Quiet,
    parallelExecution   in ThisBuild  := false,
    parallelExecution   in Global     := false,
    ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
  ) ++ publishSettings// ++ Seq(wartremoverWarnings in (Compile, compile) ++= Warts.unsafe.filter(_ != Wart.DefaultArguments))

  val tests = inConfig(Test)(Defaults.testTasks) ++ inConfig(IntegrationTest)(Defaults.itSettings)

  val testOptionSettings = Seq(
    Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
    Tests.Argument(TestFrameworks.JUnit, "-oDF", "-v", "-a")
  )

  lazy val testSettings = tests ++ jacoco.settings ++ Seq(
    parallelExecution in Test             := false,
    parallelExecution in IntegrationTest  := false,
    testOptions       in Test             ++= testOptionSettings,
    testOptions       in IntegrationTest  ++= testOptionSettings,
    fork              in Test             := true,
    fork              in IntegrationTest  := true,
    (compile in IntegrationTest)        <<= (compile in Test, compile in IntegrationTest) map { (_, c) => c },
    managedClasspath in IntegrationTest <<= Classpaths.concat(managedClasspath in IntegrationTest, exportedProducts in Test),
    jacoco.thresholds in jacoco.Config := Thresholds(instruction = 70, method = 70, branch = 70, complexity = 70, line = 70, clazz = 70)
  )
}
