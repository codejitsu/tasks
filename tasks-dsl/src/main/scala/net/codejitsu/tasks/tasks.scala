// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import java.net.URL
import java.util.concurrent.TimeoutException

import net.codejitsu.tasks.dsl.Tasks._
import net.codejitsu.tasks.dsl._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
 * Wait task.
 *
 * @param d duration.
 */
case class Wait(d: Duration) extends TaskM[Boolean] {
  override def description: String = "waiting"

  override def run(verbose: VerbosityLevel = NoOutput): TaskResult[Boolean] =
    LoggedRun(
      verbose,
      false,
      false,
      Localhost,
      s"$description for ${d.toString}",
      new TaskM[Boolean] {
        override def run(verbose: VerbosityLevel): TaskResult[Boolean] = {
          val promise = Promise[Unit]

          val result = try {
            Await.ready(promise.future, d)
            TaskResult[Boolean](Failure(new TaskExecutionError(Nil)), Nil, Nil)
          } catch {
            case t: TimeoutException => TaskResult[Boolean](Success(true), Nil, Nil)
            case e: Throwable => TaskResult[Boolean](Failure(new TaskExecutionError(List(e.getMessage))), Nil, Nil)
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
        case FullOutput => s"check $host:$port$path"
        case _ => ""
      }

      private def printCommandLog(msg: String, color: String, statusMsg: String, verbose: VerbosityLevel): Unit = verbose match {
        case Verbose | FullOutput =>
          println(s"$msg [$color $statusMsg ${Console.RESET}]")
        case _ =>
      }

      override def run(verbose: VerbosityLevel = NoOutput): TaskResult[Boolean] = {
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
          TaskResult(Success(true), Nil, Nil)
        } else {
          printCommandLog(msg, Console.RED, "failed", verbose)
          TaskResult(Failure(new TaskExecutionError(List("Check function failed."))), Nil, Nil)
        }
      }
    }
  }

  override def description: String = "check url"

  private def printTaskProgress(verbose: VerbosityLevel): Unit = verbose match {
    case Verbose | FullOutput =>
      val h = if (hosts.hosts.nonEmpty) {
        "(and " + (hosts.hosts.size - 1) + " other hosts)"
      } else {
        ""
      }

      val withPar = if(usingPar) {
        s"${Console.GREEN}!!${Console.RESET}"
      } else {
        ""
      }

      println(s"[ ${Console.YELLOW}*${Console.RESET} $withPar] $description on ${hosts.hosts.head.toString()} $h")
    case _ =>
  }

  override def run(verbose: VerbosityLevel): TaskResult[Boolean] = {
    printTaskProgress(verbose)

    val tasksFold = if (usingPar) {
      new TaskM[Boolean] {
        override def run(verbose: VerbosityLevel = NoOutput): TaskResult[Boolean] = {
          import scala.concurrent.ExecutionContext.Implicits.global

          val tasksF = tasks
            .map(t => () => Future {
            t.run(verbose)
          })

          val tasksFRes = Future.sequence(tasksF.map(_()))

          val result = Await.result(tasksFRes, timeout)

          val resultSuccess = result.map(_.res.isSuccess).forall(identity)

          val resultOut = result.
            filter(_.res.isSuccess).
            map(_.out).
            foldLeft(List.empty[String])((acc, out) => acc ++ out)

          val resultErr = result.
            filter(_.res.isSuccess).
            map(_.err).
            foldLeft(List.empty[String])((acc, err) => acc ++ err)

          if (resultSuccess) {
            TaskResult(Success(true), resultOut, resultErr)
          } else {
            TaskResult(Failure(new TaskExecutionError(resultErr)), resultOut, resultErr)
          }
        }
      }
    } else {
      tasks.foldLeft[TaskM[Boolean]](EmptyTask)((acc, t) => acc flatMap(_ => t))
    }

    val result = tasksFold.run(verbose)

    verbose match {
      case Verbose | FullOutput => println("--------------------------------------------------------------")
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

/**
 * Execute POST request.
 *
 * @param hosts target hosts.
 * @param path application path.
 * @param data body of request.
 * @param headers request headers.
 * @param checkResponseFun check function for response body.
 * @param checkStatusFun check function for response status code.
 * @param port port of application.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel execution needed.
 * @param exec path to curl executable.
 * @param user user.
 */
case class PostRequest(hosts: Hosts, path: String, data: String, headers: List[String] = Nil,
                       checkResponseFun: (String => Boolean) = _ => true,
                       checkStatusFun: (String => Boolean) = _ => true,
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
        case FullOutput => s"check post request response to $host:$port$path"
        case _ => ""
      }

      private def printCommandLog(msg: String, color: String, statusMsg: String, verbose: VerbosityLevel): Unit = verbose match {
        case Verbose | FullOutput =>
          println(s"$msg [$color $statusMsg ${Console.RESET}]")
        case _ =>
      }

      override def run(verbose: VerbosityLevel = NoOutput): TaskResult[Boolean] = {
        val callRes = super.run(verbose)

        val callSuccess = callRes.res match {
          case Success(true) => true
          case _ => false
        }

        val output = callRes.out.mkString(" ")

        val respRes = checkResponseFun(output)

        val statusRes = checkStatusFun(output)

        val resultSuccess = callSuccess && respRes && statusRes

        val msg = mkCommandLog(host.toString(), verbose)

        if (resultSuccess) {
          printCommandLog(msg, Console.GREEN, "ok", verbose)
          TaskResult(Success(true), Nil, Nil)
        } else {
          printCommandLog(msg, Console.RED, "failed", verbose)
          TaskResult(Failure(new TaskExecutionError(List("Check function failed."))), Nil, Nil)
        }
      }
    }
  }

  override def description: String = "make post request"

  private def printTaskProgress(verbose: VerbosityLevel): Unit = verbose match {
    case Verbose | FullOutput =>
      val h = if (hosts.hosts.nonEmpty) {
        "(and " + (hosts.hosts.size - 1) + " other hosts)"
      } else {
        ""
      }

      val withPar = if(usingPar) {
        s"${Console.GREEN}!!${Console.RESET}"
      } else {
        ""
      }

      println(s"[ ${Console.YELLOW}*${Console.RESET} $withPar] $description on ${hosts.hosts.head.toString()} $h")
    case _ =>
  }

  override def run(verbose: VerbosityLevel): TaskResult[Boolean] = {
    printTaskProgress(verbose)

    val tasksFold = if (usingPar) {
      new TaskM[Boolean] {
        override def run(verbose: VerbosityLevel = NoOutput): TaskResult[Boolean] = {
          import scala.concurrent.ExecutionContext.Implicits.global

          val tasksF = tasks
            .map(t => () => Future {
            t.run(verbose)
          })

          val tasksFRes = Future.sequence(tasksF.map(_()))

          val result = Await.result(tasksFRes, timeout)

          val resultSuccess = result.map(_.res.isSuccess).forall(identity)

          val resultOut = result.
            filter(_.res.isSuccess).
            map(_.out).
            foldLeft(List.empty[String])((acc, out) => acc ++ out)

          val resultErr = result.
            filter(_.res.isSuccess).
            map(_.err).
            foldLeft(List.empty[String])((acc, err) => acc ++ err)

          if (resultSuccess) {
            TaskResult(Success(true), resultOut, resultErr)
          } else {
            TaskResult(Failure(new TaskExecutionError(resultErr)), resultOut, resultErr)
          }
        }
      }
    } else {
      tasks.foldLeft[TaskM[Boolean]](EmptyTask)((acc, t) => acc flatMap(_ => t))
    }

    val result = tasksFold.run(verbose)

    verbose match {
      case Verbose | FullOutput => println("--------------------------------------------------------------")
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

//TODO add a new GenericTask for such parallel tasks

/**
 * Execute GET request.
 *
 * @param hosts target hosts.
 * @param path application path.
 * @param checkResponseFun check function for response body.
 * @param checkStatusFun check function for response status code.
 * @param port port of application.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel execution needed.
 * @param exec path to curl executable.
 * @param user user.
 */
case class GetRequest(hosts: Hosts, path: String,
                      checkResponseFun: (String => Boolean) = _ => true,
                      checkStatusFun: (String => Boolean) = _ => true,
                      port: Int = GetRequest.DefaultPort,
                      usingSudo: Boolean = false, usingPar: Boolean = false, exec: String = "/usr/bin/curl")(implicit user: User)
  extends TaskM[Boolean] with UsingSudo[GetRequest] with UsingParallelExecution[GetRequest] {

  private val tasks: collection.immutable.Seq[TaskM[Boolean]] = hosts.hosts.map { host =>
    val process = s"curl to ${host.toString()}:$port$path" on Localhost ~> {
      case Start => if (usingSudo) {
        Sudo ~ Exec(exec, GetRequest.prepareParams(host, path, port) :_*)
      } else{
        Exec(exec, GetRequest.prepareParams(host, path, port) :_*)
      }
    }

    new ShellTask(process, Start) {
      private def mkCommandLog(host: String, verbose: VerbosityLevel): String = verbose match {
        case Verbose => s"check get request response to $host:$port$path"
        case FullOutput => s"check get request response to $host:$port$path"
        case _ => ""
      }

      private def printCommandLog(msg: String, color: String, statusMsg: String, verbose: VerbosityLevel): Unit = verbose match {
        case Verbose | FullOutput =>
          println(s"$msg [$color $statusMsg ${Console.RESET}]")
        case _ =>
      }

      override def run(verbose: VerbosityLevel = NoOutput): TaskResult[Boolean] = {
        val callRes = super.run(verbose)

        val callSuccess = callRes.res match {
          case Success(true) => true
          case _ => false
        }

        val output = callRes.out.mkString(" ")

        val respRes = checkResponseFun(output)

        val statusRes = checkStatusFun(output)

        val resultSuccess = callSuccess && respRes && statusRes

        val msg = mkCommandLog(host.toString(), verbose)

        if (resultSuccess) {
          printCommandLog(msg, Console.GREEN, "ok", verbose)
          TaskResult(Success(true), Nil, Nil)
        } else {
          printCommandLog(msg, Console.RED, "failed", verbose)
          TaskResult(Failure(new TaskExecutionError(List("Check function failed."))), Nil, Nil)
        }
      }
    }
  }

  override def description: String = "make get request"

  private def printTaskProgress(verbose: VerbosityLevel): Unit = verbose match {
    case Verbose | FullOutput =>
      val h = if (hosts.hosts.nonEmpty) {
        "(and " + (hosts.hosts.size - 1) + " other hosts)"
      } else {
        ""
      }

      val withPar = if(usingPar) {
        s"${Console.GREEN}!!${Console.RESET}"
      } else {
        ""
      }

      println(s"[ ${Console.YELLOW}*${Console.RESET} $withPar] $description on ${hosts.hosts.head.toString()} $h")
    case _ =>
  }

  override def run(verbose: VerbosityLevel): TaskResult[Boolean] = {
    printTaskProgress(verbose)

    val tasksFold = if (usingPar) {
      new TaskM[Boolean] {
        override def run(verbose: VerbosityLevel = NoOutput): TaskResult[Boolean] = {
          import scala.concurrent.ExecutionContext.Implicits.global

          val tasksF = tasks
            .map(t => () => Future {
            t.run(verbose)
          })

          val tasksFRes = Future.sequence(tasksF.map(_()))

          val result = Await.result(tasksFRes, timeout)

          val resultSuccess = result.map(_.res.isSuccess).forall(identity)

          val resultOut = result.
            filter(_.res.isSuccess).
            map(_.out).
            foldLeft(List.empty[String])((acc, out) => acc ++ out)

          val resultErr = result.
            filter(_.res.isSuccess).
            map(_.err).
            foldLeft(List.empty[String])((acc, err) => acc ++ err)

          if (resultSuccess) {
            TaskResult(Success(true), resultOut, resultErr)
          } else {
            TaskResult(Failure(new TaskExecutionError(resultErr)), resultOut, resultErr)
          }
        }
      }
    } else {
      tasks.foldLeft[TaskM[Boolean]](EmptyTask)((acc, t) => acc flatMap(_ => t))
    }

    val result = tasksFold.run(verbose)

    verbose match {
      case Verbose | FullOutput => println("--------------------------------------------------------------")
      case _ =>
    }

    result
  }

  override def sudo: GetRequest = this.copy(usingSudo = true)
  override def par: GetRequest = this.copy(usingPar = true)
}

object GetRequest {
  final val DefaultPort = 8080

  def prepareParams(host: Host, path: String, port: Int): List[String] =
    List("--trace", "-", "-X", "GET") ++ List(s"${host.toString()}:$port$path")
}

/**
 * Install a debian package.
 *
 * @param hosts target hosts.
 * @param packFile path to debian package on target hosts.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel execution needed.
 * @param exec path to dpkg executable.
 * @param user user.
 */
case class InstallDeb(hosts: Hosts, packFile: String, usingSudo: Boolean = false,
                      usingPar: Boolean = false, exec: String = "/usr/bin/dpkg")(implicit user: User)
  extends GenericTask("dpkg", "install debian package", hosts, exec, List("-i", packFile),
    usingSudo, usingPar, taskRepr = s"install debian package '$packFile'") with UsingSudo[InstallDeb] with UsingParallelExecution[InstallDeb] {

  override def sudo: InstallDeb = this.copy(usingSudo = true)
  override def par: InstallDeb = this.copy(usingPar = true)
}

/**
 * Starts a service.
 *
 * @param hosts target hosts.
 * @param service service name.
 * @param usingSudo true, if sudo eeded.
 * @param usingPar true, if parallel execution needed.
 * @param exec path to init.d.
 * @param user user.
 */
case class StartService(hosts: Hosts, service: String, usingSudo: Boolean = false,
                      usingPar: Boolean = false, exec: String = "/etc/init.d/")(implicit user: User)
  extends GenericTask("service", "start service", hosts, s"$exec$service", List("start"),
    usingSudo, usingPar, taskRepr = s"start service '$service'") with UsingSudo[StartService] with UsingParallelExecution[StartService] {

  override def sudo: StartService = this.copy(usingSudo = true)
  override def par: StartService = this.copy(usingPar = true)
}

/**
 * Stops a service.
 *
 * @param hosts target hosts.
 * @param service service name.
 * @param usingSudo true, if sudo eeded.
 * @param usingPar true, if parallel execution needed.
 * @param exec path to init.d.
 * @param user user.
 */
case class StopService(hosts: Hosts, service: String, usingSudo: Boolean = false,
                        usingPar: Boolean = false, exec: String = "/etc/init.d/")(implicit user: User)
  extends GenericTask("service", "stop service", hosts, s"$exec$service", List("stop"),
    usingSudo, usingPar, cmd = Stop, taskRepr = s"stop service '$service'")
  with UsingSudo[StopService] with UsingParallelExecution[StopService] {

  override def sudo: StopService = this.copy(usingSudo = true)
  override def par: StopService = this.copy(usingPar = true)
}
