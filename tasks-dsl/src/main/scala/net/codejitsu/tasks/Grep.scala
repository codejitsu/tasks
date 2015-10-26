// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Grep task.
 *
 * @param hosts target hosts
 * @param target target file
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
final case class Grep[S <: Stage](hosts: Hosts,
                                 target: Option[String] = None,
                                 usingSudo: Boolean = false,
                                 usingPar: Boolean = false,
                                 exec: String = "/usr/bin/grep",
                                 params: List[String] = Nil,
                                 pattern: Option[String] = None)(implicit user: User, stage: S, rights: S Allow Grep[S])
  extends GenericTask("grep", "searches any given input files, selecting lines that match one or more patterns", hosts, exec,
    Grep.makeCommandLine(params, pattern, target),
    usingSudo, usingPar, taskRepr = s"grep file '${target}' for patterns") with UsingSudo[Grep[S]] with UsingParallelExecution[Grep[S]] {

  override def sudo: Grep[S] = copy[S](usingSudo = true)
  override def par: Grep[S] = copy[S](usingPar = true)
}

object Grep {
  def makeCommandLine(params: List[String], pattern: Option[String], target: Option[String]): List[String] = {
    val pat = pattern.fold("")(p => p)
    val tar = target.fold("")(t => t)

    params ++ List(pat, tar)
  }
}