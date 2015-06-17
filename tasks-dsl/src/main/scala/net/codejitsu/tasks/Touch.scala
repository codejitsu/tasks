// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl.{UsingParallelExecution, UsingSudo, User, Hosts}

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

