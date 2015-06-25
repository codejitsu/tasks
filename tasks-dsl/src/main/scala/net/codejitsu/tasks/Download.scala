// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import java.net.URL

import net.codejitsu.tasks.dsl.{UsingParallelExecution, UsingSudo, User, Hosts}

/**
 * Download url.
 *
 * @param hosts hosts.
 * @param url url to download.
 * @param destinationPath destination path on hosts.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel execution required.
 * @param user user.
 */
case class Download(hosts: Hosts, url: URL, destinationPath: String, usingSudo: Boolean = false,
                    usingPar: Boolean = false, exec: String = "/usr/bin/wget")(implicit user: User)
  extends GenericTask("wget", "download url", hosts, exec, List(url.toString, "-P", destinationPath),
    usingSudo, usingPar, taskRepr = s"download url '${url.toString}'") with UsingSudo[Download] with UsingParallelExecution[Download] {

  override def sudo: Download = this.copy(usingSudo = true)
  override def par: Download = this.copy(usingPar = true)
}
