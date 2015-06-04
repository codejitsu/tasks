// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class TaskExecutionError(err: List[String]) extends Exception(err.mkString)

case class TaskResult[+R](res: Try[R], out: List[String], err: List[String])

trait Description {
  def description: String = ""
}

trait UsingSudo[T <: UsingSudo[T]] {
  def sudo: T
}

trait UsingParallelExecution[T <: UsingParallelExecution[T]] {
  import scala.concurrent.duration._
  implicit val timeout = 90 seconds

  def par: T
}

trait TaskM[+R] extends Description {
  self =>

  def run(verbose: VerbosityLevel = NoOutput): TaskResult[R]

  def apply(): TaskResult[R] = run()

  def andThen[T >: R](task: TaskM[T]): TaskM[T] = this flatMap (_ => task)

  def map[U](f: R => U): TaskM[U] = new TaskM[U] {
    override def run(verbose: VerbosityLevel = NoOutput): TaskResult[U] = {
      val selfRes = self.run(verbose)
      selfRes.copy(res = selfRes.res.map(f))
    }
  }

  def flatMap[T >: R](f: R => TaskM[T]): TaskM[T] = new TaskM[T] {
    override def run(verbose: VerbosityLevel = NoOutput): TaskResult[T] = {
      val selfRes = self.run(verbose)

      selfRes.res match {
        case Success(r) =>
          val nextRes = f(r).run(verbose)

          nextRes.copy(out = selfRes.out ++ nextRes.out, err = selfRes.err ++ nextRes.err)

        case _ => selfRes
      }
    }
  }
}

object LoggedRun {
  def apply[R](verbose: VerbosityLevel, usingSudo: Boolean, usingPar: Boolean,
             hosts: Hosts, desc: String, task: TaskM[R]): (VerbosityLevel => TaskResult[R]) = {
    val logRun: (VerbosityLevel => TaskResult[R]) = { v =>
      verbose match {
        case Verbose | FullOutput =>
          val withSudo = if(usingSudo) {
            s"${Console.GREEN}sudo${Console.RESET}"
          } else {
            ""
          }

          val withPar = if(usingPar) {
            s"${Console.GREEN}!!${Console.RESET}"
          } else {
            ""
          }

          val h = if (hosts.hosts.size > 1) {
            " (and " + (hosts.hosts.size - 1) + " other hosts)"
          } else {
            ""
          }

          println(s"[ ${Console.YELLOW}*${Console.RESET} $withSudo $withPar] $desc " +
            s"on ${hosts.hosts.head.toString()}$h")
        case _ =>
      }

      val result = task.run(v)

      verbose match {
        case Verbose | FullOutput => println("--------------------------------------------------------------")
        case _ =>
      }

      result
    }

    logRun
  }
}

case class FailedTask(out: List[String], err: List[String]) extends TaskM[Boolean] {
  override def run(verbose: VerbosityLevel = NoOutput): TaskResult[Boolean] = TaskResult(Failure(new TaskExecutionError(Nil)), Nil, Nil)
}

case object EmptyTask extends TaskM[Boolean] {
  override def run(verbose: VerbosityLevel = NoOutput): TaskResult[Boolean] = TaskResult(Success(true), Nil, Nil)
}

class ShellTask(val ctx: Process, val op: Command)(implicit val user: User) extends TaskM[Boolean] {
  import scala.collection.mutable.ListBuffer
  import scala.sys.process._

  override def run(verbose: VerbosityLevel = NoOutput): TaskResult[Boolean] = op match {
    case Start => execute(ctx.startCmd, verbose)

    case Stop => execute(ctx.stopCmd, verbose)

    case _ => ???
  }

  def doOut(out: ListBuffer[String], verbose: VerbosityLevel)(line: String): Unit = {
    out.append(line)

    verbose match {
      case FullOutput => println(line)
      case _ =>
    }
  }

  private def mkCommandLog(cmd: CommandLine, verbose: VerbosityLevel): String = verbose match {
    case Verbose => s"${op.cmd} ${ctx.name} on '${ctx.host.toString}'"
    case FullOutput => s"$op '${ctx.name}' (${cmd.cmd}) on '${ctx.host.toString}'"
    case _ => ""
  }

  private def printCommandLog(msg: String, color: String, statusMsg: String, commandLine: List[String],
                              verbose: VerbosityLevel): Unit = verbose match {
    case Verbose | FullOutput =>
      println(s"$msg [$color $statusMsg ${Console.RESET}]")

      verbose match {
        case FullOutput => println(s"SSH: ${commandLine.mkString(" ")}")
        case _ =>
      }

    case _ =>
  }

  private def executeLocal(cmd: CommandLine, verbose: VerbosityLevel): TaskResult[Boolean] = {
    val out = ListBuffer[String]()
    val err = ListBuffer[String]()

    val msg = mkCommandLog(cmd, verbose)

    val result = cmd match {
      case SudoExec(_, _*) =>
        (s"echo '${user.localPassword().mkString}' | ${cmd.cmd}" run (ProcessLogger(doOut(out, verbose)(_),
          doOut(err, verbose)(_)))).exitValue()

      case Exec(_, _*) =>
        (cmd.cmd run (ProcessLogger(doOut(out, verbose)(_), doOut(err, verbose)(_)))).exitValue()

      case NoExec => 0
    }

    if (result == 0) {
      verbose match {
        case Verbose | FullOutput => println(s"$msg [${Console.GREEN} ok ${Console.RESET}]")
        case _ =>
      }

      TaskResult(Success(true), out.toList, err.toList)
    } else {
      verbose match {
        case Verbose | FullOutput => println(s"$msg [${Console.RED} failed ${Console.RESET}]")
        case _ =>
      }

      TaskResult(Failure(new TaskExecutionError(err.toList)), out.toList, err.toList)
    }
  }

  private def buildSshCommandFor(remoteHost: Host, cmd: CommandLine, sshu: User with SshCredentials) = {
    val noHostKeyChecking = "-o" :: "UserKnownHostsFile=/dev/null" :: "-o" :: "StrictHostKeyChecking=no" :: Nil
    val keyFileArgs = sshu.keyFile.toList.flatMap("-i" :: _.getPath :: Nil)

    cmd match {
      case SudoExec(_, _*) =>
        "ssh" :: "-qtt" :: noHostKeyChecking ::: keyFileArgs ::: s"${sshu.username}@${remoteHost.toString()}" ::
          s"echo '${sshu.password().mkString}' | ${cmd.cmd}" :: Nil

      case Exec(_, _*) =>
        "ssh" :: "-qtt" :: noHostKeyChecking ::: keyFileArgs ::: s"${sshu.username}@${remoteHost.toString()}" ::
          cmd.cmd :: Nil

      case NoExec => Nil
    }
  }

  private def executeRemoteSsh(remoteHost: Host, cmd: CommandLine, verbose: VerbosityLevel): TaskResult[Boolean] = {
    val out = ListBuffer[String]()
    val err = ListBuffer[String]()

    val msg = mkCommandLog(cmd, verbose)

    def remoteCommandLine(user: User): List[String] = try {
      user match {
        case sshu: SshUser => buildSshCommandFor(remoteHost, cmd, sshu)
        case sshu: SshUserWithPassword => buildSshCommandFor(remoteHost, cmd, sshu)

        case _ => Nil
      }
    } catch {
      case NonFatal(e) => Nil
    }

    val commandLine = remoteCommandLine(user)

    val proc = commandLine run (ProcessLogger(doOut(out, verbose)(_), doOut(err, verbose)(_)))
    val result = proc.exitValue

    val statusMsg = if (result == 0) {
      "ok"
    } else {
      "failed"
    }

    if (result == 0) {
      printCommandLog(msg, Console.GREEN, statusMsg, commandLine, verbose)

      TaskResult(Success(true), out.toList, err.toList)
    } else {
      printCommandLog(msg, Console.RED, statusMsg, commandLine, verbose)

      TaskResult(Failure(new TaskExecutionError(err.toList)), out.toList, err.toList)
    }
  }

  private def execute(cmd: CommandLine, verbose: VerbosityLevel): TaskResult[Boolean] = ctx.host match {
    case Localhost => executeLocal(cmd, verbose)
    case remoteHost: Host => executeRemoteSsh(remoteHost, cmd, verbose)
  }
}
