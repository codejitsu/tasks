// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import java.net.URL

import net.codejitsu.tasks.dsl._

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
final case class Download[S <: Stage](hosts: Hosts, url: URL,
                                      destinationPath: String, usingSudo: Boolean = false,
                                      usingPar: Boolean = false,
                                      exec: String = Download.getExec(),
                                      params: List[String] = List("-P"))(implicit user: User, stage: S, rights: S Allow Download[S])
  extends GenericTask("wget", "download url", hosts, exec, params ++ List(url.toString) ++ List(destinationPath),
    usingSudo, usingPar, taskRepr = s"download url '${url.toString}'") with UsingSudo[Download[S]] with UsingParallelExecution[Download[S]] {

  override def sudo: Download[S] = copy[S](usingSudo = true)
  override def par: Download[S] = copy[S](usingPar = true)
}

object Download {
  def getExec(): String = OS.getCurrentOs() match {
    case Linux => "/usr/bin/wget"
    case MacOS => "/usr/local/bin/wget"
    case _ => throw new IllegalArgumentException("Not supported OS")
  }
}
