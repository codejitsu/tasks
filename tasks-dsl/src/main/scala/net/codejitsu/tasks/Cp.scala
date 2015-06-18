// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl.{UsingParallelExecution, UsingSudo, User, Hosts}

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
class Cp(hosts: Hosts, source: String, destination: String,
         params: List[String] = Nil, usingSudo: Boolean = false,
         usingPar: Boolean = false, exec: String = "/usr/bin/rsync")(implicit user: User)
  extends GenericTask("rsync", "copy file(s)", hosts, exec, params ::: List(source, destination),
    usingSudo, usingPar, taskRepr = s"copy file(s) '${source}' -> '${destination}'") with UsingSudo[Cp] with UsingParallelExecution[Cp] {

  override def sudo: Cp = Cp(hosts, source, destination, params, true, usingPar, exec)
  override def par: Cp = Cp(hosts, source, destination, params, usingSudo, true, exec)
}

object Cp {
  def apply(hosts: Hosts, source: String, destination: String, params: List[String] = Nil,
            sudo: Boolean = false, parallel: Boolean = false, exec: String = "/usr/bin/rsync")(implicit user: User): Cp =
    new Cp(hosts, source, destination, params, sudo, parallel, exec)(user)

  def apply(hosts: Hosts, source: String, destination: String)(implicit user: User): Cp = Cp(hosts, source, destination, Nil)
}