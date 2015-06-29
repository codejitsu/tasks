// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Move file / dir task.
 *
 * @param hosts target hosts
 * @param source source file to move (rename)
 * @param destination destination file/dir
 * @param params command flags
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
class Mv[S <: Stage](hosts: Hosts, source: String, destination: String, params: List[String] = Nil,
         usingSudo: Boolean = false, usingPar: Boolean = false,
         exec: String = "/bin/mv")(implicit user: User, stage: S, rights: S Allow Mv[S])
  extends GenericTask("mv", "move file(s)", hosts, exec, params ::: List(source, destination),
    usingSudo, usingPar, taskRepr = s"move file(s) '${source}' -> '${destination}'") with UsingSudo[Mv[S]] with UsingParallelExecution[Mv[S]] {

  override def sudo: Mv[S] = Mv[S](hosts, source, destination, params, true, usingPar, exec)
  override def par: Mv[S] = Mv[S](hosts, source, destination, params, usingSudo, true, exec)
}

object Mv {
  // scalastyle:off
  def apply[S <: Stage](hosts: Hosts, source: String, destination: String,
            params: List[String] = Nil, usingSudo: Boolean = false,
            usingPar: Boolean = false, exec: String = "/bin/mv")(implicit user: User, stage: S, rights: S Allow Mv[S]): Mv[S] =
    new Mv[S](hosts, source, destination, params, usingSudo, usingPar, exec)(user, stage, rights)
  // scalastyle:on
}
