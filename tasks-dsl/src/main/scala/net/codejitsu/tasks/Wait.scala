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
