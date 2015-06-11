// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

/**
 * Group of hosts.
 */
case class Hosts(hosts: collection.immutable.Seq[Host]) {
  def ~ (part: String): Hosts = {
    val appended = hosts.map(h => Host(h.parts :+ HostPart(part)))

    Hosts(appended)
  }

  def ~ (parts: Product): Hosts = {
    val vals = for {
      i <- 0 until parts.productArity
    } yield parts.productElement(i).toString

    val all = vals map (v => this ~ v)

    val together = all.foldLeft(collection.immutable.Seq.empty[Host])((hseq, hosts) => hosts.hosts ++ hseq)

    Hosts(together)
  }

  def ~[T](parts: IndexedSeq[T]): Hosts = {
    val all = for {
      x <- hosts
      y <- parts
    } yield Host(x.parts :+ HostPart(y.toString))

    Hosts(all.toList)
  }

  def | (part: String): Hosts = {
    val appended = hosts.map(h => Host(h.parts.init :+ HostPart(h.parts.last.part + part)))

    Hosts(appended)
  }

  def | (parts: Product): Hosts = {
    val vals = for {
      i <- 0 until parts.productArity
    } yield parts.productElement(i).toString

    val all = vals map (v => this | v)

    val together = all.foldLeft(collection.immutable.Seq.empty[Host])((hseq, hosts) => hosts.hosts ++ hseq)

    Hosts(together)
  }

  def | (parts: Host): Hosts = {
    val all = Seq(this | parts.toString())

    val together = all.foldLeft(collection.immutable.Seq.empty[Host])((hseq, hosts) => hosts.hosts ++ hseq)

    Hosts(together)
  }

  def |[T](parts: IndexedSeq[T]): Hosts = {
    val all = for {
      x <- hosts
      y <- parts
    } yield Host(x.parts.init :+ HostPart(x.parts.last.part + y.toString))

    Hosts(all.toList)
  }
}
