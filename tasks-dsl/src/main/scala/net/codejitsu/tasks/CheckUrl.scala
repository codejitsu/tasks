// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

/**
 * Check url with predicate.
 *
 * @param hosts hosts to call over http.
 * @param path app context.
 * @param port app port.
 * @param checkFun predicate on response text.
 * @param usingPar true, if parallel execution required.
 */
final case class CheckUrl[S <: Stage](hosts: Hosts, path: String, port: Int = CheckUrl.DefaultPort,
                    checkFun: (String => Boolean) = _ => true, usingPar: Boolean = false)(implicit val stage: S, rights: S Allow CheckUrl[S])
  extends TaskM[Boolean] with UsingParallelExecution[CheckUrl[S]] {

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
          TaskResult(Failure[Boolean](new TaskExecutionError(List("Check function failed."))), Nil, Nil)
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

  override def par: CheckUrl[S] = copy[S](usingPar = true)
}

object CheckUrl {
  final val DefaultPort = 8080
}
