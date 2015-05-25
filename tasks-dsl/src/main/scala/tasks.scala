// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

import java.util.concurrent.TimeoutException

import net.codejitsu.tasks.dsl.Dsl._
import net.codejitsu.tasks.dsl.VerbosityLevel.{VerbosityLevel, _}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

//All shell specific tasks go here

/**
 * Create file task.
 *
 * @param hosts target hosts
 * @param target target file
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel ececution required.
 * @param user user
 */
case class Touch(hosts: Hosts, target: String,
                 usingSudo: Boolean = false, usingPar: Boolean = false, exec: String = "/usr/bin/touch")(implicit user: User)
  extends TaskM[Boolean] with UsingSudo[Touch] with UsingParallelExecution[Touch] {

  private val touch: Processes = "touch" on hosts ~> {
    case Start => if (usingSudo) {
      Sudo ~ Exec(exec, target)
    } else{
      Exec(exec, target)
    }
  }

  private val touchTask: TaskM[Boolean] = if (usingPar) {
    touch !! Start
  } else {
    touch ! Start
  }

  override def description: String = "create"

  override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) =
    LoggedRun(
      verbose,
      usingSudo,
      usingPar,
      hosts,
      s"$description '${target}'",
      touchTask
    )(verbose)

  override def sudo: Touch = this.copy(usingSudo = true)

  override def par: Touch = this.copy(usingPar = true)
}

/**
 * Remove file / dir task.
 *
 * @param hosts target hosts
 * @param target file/dir to remove
 * @param params command flags
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
class Rm(hosts: Hosts, target: String, params: List[String] = Nil,
         usingSudo: Boolean = false, usingPar: Boolean = false, exec: String = "/bin/rm")(implicit user: User)
  extends TaskM[Boolean] with UsingSudo[Rm] with UsingParallelExecution[Rm] {

  private val rm: Processes = "rm" on hosts ~> {
    case Start => if(usingSudo) {
      Sudo ~ Exec(exec, params ::: List(target) :_*)
    } else {
      Exec(exec, params ::: List(target) :_*)
    }
  }

  private val rmTask: TaskM[Boolean] = if(usingPar) {
    rm !! Start
  } else {
    rm ! Start
  }

  override def description: String = "remove file(s)"

  override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) =
    LoggedRun(
      verbose,
      usingSudo,
      usingPar,
      hosts,
      s"$description '${target}'",
      rmTask
    )(verbose)

  override def sudo: Rm = Rm(hosts, target, params, true, usingPar)

  override def par: Rm = Rm(hosts, target, params, usingSudo, true)
}

object Rm {
  def apply(hosts: Hosts, target: String, params: List[String] = Nil, usingSudo: Boolean = false, usingPar: Boolean = false)(implicit user: User): Rm =
    new Rm(hosts, target, params, usingSudo, usingPar)(user)
}

/**
 * Remove file / dir task (ignore errors if target not exists).
 *
 * @param hosts target hosts
 * @param target file/dir to remove
 * @param usingSudo true, if task have to be started with sudo
 * @param user user
 */
case class RmIfExists(hosts: Hosts, target: String, usingSudo: Boolean = false,
                      usingPar: Boolean = false)(implicit user: User) extends Rm(hosts, target, List("-f"), usingSudo, usingPar)(user) {
  override def description: String = "remove file(s) (if exists)"
}

/**
 * Move file / dir task.
 *
 * @param hosts target hosts
 * @param source source file to move (rename)
 * @param destination destination file/dir
 * @param params command flags
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
class Mv(hosts: Hosts, source: String, destination: String, params: List[String] = Nil,
         usingSudo: Boolean = false, usingPar: Boolean = false, exec: String = "/bin/mv")(implicit user: User)
  extends TaskM[Boolean] with UsingSudo[Mv] with UsingParallelExecution[Mv] {

  private val mv: Processes = "mv" on hosts ~> {
    case Start => if(usingSudo) {
      Sudo ~ Exec(exec,  params ::: List(source, destination) :_*)
    } else {
      Exec(exec,  params ::: List(source, destination) :_*)
    }
  }

  private val mvTask: TaskM[Boolean] = if(usingPar) {
    mv !! Start
  } else {
    mv ! Start
  }

  override def description: String = "move file(s)"

  override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) =
    LoggedRun(
      verbose,
      usingSudo,
      usingPar,
      hosts,
      s"$description '${source}' -> '${destination}'",
      mvTask
    )(verbose)

  override def sudo: Mv = Mv(hosts, source, destination, params, true, usingPar)

  override def par: Mv = Mv(hosts, source, destination, params, usingSudo, true)
}

object Mv {
  def apply(hosts: Hosts, source: String, destination: String,
            params: List[String] = Nil, usingSudo: Boolean = false, usingPar: Boolean = false)(implicit user: User): Mv =
    new Mv(hosts, source, destination, params, usingSudo, usingPar)(user)
}

/**
 * Copy file / dir task.
 *
 * @param hosts target hosts
 * @param source source object
 * @param destination destination object
 * @param params task flags
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel ececution required.
 * @param user user
 */
class Cp(hosts: Hosts, source: String, destination: String,
         params: List[String] = Nil, usingSudo: Boolean = false,
         usingPar: Boolean = false, exec: String = "/usr/bin/rsync")(implicit user: User)
  extends TaskM[Boolean] with UsingSudo[Cp] with UsingParallelExecution[Cp] {

  private val rsync: Processes = "rsync" on hosts ~> {
    case Start => if(usingSudo) {
      Sudo ~ Exec(exec, params ::: List(source, destination): _*)
    } else {
      Exec(exec, params ::: List(source, destination): _*)
    }
  }

  private val rsyncTask: TaskM[Boolean] = if (usingPar) {
    rsync !! Start
  } else {
    rsync ! Start
  }

  override def description: String = "copy file(s)"

  override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) =
    LoggedRun(
      verbose,
      usingSudo,
      usingPar,
      hosts,
      s"$description '${source}' -> '${destination}'",
      rsyncTask
    )(verbose)

  override def sudo: Cp = Cp(hosts, source, destination, params, true, usingPar)

  override def par: Cp = Cp(hosts, source, destination, params, usingSudo, true)
}

object Cp {
  def apply(hosts: Hosts, source: String, destination: String, params: List[String] = Nil,
            sudo: Boolean = false, parallel: Boolean = false)(implicit user: User): Cp =
    new Cp(hosts, source, destination, params, sudo, parallel)(user)

  def apply(hosts: Hosts, source: String, destination: String)(implicit user: User): Cp = Cp(hosts, source, destination, Nil)
}

/**
 * Upload file to remote host(s).
 *
 * @param source source file to upload
 * @param target destination hosts
 * @param destinationPath path on destination hosts
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel ececution required.
 * @param user user
 */
case class Upload(target: Hosts, source: String, destinationPath: String,
                  usingSudo: Boolean = false, usingPar: Boolean = false, exec: String = "/usr/bin/rsync")(implicit user: LocalUser)
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

  override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) =
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

/**
 * Stop tomcat service.
 *
 * @param hosts hosts.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel ececution required.
 * @param user user.
 */
case class StopTomcat(hosts: Hosts, usingSudo: Boolean = false,
                      usingPar: Boolean = false, exec: String = "/etc/init.d/tomcat7")(implicit user: User)
  extends TaskM[Boolean] with UsingSudo[StopTomcat] with UsingParallelExecution[StopTomcat] {

  private val tomcats: Processes = "tomcat" on hosts ~> {
    case Stop => if(usingSudo) {
      Sudo ~ Exec(exec, "stop")
    } else {
      Exec(exec, "stop")
    }
  }

  private val tomcatsTask: TaskM[Boolean] = if (usingPar) {
    tomcats !! Stop
  } else {
    tomcats ! Stop
  }

  override def description: String = "stop tomcat service"

  override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) =
    LoggedRun(
      verbose,
      usingSudo,
      usingPar,
      hosts,
      description,
      tomcatsTask
    )(verbose)

  override def sudo: StopTomcat = this.copy(usingSudo = true)

  override def par: StopTomcat = this.copy(usingPar = true)
}

/**
 * Start tomcat service.
 *
 * @param hosts hosts.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel ececution required.
 * @param user user.
 */
case class StartTomcat(hosts: Hosts, usingSudo: Boolean = false,
                       usingPar: Boolean = false, exec: String = "/etc/init.d/tomcat7")(implicit user: User)
  extends TaskM[Boolean] with UsingSudo[StartTomcat] with UsingParallelExecution[StartTomcat] {

  private val tomcats: Processes = "tomcat" on hosts ~> {
    case Start => if(usingSudo) {
      Sudo ~ Exec(exec, "start")
    } else {
      Exec(exec, "start")
    }
  }

  private val tomcatsTask: TaskM[Boolean] = if (usingPar) {
    tomcats !! Start
  } else {
    tomcats ! Start
  }

  override def description: String = "start tomcat service"

  override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) =
    LoggedRun(
      verbose,
      usingSudo,
      usingPar,
      hosts,
      description,
      tomcatsTask
    )(verbose)

  override def sudo: StartTomcat = this.copy(usingSudo = true)

  override def par: StartTomcat = this.copy(usingPar = true)
}

/**
 * Wait task.
 *
 * @param d duration.
 */
case class Wait(d: Duration) extends TaskM[Boolean] {
  override def description: String = "waiting"

  override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) =
    LoggedRun(
      verbose,
      false,
      false,
      Localhost,
      s"$description for ${d.toString}",
      new TaskM[Boolean] {
        override def run(verbose: VerbosityLevel): (Try[Boolean], List[String], List[String]) = {
          val promise = Promise[Unit]

          val result = try {
            Await.ready(promise.future, d)
            (Failure(new TaskExecutionError(Nil)), Nil, Nil)
          } catch {
            case t: TimeoutException => (Success(true), Nil, Nil)
            case e: Throwable => (Failure(new TaskExecutionError(List(e.getMessage))), Nil, Nil)
          }

          result
        }
      }
    )(verbose)
}

/**
 * Check url with predicate.
 *
 * @param hosts hosts to call over http.
 * @param path app context.
 * @param port app port.
 * @param checkFun predicate on response text.
 * @param usingPar true, if parallel execution required.
 */
case class CheckUrl(hosts: Hosts, path: String, port: Int = CheckUrl.DefaultPort,
                    checkFun: (String => Boolean) = _ => true, usingPar: Boolean = false)
  extends TaskM[Boolean] with UsingParallelExecution[CheckUrl] {

  private val tasks: collection.immutable.Seq[TaskM[Boolean]] = hosts.hosts.map { host =>
    new TaskM[Boolean] {
      private def mkCommandLog(host: String, verbose: VerbosityLevel): String = verbose match {
        case Verbose => s"check $host:$port$path"
        case Full => s"check $host:$port$path"
        case _ => ""
      }

      private def printCommandLog(msg: String, color: String, statusMsg: String, verbose: VerbosityLevel): Unit = verbose match {
        case Verbose | Full =>
          println(s"$msg [$color $statusMsg ${Console.WHITE}]")
        case _ =>
      }

      override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) = {
        import scala.io.Source

        val prot = if (host.toString().startsWith("http://")) {
          ""
        } else {
          "http://"
        }

        val msg = mkCommandLog(host.toString(), verbose)

        val resp = Try(Source.fromURL(s"$prot$host:$port$path"))
        val html = resp match {
          case Success(h) => (h.mkString, true, "")
          case Failure(e) => ("", false, e.getMessage)
        }

        val resultSuccess = html._2 && checkFun(html._1)

        if (resultSuccess) {
          printCommandLog(msg, Console.GREEN, "ok", verbose)
          (Success(true), Nil, Nil)
        } else {
          printCommandLog(msg, Console.RED, "failed", verbose)
          (Failure(new TaskExecutionError(List("Check function failed."))), Nil, Nil)
        }
      }
    }
  }

  override def description: String = "check url"

  private def printTaskProgress(verbose: VerbosityLevel): Unit = verbose match {
    case Verbose | Full =>
      val h = if (hosts.hosts.nonEmpty) {
        "(and " + (hosts.hosts.size - 1) + " other hosts)"
      } else {
        ""
      }

      val withPar = if(usingPar) {
        s"${Console.GREEN}!!${Console.WHITE}"
      } else {
        ""
      }

      println(s"[ ${Console.YELLOW}*${Console.WHITE} $withPar] $description on ${hosts.hosts.head.toString()} $h")
    case _ =>
  }

  override def run(verbose: VerbosityLevel): (Try[Boolean], List[String], List[String]) = {
    printTaskProgress(verbose)

    val tasksFold = if (usingPar) {
      new TaskM[Boolean] {
        override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) = {
          import scala.concurrent.ExecutionContext.Implicits.global

          val tasksF = tasks
            .map(t => () => Future {
            t.run(verbose)
          })

          val tasksFRes = Future.sequence(tasksF.map(_()))

          val result = Await.result(tasksFRes, timeout)

          val resultSuccess = result.map(_._1.isSuccess).forall(identity)

          val resultOut = result.
            filter(_._1.isSuccess).
            map(_._2).
            foldLeft(List.empty[String])((acc, out) => acc ++ out)

          val resultErr = result.
            filter(_._1.isSuccess).
            map(_._3).
            foldLeft(List.empty[String])((acc, err) => acc ++ err)

          if (resultSuccess) {
            (Success(true), resultOut, resultErr)
          } else {
            (Failure(new TaskExecutionError(resultErr)), resultOut, resultErr)
          }
        }
      }
    } else {
      tasks.foldLeft[TaskM[Boolean]](EmptyTask)((acc, t) => acc flatMap(_ => t))
    }

    val result = tasksFold.run(verbose)

    verbose match {
      case Verbose | Full => println("--------------------------------------------------------------")
      case _ =>
    }

    result
  }

  override def par: CheckUrl = this.copy(usingPar = true)
}

object CheckUrl {
  final val DefaultPort = 8080
}

//TODO define extensible custom task template
