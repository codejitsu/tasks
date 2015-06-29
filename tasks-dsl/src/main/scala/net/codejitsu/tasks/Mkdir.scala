// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * mkdir-task
 */
class Mkdir[S <: Stage](hosts: Hosts, target: String, usingSudo: Boolean = false, usingPar: Boolean = false,
                        exec: String = "/bin/mkdir")(implicit user: User, stage: S, rights: S Allow Mkdir[S])
  extends GenericTask("mkdir", "create directory", hosts, exec, List("-p", target),
    usingSudo, usingPar, taskRepr = s"create directory '$target'") with UsingSudo[Mkdir[S]] with UsingParallelExecution[Mkdir[S]] {

  override def sudo: Mkdir[S] = Mkdir[S](hosts, target, true, usingPar, exec)
  override def par: Mkdir[S] = Mkdir[S](hosts, target, usingSudo, true, exec)
}

object Mkdir {
  def apply[S <: Stage](hosts: Hosts, target: String, usingSudo: Boolean = false, usingPar: Boolean = false,
                        exec: String = "/bin/mkdir")(implicit user: User, stage: S, rights: S Allow Mkdir[S]): Mkdir[S] =
    new Mkdir[S](hosts, target, usingSudo, usingPar, exec)(user, stage, rights)
}
