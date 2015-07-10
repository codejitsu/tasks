// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Starts a service.
 *
 * @param hosts target hosts.
 * @param service service name.
 * @param usingSudo true, if sudo eeded.
 * @param usingPar true, if parallel execution needed.
 * @param exec path to init.d.
 * @param user user.
 */
final case class StartService[S <: Stage](hosts: Hosts, service: String, usingSudo: Boolean = false,
                                          usingPar: Boolean = false,
                                          exec: String = "/etc/init.d/")(implicit user: User, stage: S, rights: S Allow StartService[S])
  extends GenericTask("service", "start service", hosts, s"$exec$service", List("start"),
    usingSudo, usingPar, taskRepr = s"start service '$service'") with UsingSudo[StartService[S]] with UsingParallelExecution[StartService[S]] {

  override def sudo: StartService[S] = copy[S](usingSudo = true)
  override def par: StartService[S] = copy[S](usingPar = true)
}

/**
 * Stops a service.
 *
 * @param hosts target hosts.
 * @param service service name.
 * @param usingSudo true, if sudo eeded.
 * @param usingPar true, if parallel execution needed.
 * @param exec path to init.d.
 * @param user user.
 */
final case class StopService[S <: Stage](hosts: Hosts, service: String, usingSudo: Boolean = false,
                                         usingPar: Boolean = false,
                                         exec: String = "/etc/init.d/")(implicit user: User, stage: S, rights: S Allow StopService[S])
  extends GenericTask("service", "stop service", hosts, s"$exec$service", List("stop"),
    usingSudo, usingPar, cmd = Stop, taskRepr = s"stop service '$service'")
  with UsingSudo[StopService[S]] with UsingParallelExecution[StopService[S]] {

  override def sudo: StopService[S] = copy[S](usingSudo = true)
  override def par: StopService[S] = copy[S](usingPar = true)
}

