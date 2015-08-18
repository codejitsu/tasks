// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Echo task.
 *
 * @param hosts target hosts
 * @param text text to display
 * @param target target file
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
final case class Echo[S <: Stage](hosts: Hosts,
                                  text: String = "",
                                  target: Option[String] = None,
                                  usingSudo: Boolean = false,
                                  usingPar: Boolean = false,
                                  exec: String = "/bin/echo",
                                  params: List[String] = if (OS.isLinux()) List("-e") else Nil)(implicit user: User, stage: S, rights: S Allow Echo[S])
  extends GenericTask("echo", "display a line of text", hosts, exec, params ++ List(text) ++ Echo.checkTarget(target),
    usingSudo, usingPar, taskRepr = s"display text '${text}'") with UsingSudo[Echo[S]] with UsingParallelExecution[Echo[S]] {
  //TODO truncate text to N characters

  override def sudo: Echo[S] = copy[S](usingSudo = true)
  override def par: Echo[S] = copy[S](usingPar = true)
}

object Echo {
  private def checkTarget(target: Option[String]): List[String] = target match {
    case Some(file) => List(file)
    case _ => Nil
  }
}