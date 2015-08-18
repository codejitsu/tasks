// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Tail task.
 *
 * @param hosts target hosts
 * @param target target file
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
final case class Tail[S <: Stage](hosts: Hosts,
                                 target: Option[String] = None,
                                 usingSudo: Boolean = false,
                                 usingPar: Boolean = false,
                                 exec: String = "/usr/bin/tail",
                                 params: List[String] = Nil)(implicit user: User, stage: S, rights: S Allow Tail[S])
  extends GenericTask("tail", "output the last part of file", hosts, exec, params ++ Tail.checkTarget(target),
    usingSudo, usingPar, taskRepr = s"output the last part of file '${target}'") with UsingSudo[Tail[S]] with UsingParallelExecution[Tail[S]] {
  //TODO truncate text to N characters

  override def sudo: Tail[S] = copy[S](usingSudo = true)
  override def par: Tail[S] = copy[S](usingPar = true)
}

object Tail {
  private def checkTarget(target: Option[String]): List[String] = target match {
    case Some(file) => List(file)
    case _ => Nil
  }
}
