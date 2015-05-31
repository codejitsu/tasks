// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

import java.net.URL
import java.util.concurrent.TimeoutException

import net.codejitsu.tasks.dsl.Dsl._
import net.codejitsu.tasks.dsl.VerbosityLevel.{VerbosityLevel, _}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

//All shell specific tasks go here

abstract class GenericTask(name: String, desc: String, hosts: Hosts, exec: String,
                       params: List[String] = Nil, usingSudo: Boolean = false,
                       usingPar: Boolean = false, cmd: Command = Start, taskRepr: String = "")(implicit user: User) extends TaskM[Boolean] {
  override def description: String = desc

  protected val procs: Processes = name on hosts ~> {
    case cmd => if (usingSudo) {
      Sudo ~ Exec(exec, params :_*)
    } else{
      Exec(exec, params :_*)
    }
  }

  protected val task: TaskM[Boolean] = if (usingPar) {
    import scala.concurrent.duration._
    implicit val timeout = 90 seconds

    procs !! cmd
  } else {
    procs ! cmd
  }

  override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) =
    LoggedRun(
      verbose,
      usingSudo,
      usingPar,
      hosts,
      taskRepr,
      task
    )(verbose)
}

/**
 * Create file task.
 *
 * @param hosts target hosts
 * @param target target file
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
case class Touch(hosts: Hosts, target: String,
                 usingSudo: Boolean = false, usingPar: Boolean = false, exec: String = "/usr/bin/touch")(implicit user: User)
  extends GenericTask("touch", "create file", hosts, exec, List(target),
    usingSudo, usingPar, taskRepr = s"create file '$target'") with UsingSudo[Touch] with UsingParallelExecution[Touch] {

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
class Rm(hosts: Hosts, target: String, params: List[String] = List("-r"),
         usingSudo: Boolean = false, usingPar: Boolean = false,
         exec: String = "/bin/rm", desc: String = "remove file(s)")(implicit user: User)
  extends GenericTask("rm", desc, hosts, exec, params ::: List(target),
    usingSudo, usingPar, taskRepr = s"$desc '$target'") with UsingSudo[Rm] with UsingParallelExecution[Rm] {

  override def sudo: Rm = Rm(hosts, target, params, true, usingPar, exec)
  override def par: Rm = Rm(hosts, target, params, usingSudo, true, exec)
}

object Rm {
  def apply(hosts: Hosts, target: String, params: List[String] = List("-r"), usingSudo: Boolean = false,
            usingPar: Boolean = false, exec: String = "/bin/rm")(implicit user: User): Rm =
    new Rm(hosts, target, params, usingSudo, usingPar, exec)(user)
}

/**
 * Remove file / dir if exists task.
 *
 * @param hosts target hosts
 * @param target file/dir to remove
 * @param params command flags
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
class RmIfExists(hosts: Hosts, target: String, params: List[String] = List("-rf"),
         usingSudo: Boolean = false, usingPar: Boolean = false,
         exec: String = "/bin/rm", desc: String = "remove file(s) (if exists)")(implicit user: User)
  extends GenericTask("rm", desc, hosts, exec, params ::: List(target),
    usingSudo, usingPar, taskRepr = s"$desc '$target'") with UsingSudo[RmIfExists] with UsingParallelExecution[RmIfExists] {

  override def sudo: RmIfExists = RmIfExists(hosts, target, params, true, usingPar, exec)
  override def par: RmIfExists = RmIfExists(hosts, target, params, usingSudo, true, exec)
}

object RmIfExists {
  def apply(hosts: Hosts, target: String, params: List[String] = List("-rf"), usingSudo: Boolean = false,
            usingPar: Boolean = false, exec: String = "/bin/rm")(implicit user: User): RmIfExists =
    new RmIfExists(hosts, target, params, usingSudo, usingPar, exec)(user)
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
  extends GenericTask("mv", "move file(s)", hosts, exec, params ::: List(source, destination),
    usingSudo, usingPar, taskRepr = s"move file(s) '${source}' -> '${destination}'") with UsingSudo[Mv] with UsingParallelExecution[Mv] {

  override def sudo: Mv = Mv(hosts, source, destination, params, true, usingPar, exec)
  override def par: Mv = Mv(hosts, source, destination, params, usingSudo, true, exec)
}

object Mv {
  def apply(hosts: Hosts, source: String, destination: String,
            params: List[String] = Nil, usingSudo: Boolean = false,
            usingPar: Boolean = false, exec: String = "/bin/mv")(implicit user: User): Mv =
    new Mv(hosts, source, destination, params, usingSudo, usingPar, exec)(user)
}

/**
 * Copy file / dir task.
 *
 * @param hosts target hosts
 * @param source source object
 * @param destination destination object
 * @param params task flags
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
class Cp(hosts: Hosts, source: String, destination: String,
         params: List[String] = Nil, usingSudo: Boolean = false,
         usingPar: Boolean = false, exec: String = "/usr/bin/rsync")(implicit user: User)
  extends GenericTask("rsync", "copy file(s)", hosts, exec, params ::: List(source, destination),
    usingSudo, usingPar, taskRepr = s"copy file(s) '${source}' -> '${destination}'") with UsingSudo[Cp] with UsingParallelExecution[Cp] {

  override def sudo: Cp = Cp(hosts, source, destination, params, true, usingPar, exec)
  override def par: Cp = Cp(hosts, source, destination, params, usingSudo, true, exec)
}

object Cp {
  def apply(hosts: Hosts, source: String, destination: String, params: List[String] = Nil,
            sudo: Boolean = false, parallel: Boolean = false, exec: String = "/usr/bin/rsync")(implicit user: User): Cp =
    new Cp(hosts, source, destination, params, sudo, parallel, exec)(user)

  def apply(hosts: Hosts, source: String, destination: String)(implicit user: User): Cp = Cp(hosts, source, destination, Nil)
}

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
 * @param usingPar true, if parallel execution required.
 * @param user user.
 */
case class StopTomcat(hosts: Hosts, usingSudo: Boolean = false,
                      usingPar: Boolean = false, exec: String = "/etc/init.d/tomcat7")(implicit user: User)
  extends GenericTask("tomcat", "stop tomcat service", hosts, exec, List("stop"),
    usingSudo, usingPar, Stop, "stop tomcat service") with UsingSudo[StopTomcat] with UsingParallelExecution[StopTomcat] {

  override def sudo: StopTomcat = this.copy(usingSudo = true)
  override def par: StopTomcat = this.copy(usingPar = true)
}

/**
 * Start tomcat service.
 *
 * @param hosts hosts.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel execution required.
 * @param user user.
 */
case class StartTomcat(hosts: Hosts, usingSudo: Boolean = false,
                       usingPar: Boolean = false, exec: String = "/etc/init.d/tomcat7")(implicit user: User)
  extends GenericTask("tomcat", "start tomcat service", hosts, exec, List("start"),
    usingSudo, usingPar, taskRepr = "start tomcat service") with UsingSudo[StartTomcat] with UsingParallelExecution[StartTomcat] {

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

/**
 * Download url.
 *
 * @param hosts hosts.
 * @param url url to download.
 * @param destinationPath destination path on hosts.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel execution required.
 * @param user user.
 */
case class Download(hosts: Hosts, url: URL, destinationPath: String, usingSudo: Boolean = false,
                usingPar: Boolean = false, exec: String = "/usr/bin/wget")(implicit user: User)
  extends GenericTask("wget", "download url", hosts, exec, List(url.toString, "-P", destinationPath),
    usingSudo, usingPar, taskRepr = s"download url '${url.toString}'") with UsingSudo[Download] with UsingParallelExecution[Download] {

  override def sudo: Download = this.copy(usingSudo = true)
  override def par: Download = this.copy(usingPar = true)
}

case class PostRequest(hosts: Hosts, path: String, data: String, headers: List[String] = Nil,
                       checkResponseFun: (String => Boolean) = _ => true,
                       checkStatusFun: (String => Boolean) = resp => resp.contains("200 OK"),
                       port: Int = PostRequest.DefaultPort,
                       usingSudo: Boolean = false, usingPar: Boolean = false, exec: String = "/usr/bin/curl")(implicit user: User)
  extends TaskM[Boolean] with UsingSudo[PostRequest] with UsingParallelExecution[PostRequest] {

  private val tasks: collection.immutable.Seq[TaskM[Boolean]] = hosts.hosts.map { host =>
    val process = s"curl to ${host.toString()}:$port$path" on Localhost ~> {
      case Start => if (usingSudo) {
        Sudo ~ Exec(exec, PostRequest.prepareParams(headers, data, host, path, port) :_*)
      } else{
        Exec(exec, PostRequest.prepareParams(headers, data, host, path, port) :_*)
      }
    }

    new ShellTask(process, Start) {
      private def mkCommandLog(host: String, verbose: VerbosityLevel): String = verbose match {
        case Verbose => s"check post request response to $host:$port$path"
        case Full => s"check post request response to $host:$port$path"
        case _ => ""
      }

      private def printCommandLog(msg: String, color: String, statusMsg: String, verbose: VerbosityLevel): Unit = verbose match {
        case Verbose | Full =>
          println(s"$msg [$color $statusMsg ${Console.WHITE}]")
        case _ =>
      }

      override def run(verbose: VerbosityLevel = No): (Try[Boolean], List[String], List[String]) = {
        val callRes = super.run(verbose)

        val callSuccess = callRes._1 match {
          case Success(true) => true
          case _ => false
        }

        val output = callRes._2.mkString(" ")

        val respRes = checkResponseFun(output)
        println("CHECK BODY FUN: " + respRes)

        val statusRes = checkStatusFun(output)
        println("CHECK STATUS FUN: " + statusRes)

        val resultSuccess = callSuccess && respRes && statusRes

        val msg = mkCommandLog(host.toString(), verbose)

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

  override def description: String = "make post request"

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

  override def sudo: PostRequest = this.copy(usingSudo = true)
  override def par: PostRequest = this.copy(usingPar = true)
}

object PostRequest {
  final val DefaultPort = 8080

  def prepareParams(headers: List[String], data: String, host: Host, path: String, port: Int): List[String] = {
    val postData: List[String] =
      List("--trace", "-", "-X", "POST") ++
        headers.map(h => s" -H $h") ++
        List(s" -d $data ") ++ List(s"${host.toString()}:$port$path")

    postData
  }
}

case class InstallDeb(hosts: Hosts, packFile: String, usingSudo: Boolean = false,
                      usingPar: Boolean = false, exec: String = "/usr/bin/dpkg")(implicit user: User)
  extends GenericTask("dpkg", "install debian package", hosts, exec, List("-i", packFile),
    usingSudo, usingPar, taskRepr = s"install debian package '$packFile'") with UsingSudo[InstallDeb] with UsingParallelExecution[InstallDeb] {

  override def sudo: InstallDeb = this.copy(usingSudo = true)
  override def par: InstallDeb = this.copy(usingPar = true)
}

case class StartService(hosts: Hosts, service: String, usingSudo: Boolean = false,
                      usingPar: Boolean = false, exec: String = "/etc/init.d/")(implicit user: User)
  extends GenericTask("service", "start service", hosts, s"$exec$service", List("start"),
    usingSudo, usingPar, taskRepr = s"start service '$service'") with UsingSudo[StartService] with UsingParallelExecution[StartService] {

  override def sudo: StartService = this.copy(usingSudo = true)
  override def par: StartService = this.copy(usingPar = true)
}

case class StopService(hosts: Hosts, service: String, usingSudo: Boolean = false,
                        usingPar: Boolean = false, exec: String = "/etc/init.d/")(implicit user: User)
  extends GenericTask("service", "stop service", hosts, s"$exec$service", List("stop"),
    usingSudo, usingPar, cmd = Stop, taskRepr = s"stop service '$service'")
  with UsingSudo[StopService] with UsingParallelExecution[StopService] {

  override def sudo: StopService = this.copy(usingSudo = true)
  override def par: StopService = this.copy(usingPar = true)
}
