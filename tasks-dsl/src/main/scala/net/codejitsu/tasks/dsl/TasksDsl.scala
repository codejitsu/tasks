// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
 * DSL for tasks scripting.
 */
object Tasks {
  implicit class HostStringOps(val ctx: String) {
    def ~ (part: String): Host = Host(List(HostPart(ctx), HostPart(part)))

    def ~[P](parts: IndexedSeq[P]): Hosts = {
      val all = for {
        y <- parts
      } yield Host(List(HostPart(ctx), HostPart(y.toString)))

      Hosts(all.toList)
    }

    def ~ (parts: Product): Hosts = {
      val flatProduct = for {
        i <- 0 until parts.productArity
      } yield parts.productElement(i).toString

      this ~ flatProduct
    }

    def | (part: String): Host = Host(List(HostPart(ctx + part)))

    def |[P](parts: IndexedSeq[P]): Hosts = {
      val all = for {
        y <- parts
      } yield Host(List(HostPart(ctx + y.toString)))

      Hosts(all.toList)
    }

    def | (parts: Product): Hosts = {
      val flatProduct = for {
        i <- 0 until parts.productArity
      } yield parts.productElement(i).toString

      this | flatProduct
    }

    def | (parts: Hosts): Hosts = {
      val flatProduct = for {
        h <- parts.hosts
      } yield h.toString

      this | flatProduct.toVector
    }

    def on (ps: ProcessStep): Process = Process(ctx, ps.host, ps.proc)

    def on (ps: ProcessSteps): Processes = {
      val p = ps.steps map (s => ctx on s)
      Processes(p)
    }

    def ~>(proc: PartialFunction[Command, CommandLine]): ProcessStep = Host(List(HostPart(ctx))) ~> proc

    def h: Host = Host(List(HostPart(ctx)))
  }

  implicit class HostRangeOps[T](val ctx: IndexedSeq[T]) {
    def ~ (part: String): Hosts = {
      val mapped: collection.immutable.Seq[Host] =
        ctx.map(p => Host(List(HostPart(p.toString), HostPart(part)))).toVector
      Hosts(mapped)
    }

    def ~[P](parts: IndexedSeq[P]): Hosts = {
      val all = for {
        x <- ctx
        y <- parts
      } yield Host(List(HostPart(x.toString), HostPart(y.toString)))

      Hosts(all.toList)
    }

    def ~(parts: Product): Hosts = {
      val flatProduct = for {
        i <- 0 until parts.productArity
      } yield parts.productElement(i).toString

      this ~ flatProduct
    }
  }

  implicit class HostProductOps[P <: Product](val ctx: P) {
    def ~ (part: String): Hosts = {
      val vals = for {
        i <- 0 until ctx.productArity
      } yield ctx.productElement(i).toString

      val mapped: collection.immutable.Seq[Host] =
        vals.map(p => Host(List(HostPart(p), HostPart(part)))).toVector
      Hosts(mapped)
    }

    def ~[T](parts: IndexedSeq[T]): Hosts = {
      val vals = for {
        i <- 0 until ctx.productArity
      } yield ctx.productElement(i).toString

      val all = for {
        x <- vals
        y <- parts
      } yield Host(List(HostPart(x.toString), HostPart(y.toString)))

      Hosts(all.toList)
    }
  }

  implicit class HostOps(val ctx: HostLike) {
    def ~>(proc: PartialFunction[Command, CommandLine]): ProcessStep = ProcessStep(proc, host = ctx)
  }

  implicit class HostsOps(val ctx: Hosts) {
    def ~>(proc: PartialFunction[Command, CommandLine]): ProcessSteps = {
      val steps = ctx.hosts map (h => h ~> proc)
      ProcessSteps(steps)
    }
  }

  implicit class ProcessOps(val ctx: Process) {
    def ! (op: Command)(implicit user: User): TaskM[Boolean] = new ShellTask(ctx, op)
  }

  implicit class ProcessesOps(val ctx: Processes) {
    import scala.concurrent.duration._

    def ! (op: Command)(implicit user: User): TaskM[Boolean] = {
      val tasks = ctx.procs.map(_ ! op)

      tasks.foldLeft[TaskM[Boolean]](SuccessfulTask)((acc, t) => acc flatMap(_ => t))
    }

    def !! (op: Command)(implicit user: User, timeout: Duration): TaskM[Boolean] = {
      new TaskM[Boolean] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = {
          val tasksF = ctx.procs
            .map(_ ! op)
            .map(t => () => Future {
              t.run(verbose, input)
            })

          val tasksFRes = Future.sequence(tasksF.map(_()))

          val result = Await.result(tasksFRes, timeout)

          val resultSuccess = result.map(_.res.isSuccess).forall(identity)

          val resultOut = result.
            map(_.out).
            foldLeft(List.empty[String])((acc, out) => acc ++ out)

          val resultErr = result.
            map(_.err).
            foldLeft(List.empty[String])((acc, err) => acc ++ err)

          if (resultSuccess) {
            TaskResult(Success(true), resultOut, resultErr)
          } else {
            TaskResult(Failure[Boolean](new TaskExecutionError(resultErr)), resultOut, resultErr)
          }
        }
      }
    }
  }

  implicit class PathOps(ctx: String) {
    def / (path: String): String = s"$ctx/$path"
  }

  object Sudo {
    final case class ParHandler() {
      def ~[T <: UsingSudo[T] with UsingParallelExecution[T]](task: T): T = task.sudo.par
    }

    def ~ (exec: Exec): SudoExec = SudoExec(exec.path, exec.params)
    def ~[T <: UsingSudo[T]](task: T): T = task.sudo
    def ~ (p: Par.type): ParHandler = ParHandler()
  }

  object Par {
    final case class SudoHandler() {
      def ~[T <: UsingSudo[T] with UsingParallelExecution[T]](task: T): T = task.par.sudo
    }

    def ~[T <: UsingParallelExecution[T]](task: T): T = task.par
    def ~ (s: Sudo.type): SudoHandler = SudoHandler()
  }

  final case class Clear[S <: Stage](implicit user: User, stage: S, rights: S Allow Clear[S]) extends TaskM[Boolean] {
    self =>

    override def run(verbose: VerbosityLevel, input: Option[TaskResult[_]]): TaskResult[Boolean] = {
      TaskResult(Success(true), Nil, Nil)
    }

    override def flatMap[T](f: Boolean => TaskM[T]): TaskM[T] = {
      new TaskM[T] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[T] = {
          val selfRes: TaskResult[Boolean] = self.run(verbose, input)

          selfRes.res match {
            case Success(r) =>
              val nextRes = f(r).run(verbose, input)
              nextRes.copy[T](out = Nil, err = Nil)
            case Failure(e) => TaskResult[T](Failure[T](e), selfRes.out, selfRes.err)
          }
        }
      }
    }
  }

  implicit def host2Hosts(host: HostLike): Hosts = Hosts(List(host))
}
