// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl.Tasks._
import net.codejitsu.tasks.dsl._

/**
 * Upload file to remote host(s).
 *
 * @param source source file to upload
 * @param target destination hosts
 * @param destinationPath path on destination hosts
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
case class Upload(target: Hosts, source: String, destinationPath: String,
                  usingSudo: Boolean = false, usingPar: Boolean = false, exec: String = "/usr/bin/rsync")(implicit user: User)
  extends TaskM[Boolean] with UsingSudo[Upload] with UsingParallelExecution[Upload] {

  private lazy val uploadProcs = target.hosts map {
    case h: Host =>
      val up: Process = "rsync" on Localhost ~> {
        case Start => if (usingSudo) {
          Sudo ~ Exec(exec, "-avzhe", "ssh", source, s"${h.toString()}:$destinationPath")
        } else {
          Exec(exec, "-avzhe", "ssh", source, s"${h.toString()}:$destinationPath")
        }
      }

      up
  }

  private lazy val uploadTask: TaskM[Boolean] = if (usingPar) {
    Processes(uploadProcs) !! Start
  } else {
    Processes(uploadProcs) ! Start
  }

  override def description: String = "upload file(s)"

  override def run(verbose: VerbosityLevel = NoOutput): TaskResult[Boolean] =
    LoggedRun(
      verbose,
      usingSudo,
      usingPar,
      target,
      s"$description '${source}' -> '${destinationPath}'",
      uploadTask
    )(verbose)

  override def sudo: Upload = this.copy(usingSudo = true)

  override def par: Upload = this.copy(usingPar = true)
}