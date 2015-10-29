// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import java.net.URL

import net.codejitsu.tasks.dsl._
import net.codejitsu.tasks.dsl.Tasks._

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

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
        Sudo ~ Exec(exec, GetRequest.prepareParams(host, path, port))
      } else{
        Exec(exec, GetRequest.prepareParams(host, path, port))
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

      override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = {
        val callRes = super.run(verbose, input)

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

  override def run(verbose: VerbosityLevel, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = {
    printTaskProgress(verbose)

    val tasksFold = if (usingPar) {
      new TaskM[Boolean] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = {
          import scala.concurrent.ExecutionContext.Implicits.global

          val tasksF = tasks
            .map(t => () => Future {
            t.run(verbose, input)
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

    val result = tasksFold.run(verbose, input)

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

  def prepareParams(host: HostLike, path: String, port: Int): List[String] =
    List("--trace", "-", "-X", "GET") ++ List(s"${host.toString()}:$port$path")

  def apply[S <: Stage](url: String, port: Int)(implicit user: User, stage: S, rights: S Allow GetRequest[S]): GetRequest[S] = {
    val u = new URL(url)

    GetRequest[S](u.getHost.h, u.getPath + "?" + u.getQuery, port = port)
  }
}
