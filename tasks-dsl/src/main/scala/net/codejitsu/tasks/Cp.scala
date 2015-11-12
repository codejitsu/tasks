// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Copy file / dir task.
 *
 * @param hosts target hosts
 * @param source source object
 * @param destination destination object
 * @param params task flags
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
class Cp[S <: Stage](hosts: Hosts, source: String, destination: String,
         params: List[String] = Nil, usingSudo: Boolean = false,
         usingPar: Boolean = false, exec: String = "/bin/cp")(implicit user: User, stage: S, rights: S Allow Cp[S])
  extends GenericTask("cp", "copy file(s)", hosts, exec, params ::: List(source, destination),
    usingSudo, usingPar, taskRepr = s"copy file(s) '${source}' -> '${destination}'") with UsingSudo[Cp[S]] with UsingParallelExecution[Cp[S]] {

  override def sudo: Cp[S] = Cp[S](hosts, source, destination, params, true, usingPar, exec)
  override def par: Cp[S] = Cp[S](hosts, source, destination, params, usingSudo, true, exec)
}

object Cp {
  // scalastyle:off
  def apply[S <: Stage](hosts: Hosts,
                        source: String,
                        destination: String,
                        params: List[String] = Nil,
                        sudo: Boolean = false,
                        parallel: Boolean = false,
                        exec: String = "/bin/cp")(implicit user: User,
                                                         stage: S, rights: S Allow Cp[S]): Cp[S] =
    new Cp[S](hosts, source, destination, params, sudo, parallel, exec)(user, stage, rights)
  // scalastyle:on

  def apply[S <: Stage](hosts: Hosts,
                        source: String,
                        destination: String)(implicit user: User,
                                             stage: S, rights: S Allow Cp[S]): Cp[S] = Cp[S](hosts, source, destination, Nil)
}

