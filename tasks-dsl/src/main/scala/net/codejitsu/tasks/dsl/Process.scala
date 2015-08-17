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
  def cmd: String = s"$path ${args.mkString(" ")}"
  def shortPath: String = path.split("/").last
}

final case class Exec(path: String, params: String*) extends CommandLine {
  def args: Array[String] = params.toArray
}

final case class SudoExec(path: String, params: String*) extends CommandLine {
  def args: Array[String] = params.toArray
  override def cmd: String = s"sudo -S ${super.cmd}"
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
