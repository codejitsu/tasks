// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl.{UsingParallelExecution, UsingSudo, User, Hosts}

/**
 * mkdir-task
 */
class Mkdir(hosts: Hosts, target: String, usingSudo: Boolean = false, usingPar: Boolean = false, exec: String = "/bin/mkdir")(implicit user: User)
  extends GenericTask("mkdir", "create directory", hosts, exec, List("-p", target),
    usingSudo, usingPar, taskRepr = s"create directory '$target'") with UsingSudo[Mkdir] with UsingParallelExecution[Mkdir] {

  override def sudo: Mkdir = Mkdir(hosts, target, true, usingPar, exec)
  override def par: Mkdir = Mkdir(hosts, target, usingSudo, true, exec)
}

object Mkdir {
  def apply(hosts: Hosts, target: String, usingSudo: Boolean = false, usingPar: Boolean = false, exec: String = "/bin/mkdir")(implicit user: User): Mkdir =
    new Mkdir(hosts, target, usingSudo, usingPar, exec)(user)
}
