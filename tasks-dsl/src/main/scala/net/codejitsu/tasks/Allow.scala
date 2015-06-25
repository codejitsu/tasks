// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl.TaskM

import scala.annotation.implicitNotFound

trait Stage
trait Dev extends Stage
trait Production extends Stage

@implicitNotFound("Stage '${S}' is not allowed to run task '${T}'.")
class Allow[S <: Stage, T <: TaskM[Boolean]]

object Allow {
  implicit val devAllowAll: Dev Allow TaskM[Boolean] = new Allow[Dev, TaskM[Boolean]]

  // scalastyle:off
  implicit val prodAllowNothing: Production Allow TaskM[Boolean] = null
  // scalastyle:on
}
