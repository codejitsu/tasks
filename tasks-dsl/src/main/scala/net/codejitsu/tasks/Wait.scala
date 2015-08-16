// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import java.util.concurrent.TimeoutException

import net.codejitsu.tasks.dsl._
import net.codejitsu.tasks.dsl.Tasks._

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration
import scala.util.{Success, Failure}

/**
 * Wait task.
 *
 * @param d duration.
 */
final case class Wait[S <: Stage](d: Duration)(implicit val stage: S, rights: S Allow Wait[S]) extends TaskM[Boolean] {
  override def description: String = "waiting"

  override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] =
    LoggedRun(
      verbose,
      false,
      false,
      Localhost,
      s"$description for ${d.toString}",
      new TaskM[Boolean] {
        override def run(verbose: VerbosityLevel, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = {
          val promise = Promise[Unit]

          val result = try {
            Await.ready(promise.future, d)
            TaskResult[Boolean](Failure[Boolean](new TaskExecutionError(Nil)), Nil, Nil)
          } catch {
            case t: TimeoutException => TaskResult[Boolean](Success(true), Nil, Nil)
            case e: Throwable => TaskResult[Boolean](Failure[Boolean](new TaskExecutionError(List(e.getMessage))), Nil, Nil)
          }

          result
        }
      },
      input
    )(verbose)
}
