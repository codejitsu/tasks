// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

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
class Rm[S <: Stage](hosts: Hosts, target: String, params: List[String] = List("-r"),
         usingSudo: Boolean = false, usingPar: Boolean = false,
         exec: String = "/bin/rm", desc: String = "remove file(s)")(implicit user: User, stage: S, rights: S Allow Rm[S])
  extends GenericTask("rm", desc, hosts, exec, params ::: List(target),
    usingSudo, usingPar, taskRepr = s"$desc '$target'") with UsingSudo[Rm[S]] with UsingParallelExecution[Rm[S]] {

  override def sudo: Rm[S] = Rm[S](hosts, target, params, true, usingPar, exec)
  override def par: Rm[S] = Rm[S](hosts, target, params, usingSudo, true, exec)
}

object Rm {
  def apply[S <: Stage](hosts: Hosts, target: String, params: List[String] = List("-r"), usingSudo: Boolean = false,
            usingPar: Boolean = false, exec: String = "/bin/rm")(implicit user: User, stage: S, rights: S Allow Rm[S]): Rm[S] =
    new Rm[S](hosts, target, params, usingSudo, usingPar, exec)(user, stage, rights)
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
class RmIfExists[S <: Stage](hosts: Hosts, target: String, params: List[String] = List("-rf"),
                 usingSudo: Boolean = false, usingPar: Boolean = false,
                 exec: String = "/bin/rm", desc: String = "remove file(s) (if exists)")(implicit user: User, stage: S, rights: S Allow RmIfExists[S])
  extends GenericTask("rm", desc, hosts, exec, params ::: List(target),
    usingSudo, usingPar, taskRepr = s"$desc '$target'") with UsingSudo[RmIfExists[S]] with UsingParallelExecution[RmIfExists[S]] {

  override def sudo: RmIfExists[S] = RmIfExists[S](hosts, target, params, true, usingPar, exec)
  override def par: RmIfExists[S] = RmIfExists[S](hosts, target, params, usingSudo, true, exec)
}

object RmIfExists {
  def apply[S <: Stage](hosts: Hosts, target: String, params: List[String] = List("-rf"), usingSudo: Boolean = false,
            usingPar: Boolean = false, exec: String = "/bin/rm")(implicit user: User, stage: S, rights: S Allow RmIfExists[S]): RmIfExists[S] =
    new RmIfExists[S](hosts, target, params, usingSudo, usingPar, exec)(user, stage, rights)
}
