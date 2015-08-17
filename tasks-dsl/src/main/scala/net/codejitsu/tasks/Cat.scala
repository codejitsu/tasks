package net.codejitsu.tasks

import net.codejitsu.tasks.dsl._

/**
 * Cat task.
 *
 * @param hosts target hosts
 * @param target target file
 * @param usingSudo true, if task have to be started with sudo
 * @param usingPar true, if parallel execution required.
 * @param user user
 */
final case class Cat[S <: Stage](hosts: Hosts,
                                 target: Option[String] = None,
                                 usingSudo: Boolean = false,
                                 usingPar: Boolean = false,
                                 exec: String = "/bin/cat")(implicit user: User, stage: S, rights: S Allow Cat[S])
  extends GenericTask("cat", "concatenate files and print on the standard output", hosts, exec, Nil,
    usingSudo, usingPar, taskRepr = s"display file '${target}'") with UsingSudo[Cat[S]] with UsingParallelExecution[Cat[S]] {
  //TODO truncate text to N characters

  override def sudo: Cat[S] = copy[S](usingSudo = true)
  override def par: Cat[S] = copy[S](usingPar = true)
}
