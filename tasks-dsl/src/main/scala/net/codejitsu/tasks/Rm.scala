// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl.{UsingParallelExecution, UsingSudo, User, Hosts}

/**
 * Remove file / dir task.
 *
 * @param hosts target hosts
 * @param target file/dir to remove
 * @param params command flags
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
class Rm(hosts: Hosts, target: String, params: List[String] = List("-r"),
         usingSudo: Boolean = false, usingPar: Boolean = false,
         exec: String = "/bin/rm", desc: String = "remove file(s)")(implicit user: User)
  extends GenericTask("rm", desc, hosts, exec, params ::: List(target),
    usingSudo, usingPar, taskRepr = s"$desc '$target'") with UsingSudo[Rm] with UsingParallelExecution[Rm] {

  override def sudo: Rm = Rm(hosts, target, params, true, usingPar, exec)
  override def par: Rm = Rm(hosts, target, params, usingSudo, true, exec)
}

object Rm {
  def apply(hosts: Hosts, target: String, params: List[String] = List("-r"), usingSudo: Boolean = false,
            usingPar: Boolean = false, exec: String = "/bin/rm")(implicit user: User): Rm =
    new Rm(hosts, target, params, usingSudo, usingPar, exec)(user)
}

/**
 * Remove file / dir if exists task.
 *
 * @param hosts target hosts
 * @param target file/dir to remove
 * @param params command flags
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
class RmIfExists(hosts: Hosts, target: String, params: List[String] = List("-rf"),
                 usingSudo: Boolean = false, usingPar: Boolean = false,
                 exec: String = "/bin/rm", desc: String = "remove file(s) (if exists)")(implicit user: User)
  extends GenericTask("rm", desc, hosts, exec, params ::: List(target),
    usingSudo, usingPar, taskRepr = s"$desc '$target'") with UsingSudo[RmIfExists] with UsingParallelExecution[RmIfExists] {

  override def sudo: RmIfExists = RmIfExists(hosts, target, params, true, usingPar, exec)
  override def par: RmIfExists = RmIfExists(hosts, target, params, usingSudo, true, exec)
}

object RmIfExists {
  def apply(hosts: Hosts, target: String, params: List[String] = List("-rf"), usingSudo: Boolean = false,
            usingPar: Boolean = false, exec: String = "/bin/rm")(implicit user: User): RmIfExists =
    new RmIfExists(hosts, target, params, usingSudo, usingPar, exec)(user)
}
