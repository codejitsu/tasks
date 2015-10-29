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
                                      destinationPath: Option[String] = None, usingSudo: Boolean = false,
                                      usingPar: Boolean = false,
                                      exec: String = Download.getExec(),
                                      params: List[String] = List("-P"),
                                      verbose: VerbosityLevel = NoOutput)(implicit user: User, stage: S, rights: S Allow Download[S])
  extends GenericTask("wget", "download url", hosts, exec, params ++ List(url.toString) ++ Download.getDestination(destinationPath),
    usingSudo, usingPar, taskRepr = s"download url '${url.toString}'", verbose = Option(verbose)) with UsingSudo[Download[S]] with UsingParallelExecution[Download[S]] {

  override def sudo: Download[S] = copy[S](usingSudo = true)
  override def par: Download[S] = copy[S](usingPar = true)
}

object Download {
  def getExec(): String = OS.getCurrentOs() match {
    case Linux => "/usr/bin/wget"
    case MacOS => "/usr/local/bin/wget"
    case _ => throw new IllegalArgumentException("Not supported OS")
  }

  def getDestination(destinationPath: Option[String]): List[String] =
    destinationPath.fold(List.empty[String])(d => List(d))
}
