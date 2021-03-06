// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

import java.io.File

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class TaskExecutionError(err: List[String]) extends Exception(err.mkString)

final case class TaskResult[+R](res: Try[R], out: List[String], err: List[String])

trait Description {
  def description: String = ""
}

trait UsingSudo[T <: UsingSudo[T]] { this: T =>
  def sudo: T
}

trait UsingParallelExecution[T <: UsingParallelExecution[T]] { this: T =>
  import scala.concurrent.duration._
  implicit val timeout = 90 seconds

  def par: T
}

trait TaskM[+R] extends Description {
  self =>

  def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[R]

  def apply(): TaskResult[R] = run()

  def andThen[T >: R](task: TaskM[T]): TaskM[T] = self flatMap (_ => task)

  def map[U](f: R => U): TaskM[U] = new TaskM[U] {
    override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[U] = {
      val selfRes = self.run(verbose)
      selfRes.copy[U](res = selfRes.res.map(f))
    }
  }

  def flatMap[T](f: R => TaskM[T]): TaskM[T] = new TaskM[T] {
    override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[T] = {
      val selfRes: TaskResult[R] = self.run(verbose)

      selfRes.res match {
        case Success(r) =>
          val nextRes = f(r).run(verbose, input)

          nextRes.copy[T](out = selfRes.out ++ nextRes.out, err = selfRes.err ++ nextRes.err)

        case Failure(e) => TaskResult[T](Failure[T](e), selfRes.out, selfRes.err)
      }
    }
  }

  def orElse[T >: R](task: TaskM[T]): TaskM[T] = new TaskM[T] {
    override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[T] = {
      val selfRes: TaskResult[T] = self.run(verbose)

      selfRes.res match {
        case Success(r) => selfRes
        case _ => task.run(verbose)
      }
    }
  }

  def pipeTo[T](task: TaskM[T]): TaskM[T] = new TaskM[T] {
    override def run(verbose: VerbosityLevel, input: Option[TaskResult[_]] = None): TaskResult[T] = {
      val selfRes: TaskResult[R] = self.run(verbose)

      selfRes.res match {
        case Success(r) => task.run(verbose, Option(selfRes))
        case Failure(e) => TaskResult[T](Failure[T](e), selfRes.out, selfRes.err)
      }
    }
  }
}

object LoggedRun {
  def apply[R](verbose: VerbosityLevel, usingSudo: Boolean, usingPar: Boolean,
             hosts: Hosts, desc: String, task: TaskM[R], input: Option[TaskResult[_]]): (VerbosityLevel => TaskResult[R]) = {
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

      val result = task.run(v, input)

      verbose match {
        case Verbose | FullOutput => println("--------------------------------------------------------------")
        case _ =>
      }

      result
    }

    logRun
  }
}

final case class FailedTask(out: List[String], err: List[String]) extends TaskM[Boolean] {
  override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] =
    TaskResult(Failure[Boolean](new TaskExecutionError(Nil)), Nil, Nil)
}

final case object SuccessfulTask extends TaskM[Boolean] {
  override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] =
    TaskResult(Success(true), Nil, Nil)
}

class ShellTask(val ctx: Process, val op: Command)(implicit val user: User) extends TaskM[Boolean] {
  import scala.collection.mutable.ListBuffer
  import scala.sys.process._

  override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = op match {
    case Start => execute(ctx.startCmd, verbose, input)
    case Stop => execute(ctx.stopCmd, verbose, input)
  }

  def doOut(out: ListBuffer[String], verbose: VerbosityLevel)(line: String): Unit = {
    out.append(line)

    verbose match {
      case FullOutput => println(line)
      case _ =>
    }
  }

  private def mkCommandLog(cmd: CommandLine, verbose: VerbosityLevel, input: Option[TaskResult[_]]): String = verbose match {
    //TODO print input too
    case Verbose => s"${op.cmd} ${ctx.name} on '${ctx.host.toString}'"
    case FullOutput => s"$op '${ctx.name}' (${cmd.cmd}) on '${ctx.host.toString}'"
    case _ => ""
  }

  private def printCommandLog(msg: String, color: String, statusMsg: String, commandLine: Seq[String],
                              verbose: VerbosityLevel): Unit = verbose match {
    case Verbose | FullOutput =>
      println(s"$msg [$color $statusMsg ${Console.RESET}]")

      verbose match {
        case FullOutput => println(s"SSH: ${commandLine.mkString(" ")}")
        case _ =>
      }

    case _ =>
  }

  private def executeOnLocalhost(cmd: CommandLine, verbose: VerbosityLevel, input: Option[TaskResult[_]]): TaskResult[Boolean] = {
    val out = ListBuffer[String]()
    val err = ListBuffer[String]()

    val msg = mkCommandLog(cmd, verbose, input)

    val inputStr = mkInput(input)

    val result = cmd match {
      case SudoExec(_, _, _) =>
        //TODO convert all to Seq
        (s"/bin/echo '${user.localPassword().mkString}' | ${cmd.cmd}" run (ProcessLogger(doOut(out, verbose)(_),
          doOut(err, verbose)(_)))).exitValue()

      case Exec(_, _, _) if inputStr.isEmpty =>
        (cmd.cmd run (ProcessLogger(doOut(out, verbose)(_), doOut(err, verbose)(_)))).exitValue()

      case Exec(_, _, _) if !inputStr.isEmpty =>
        val (args: Array[String], file: Option[String]) = cmd.extractFileFromArgs()

        val command  = OS.getCurrentOs() match {
          case Linux => file match {
              case None => Seq("/bin/echo", "-e", inputStr) #| cmd.pipeCmd
              case Some(f) => Seq("/bin/echo", inputStr) #| cmd.pipeCmd #> new File(f)
            }

          case MacOS => file match {
              case None => Seq("/bin/echo", inputStr) #| cmd.pipeCmd
              case Some(f) => Seq("/bin/echo", "-n", inputStr) #| cmd.pipeCmd #> new File(f)
            }

          case _ => throw new IllegalArgumentException("Not supported OS")
        }

        (command run (ProcessLogger(doOut(out, verbose)(_), doOut(err, verbose)(_)))).exitValue()

      case NoExec => 0
    }

    if (result == 0) {
      verbose match {
        case Verbose | FullOutput => doOut(out, verbose)(s"$msg [${Console.GREEN} ok ${Console.RESET}]")
        case _ =>
      }

      TaskResult(Success(true), out.toList, err.toList)
    } else {
      verbose match {
        case Verbose | FullOutput => doOut(err, verbose)(s"$msg [${Console.RED} failed ${Console.RESET}]")
        case _ =>
      }

      TaskResult(Failure[Boolean](new TaskExecutionError(err.toList)), out.toList, err.toList)
    }
  }

  private def mkInput(input: Option[TaskResult[_]]): String = input.map { res =>
    OS.getCurrentOs() match {
      case Linux => res.out.mkString("\\n")
      case MacOS => res.out.mkString(System.lineSeparator())
      case _ => res.out.mkString(System.lineSeparator())
    }
  }.getOrElse("")

  private def buildSshCommandFor(remoteHost: Host, cmd: CommandLine, sshu: User with SshCredentials): Seq[String] = {
    val noHostKeyChecking = Seq("-o", "UserKnownHostsFile=/dev/null", "-o", "StrictHostKeyChecking=no")
    val keyFileArgs = sshu.keyFile.toList.flatMap("-i" :: _.getPath :: Nil)

    //FIXME convert all to Seq
    cmd match {
      case SudoExec(_, _, _) =>
        Seq("ssh", "-qtt") ++ noHostKeyChecking //::: keyFileArgs ::: s"${sshu.username}@${remoteHost.toString()}" ::
          //s"echo '${sshu.password().mkString}' | ${cmd.cmd}" :: Nil

      case Exec(_, _, _) =>
        Seq("ssh", "-qtt") ++ noHostKeyChecking //::: keyFileArgs ::: s"${sshu.username}@${remoteHost.toString()}" ::
          //cmd.cmd :: Nil

      case NoExec => Nil
    }
  }

  private def executeRemoteSsh(remoteHost: Host, cmd: CommandLine, verbose: VerbosityLevel, input: Option[TaskResult[_]]): TaskResult[Boolean] = {
    val out = ListBuffer[String]()
    val err = ListBuffer[String]()

    val msg = mkCommandLog(cmd, verbose, input)

    def remoteCommandLine(user: User): Seq[String] = try {
      user match {
        case sshu: SshUser => buildSshCommandFor(remoteHost, cmd, sshu)
        case sshu: SshUserWithPassword => buildSshCommandFor(remoteHost, cmd, sshu)

        case _ => Nil
      }
    } catch {
      case NonFatal(e) => Nil
    }

    val commandLine = remoteCommandLine(user)

    if (commandLine.isEmpty) {
      val noSsh = "No ssh credentials specified. Please provide a valid ssh username, ssh-key and password (see User.load)."

      err += noSsh
      out += noSsh

      TaskResult(Failure[Boolean](new TaskExecutionError(err.toList)), out.toList, err.toList)
    } else {
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

        TaskResult(Failure[Boolean](new TaskExecutionError(err.toList)), out.toList, err.toList)
      }
    }
  }

  private def execute(cmd: CommandLine, verbose: VerbosityLevel,
                      input: Option[TaskResult[_]]): TaskResult[Boolean] = ctx.host match {
    case Localhost => executeOnLocalhost(cmd, verbose, input)
    case remoteHost: Host => executeRemoteSsh(remoteHost, cmd, verbose, input)
  }
}
