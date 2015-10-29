// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Head task.
 *
 * @param hosts target hosts
 * @param target target file
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
final case class Head[S <: Stage](hosts: Hosts,
                                 target: Option[String] = None,
                                 usingSudo: Boolean = false,
                                 usingPar: Boolean = false,
                                 exec: String = "/usr/bin/head",
                                 verbose: VerbosityLevel = NoOutput)(implicit user: User, stage: S, rights: S Allow Head[S])
  extends GenericTask("head", "the first part of file", hosts, exec, Nil,
    usingSudo, usingPar, taskRepr = s"display file ${Head.getFileDescription(target)}",
    pipeCmd = Head.pipeCmd(), verbose = Option(verbose)) with UsingSudo[Head[S]] with UsingParallelExecution[Head[S]] {

  override def sudo: Head[S] = copy[S](usingSudo = true)
  override def par: Head[S] = copy[S](usingPar = true)
}

object Head {
  def getFileDescription(target: Option[String]): String =
    target.fold("")(f => s"'$f'")

  def pipeCmd(): Seq[String] = Seq("/usr/bin/head")
}
