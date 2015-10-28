// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

sealed trait Command {
  def cmd: String
}

case object Start extends Command {
  val cmd = "start"
}

case object Stop extends Command {
  val cmd = "stop"
}

sealed trait CommandLine {
  def path: String
  def args: Array[String]
  def cmd: Seq[String] = Seq(path) ++ args.toSeq
  def shortPath: String = path.split("/").last
  def pipeCmd: Seq[String] = {
    val (args: Array[String], file: Option[String]) = this.extractFileFromArgs()

    val command  = OS.getCurrentOs() match {
      case Linux => file match {
        case None => Seq("/usr/bin/xargs", this.path) ++ args
        case Some(f) => Seq("/usr/bin/xargs", "-d", "\\n", this.path) ++ args
      }

      case MacOS => file match {
        case None => Seq("/usr/bin/xargs", "-0", this.path) ++ args
        case Some(f) => Seq("/usr/bin/xargs", "-0", this.path) ++ args
      }

      case _ => throw new IllegalArgumentException("Not supported OS")
    }

    command
  }

  def extractFileFromArgs(): (Array[String], Option[String]) = {
    val args = this.args.takeWhile(_ != ">")
    val fileData = this.args.dropWhile(_ != ">")

    val file = if (fileData.isEmpty) {
      None
    } else if (fileData.size == 2) {
      Some(fileData(1))
    } else {
      None
    }
    (args, file)
  }
}

final case class Exec(path: String, params: List[String] = Nil, pipe: Seq[String] = Seq[String]()) extends CommandLine {
  def args: Array[String] = params.toArray
  override def pipeCmd: Seq[String] = if(pipe.isEmpty) super.pipeCmd else pipe
}

final case class SudoExec(path: String, params: List[String] = Nil, pipe: Seq[String] = Seq[String]()) extends CommandLine {
  def args: Array[String] = params.toArray
  override def cmd: Seq[String] = Seq("sudo", "-S") ++ super.cmd
  override def pipeCmd: Seq[String] = if(pipe.isEmpty) super.pipeCmd else pipe
}

case object NoExec extends CommandLine {
  override val path: String = ""
  override val args: Array[String] = Array.empty[String]
}

final case class Process(name: String, host: HostLike, proc: PartialFunction[Command, CommandLine]) {
  def startCmd: CommandLine = {
    if (proc.isDefinedAt(Start)) {
      proc(Start)
    } else {
      NoExec
    }
  }

  def stopCmd: CommandLine = {
    if (proc.isDefinedAt(Stop)) {
      proc(Stop)
    } else {
      NoExec
    }
  }
}

final case class ProcessStep(proc: PartialFunction[Command, CommandLine], host: HostLike)

final case class ProcessSteps(steps: collection.immutable.Seq[ProcessStep])

final case class Processes(procs: collection.immutable.Seq[Process])
