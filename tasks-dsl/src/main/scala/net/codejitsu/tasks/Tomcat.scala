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
case class StopTomcat(hosts: Hosts, usingSudo: Boolean = false,
                      usingPar: Boolean = false, exec: String = "/etc/init.d/tomcat7")(implicit user: User)
  extends GenericTask("tomcat", "stop tomcat service", hosts, exec, List("stop"),
    usingSudo, usingPar, Stop, "stop tomcat service") with UsingSudo[StopTomcat] with UsingParallelExecution[StopTomcat] {

  override def sudo: StopTomcat = this.copy(usingSudo = true)
  override def par: StopTomcat = this.copy(usingPar = true)
}

/**
 * Start tomcat service.
 *
 * @param hosts hosts.
 * @param usingSudo true, if sudo needed.
 * @param usingPar true, if parallel execution required.
 * @param user user.
 */
case class StartTomcat(hosts: Hosts, usingSudo: Boolean = false,
                       usingPar: Boolean = false, exec: String = "/etc/init.d/tomcat7")(implicit user: User)
  extends GenericTask("tomcat", "start tomcat service", hosts, exec, List("start"),
    usingSudo, usingPar, taskRepr = "start tomcat service") with UsingSudo[StartTomcat] with UsingParallelExecution[StartTomcat] {

  override def sudo: StartTomcat = this.copy(usingSudo = true)
  override def par: StartTomcat = this.copy(usingPar = true)
}
