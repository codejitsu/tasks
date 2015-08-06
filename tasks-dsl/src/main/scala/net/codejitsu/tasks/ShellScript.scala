// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Run shell scripts.
 */
final case class ShellScript[S <: Stage](hosts: Hosts, script: String, params: List[String] = List.empty[String],
                                         usingSudo: Boolean = false, usingPar: Boolean = false,
                                         shell: String = "/bin/sh")(implicit user: User, stage: S, rights: S Allow StartService[S])
  extends GenericTask("script", "run shell script", hosts, s"$shell $script", params,
    usingSudo, usingPar, taskRepr = s"run shell script '$script'") with UsingSudo[ShellScript[S]] with UsingParallelExecution[ShellScript[S]] {

  override def sudo: ShellScript[S] = copy[S](usingSudo = true)
  override def par: ShellScript[S] = copy[S](usingPar = true)
}
