// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._
import net.codejitsu.tasks.dsl.Tasks._

abstract class GenericTask(name: String, desc: String, hosts: Hosts, exec: String,
                           params: List[String] = Nil, usingSudo: Boolean = false,
                           usingPar: Boolean = false, cmd: Command = Start, taskRepr: String = "",
                           pipeCmd: Seq[String] = Seq[String]())(implicit user: User) extends TaskM[Boolean] {
  override def description: String = desc

  protected val procs: Processes = name on hosts ~> {
    case cmd => if (usingSudo) {
      Sudo ~ Exec(exec, params, pipeCmd)
    } else {
      Exec(exec, params, pipeCmd)
    }
  }

  protected val task: TaskM[Boolean] = if (usingPar) {
    import scala.concurrent.duration._
    implicit val timeout = 90 seconds

    procs !! cmd
  } else {
    procs ! cmd
  }

  override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] =
    LoggedRun(
      verbose,
      usingSudo,
      usingPar,
      hosts,
      taskRepr,
      task,
      input
    )(verbose)
}

