// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Stop tomcat service.
 *
 * @param hosts hosts.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel execution required.
 * @param user user.
 */
final case class StopTomcat[S <: Stage](hosts: Hosts, usingSudo: Boolean = false,
                      usingPar: Boolean = false,
                      exec: String = "/etc/init.d/tomcat7")(implicit user: User, stage: S, rights: S Allow StopTomcat[S])
  extends GenericTask("tomcat", "stop tomcat service", hosts, exec, List("stop"),
    usingSudo, usingPar, Stop, "stop tomcat service") with UsingSudo[StopTomcat[S]] with UsingParallelExecution[StopTomcat[S]] {

  override def sudo: StopTomcat[S] = copy[S](usingSudo = true)
  override def par: StopTomcat[S] = copy[S](usingPar = true)
}

/**
 * Start tomcat service.
 *
 * @param hosts hosts.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel execution required.
 * @param user user.
 */
final case class StartTomcat[S <: Stage](hosts: Hosts, usingSudo: Boolean = false,
                       usingPar: Boolean = false, exec: String = "/etc/init.d/tomcat7")(implicit user: User, stage: S, rights: S Allow StartTomcat[S])
  extends GenericTask("tomcat", "start tomcat service", hosts, exec, List("start"),
    usingSudo, usingPar, taskRepr = "start tomcat service") with UsingSudo[StartTomcat[S]] with UsingParallelExecution[StartTomcat[S]] {

  override def sudo: StartTomcat[S] = copy[S](usingSudo = true)
  override def par: StartTomcat[S] = copy[S](usingPar = true)
}
