// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl.Tasks._
import net.codejitsu.tasks.dsl._

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

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
final case class PostRequest[S <: Stage](hosts: Hosts, path: String, data: String, headers: List[String] = Nil,
                       checkResponseFun: (String => Boolean) = _ => true,
                       checkStatusFun: (String => Boolean) = _ => true,
                       port: Int = PostRequest.DefaultPort,
                       usingSudo: Boolean = false, usingPar: Boolean = false,
                       exec: String = "/usr/bin/curl")(implicit user: User, stage: S, rights: S Allow PostRequest[S])
  extends TaskM[Boolean] with UsingSudo[PostRequest[S]] with UsingParallelExecution[PostRequest[S]] {

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
          TaskResult(Failure[Boolean](new TaskExecutionError(List("Check function failed."))), Nil, Nil)
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
            TaskResult(Failure[Boolean](new TaskExecutionError(resultErr)), resultOut, resultErr)
          }
        }
      }
    } else {
      tasks.foldLeft[TaskM[Boolean]](SuccessfulTask)((acc, t) => acc flatMap(_ => t))
    }

    val result = tasksFold.run(verbose)

    verbose match {
      case Verbose | FullOutput => println("--------------------------------------------------------------")
      case _ =>
    }

    result
  }

  override def sudo: PostRequest[S] = copy[S](usingSudo = true)
  override def par: PostRequest[S] = copy[S](usingPar = true)
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
final case class GetRequest[S <: Stage](hosts: Hosts, path: String,
                      checkResponseFun: (String => Boolean) = _ => true,
                      checkStatusFun: (String => Boolean) = _ => true,
                      port: Int = GetRequest.DefaultPort,
                      usingSudo: Boolean = false, usingPar: Boolean = false,
                      exec: String = "/usr/bin/curl")(implicit user: User, stage: S, rights: S Allow GetRequest[S])
  extends TaskM[Boolean] with UsingSudo[GetRequest[S]] with UsingParallelExecution[GetRequest[S]] {

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
          TaskResult(Failure[Boolean](new TaskExecutionError(List("Check function failed."))), Nil, Nil)
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
            TaskResult(Failure[Boolean](new TaskExecutionError(resultErr)), resultOut, resultErr)
          }
        }
      }
    } else {
      tasks.foldLeft[TaskM[Boolean]](SuccessfulTask)((acc, t) => acc flatMap(_ => t))
    }

    val result = tasksFold.run(verbose)

    verbose match {
      case Verbose | FullOutput => println("--------------------------------------------------------------")
      case _ =>
    }

    result
  }

  override def sudo: GetRequest[S] = copy[S](usingSudo = true)
  override def par: GetRequest[S] = copy[S](usingPar = true)
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
final case class InstallDeb[S <: Stage](hosts: Hosts, packFile: String, usingSudo: Boolean = false,
                      usingPar: Boolean = false,
                      exec: String = "/usr/bin/dpkg")(implicit user: User, stage: S, rights: S Allow InstallDeb[S])
  extends GenericTask("dpkg", "install debian package", hosts, exec, List("-i", packFile),
    usingSudo, usingPar, taskRepr = s"install debian package '$packFile'") with UsingSudo[InstallDeb[S]] with UsingParallelExecution[InstallDeb[S]] {

  override def sudo: InstallDeb[S] = copy[S](usingSudo = true)
  override def par: InstallDeb[S] = copy[S](usingPar = true)
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
final case class StartService[S <: Stage](hosts: Hosts, service: String, usingSudo: Boolean = false,
                      usingPar: Boolean = false,
                      exec: String = "/etc/init.d/")(implicit user: User, stage: S, rights: S Allow StartService[S])
  extends GenericTask("service", "start service", hosts, s"$exec$service", List("start"),
    usingSudo, usingPar, taskRepr = s"start service '$service'") with UsingSudo[StartService[S]] with UsingParallelExecution[StartService[S]] {

  override def sudo: StartService[S] = copy[S](usingSudo = true)
  override def par: StartService[S] = copy[S](usingPar = true)
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
final case class StopService[S <: Stage](hosts: Hosts, service: String, usingSudo: Boolean = false,
                        usingPar: Boolean = false,
                        exec: String = "/etc/init.d/")(implicit user: User, stage: S, rights: S Allow StopService[S])
  extends GenericTask("service", "stop service", hosts, s"$exec$service", List("stop"),
    usingSudo, usingPar, cmd = Stop, taskRepr = s"stop service '$service'")
  with UsingSudo[StopService[S]] with UsingParallelExecution[StopService[S]] {

  override def sudo: StopService[S] = copy[S](usingSudo = true)
  override def par: StopService[S] = copy[S](usingPar = true)
}
