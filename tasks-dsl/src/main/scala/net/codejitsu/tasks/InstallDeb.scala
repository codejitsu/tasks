// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Install a debian package.
 *
 * @param hosts target hosts.
 * @param packFile path to debian package on target hosts.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel execution needed.
 * @param exec path to dpkg executable.
 * @param user user.
 */
final case class InstallDeb[S <: Stage](hosts: Hosts, packFile: String, usingSudo: Boolean = false,
                                        usingPar: Boolean = false,
                                        exec: String = "/usr/bin/dpkg")(implicit user: User, stage: S, rights: S Allow InstallDeb[S])
  extends GenericTask("dpkg", "install debian package", hosts, exec, List("-i", packFile),
    usingSudo, usingPar, taskRepr = s"install debian package '$packFile'") with UsingSudo[InstallDeb[S]] with UsingParallelExecution[InstallDeb[S]] {

  override def sudo: InstallDeb[S] = copy[S](usingSudo = true)
  override def par: InstallDeb[S] = copy[S](usingPar = true)
}

