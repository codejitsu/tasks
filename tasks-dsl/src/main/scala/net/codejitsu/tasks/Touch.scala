// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Create file task.
 *
 * @param hosts target hosts
 * @param target target file
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
case class Touch[S <: Stage](hosts: Hosts,
                             target: String,
                             usingSudo: Boolean = false,
                             usingPar: Boolean = false,
                             exec: String = "/usr/bin/touch")(implicit user: User, stage: S, rights: S Allow Touch[S])
  extends GenericTask("touch", "create file", hosts, exec, List(target),
    usingSudo, usingPar, taskRepr = s"create file '$target'") with UsingSudo[Touch[S]] with UsingParallelExecution[Touch[S]] {

  override def sudo: Touch[S] = this.copy(usingSudo = true)
  override def par: Touch[S] = this.copy(usingPar = true)
}

