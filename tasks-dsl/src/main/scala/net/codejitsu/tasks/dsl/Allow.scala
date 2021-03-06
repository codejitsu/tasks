// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

import net.codejitsu.tasks._
import net.codejitsu.tasks.dsl.Tasks.Clear

import scala.annotation.implicitNotFound

@implicitNotFound("Stage is not allowed to run task '${T}'.")
class Allow[S <: Stage, T <: TaskM[Boolean]]

object Allow {
  //all tasks enabled per default in Dev
  implicit val devAllowTouch: Dev Allow Touch[Dev] = new Allow[Dev, Touch[Dev]]
  implicit val devAllowWait: Dev Allow Wait[Dev] = new Allow[Dev, Wait[Dev]]
  implicit val devAllowUpload: Dev Allow Upload[Dev] = new Allow[Dev, Upload[Dev]]
  implicit val devAllowCheckUrl: Dev Allow CheckUrl[Dev] = new Allow[Dev, CheckUrl[Dev]]
  implicit val devAllowCp: Dev Allow Cp[Dev] = new Allow[Dev, Cp[Dev]]
  implicit val devAllowDownload: Dev Allow Download[Dev] = new Allow[Dev, Download[Dev]]
  implicit val devAllowMkdir: Dev Allow Mkdir[Dev] = new Allow[Dev, Mkdir[Dev]]
  implicit val devAllowMv: Dev Allow Mv[Dev] = new Allow[Dev, Mv[Dev]]
  implicit val devAllowRm: Dev Allow Rm[Dev] = new Allow[Dev, Rm[Dev]]
  implicit val devAllowRmIfExists: Dev Allow RmIfExists[Dev] = new Allow[Dev, RmIfExists[Dev]]
  implicit val devAllowGetRequest: Dev Allow GetRequest[Dev] = new Allow[Dev, GetRequest[Dev]]
  implicit val devAllowPostRequest: Dev Allow PostRequest[Dev] = new Allow[Dev, PostRequest[Dev]]
  implicit val devAllowInstallDeb: Dev Allow InstallDeb[Dev] = new Allow[Dev, InstallDeb[Dev]]
  implicit val devAllowStartService: Dev Allow StartService[Dev] = new Allow[Dev, StartService[Dev]]
  implicit val devAllowStopService: Dev Allow StopService[Dev] = new Allow[Dev, StopService[Dev]]
  implicit val devAllowStartTomcat: Dev Allow StartTomcat[Dev] = new Allow[Dev, StartTomcat[Dev]]
  implicit val devAllowStopTomcat: Dev Allow StopTomcat[Dev] = new Allow[Dev, StopTomcat[Dev]]
  implicit val devAllowShellScripts: Dev Allow ShellScript[Dev] = new Allow[Dev, ShellScript[Dev]]
  implicit val devAllowEcho: Dev Allow Echo[Dev] = new Allow[Dev, Echo[Dev]]
  implicit val devAllowCat: Dev Allow Cat[Dev] = new Allow[Dev, Cat[Dev]]
  implicit val devAllowTail: Dev Allow Tail[Dev] = new Allow[Dev, Tail[Dev]]
  implicit val devAllowClear: Dev Allow Clear[Dev] = new Allow[Dev, Clear[Dev]]
  implicit val devAllowGrep: Dev Allow Grep[Dev] = new Allow[Dev, Grep[Dev]]
  implicit val devAllowHead: Dev Allow Head[Dev] = new Allow[Dev, Head[Dev]]
}
