package net.codejitsu.tasks.dsl

import org.scalatest.{Matchers, FlatSpec}

import scala.util.Success

/**
 * DSL tests.
 */
class DslTest extends FlatSpec with Matchers {
  import scala.concurrent.duration._
  import net.codejitsu.tasks._
  import net.codejitsu.tasks.dsl.Tasks._

  implicit val timeout = 30 seconds

  implicit val stage = new Dev

  "DSL" should "allow to compose two host parts together with ~" in {
    val host: Host = "my" ~ "test" ~ "host" ~ "system.1"

    host.toString should be ("my.test.host.system.1")
  }

  it should "allow to compose strings and ranges with ~" in {
    val hosts: Hosts = "my" ~ "test" ~ "host" ~ (1 to 3) ~ "system.1"

    val all = hosts.hosts map (_.toString())

    all should be (List("my.test.host.1.system.1", "my.test.host.2.system.1", "my.test.host.3.system.1"))
  }

  it should "allow to compose ranges and strings with ~" in {
    val hosts: Hosts = (1 to 3) ~ "system.1" ~ "my" ~ "test" ~ "host"

    val all = hosts.hosts map (_.toString())

    all should be (List("1.system.1.my.test.host", "2.system.1.my.test.host", "3.system.1.my.test.host"))
  }

  it should "allow to compose ranges and ranges with ~" in {
    val hosts: Hosts = (1 to 3) ~ ('a' to 'b') ~ "system.1" ~ "my" ~ "test" ~ "host"

    val all = hosts.hosts map (_.toString())

    all should be (List("1.a.system.1.my.test.host", "1.b.system.1.my.test.host", "2.a.system.1.my.test.host",
      "2.b.system.1.my.test.host", "3.a.system.1.my.test.host", "3.b.system.1.my.test.host"))
  }

  it should "allow to compose ranges and tuples with ~" in {
    val hosts: Hosts = (1 to 3) ~ (('a', 'b')) ~ "system.1" ~ "my" ~ "test" ~ "host"

    val all = hosts.hosts map (_.toString())

    all should be (List("1.a.system.1.my.test.host", "1.b.system.1.my.test.host", "2.a.system.1.my.test.host",
      "2.b.system.1.my.test.host", "3.a.system.1.my.test.host", "3.b.system.1.my.test.host"))
  }

  it should "allow to compose tuples and ranges with ~" in {
    val hosts: Hosts = ((1, 2, 3)) ~ ('a' to 'b') ~ "system.1" ~ "my" ~ "test" ~ "host"

    val all = hosts.hosts map (_.toString())

    all should be (List("1.a.system.1.my.test.host", "1.b.system.1.my.test.host", "2.a.system.1.my.test.host",
      "2.b.system.1.my.test.host", "3.a.system.1.my.test.host", "3.b.system.1.my.test.host"))
  }

  it should "allow to compose strings and tuples with ~" in {
    val hosts: Hosts = "my" ~ "test" ~ "host" ~ (("abc", 100, 'z')) ~ "system.1"

    val all = hosts.hosts map (_.toString())

    all should be (List("my.test.host.abc.system.1", "my.test.host.100.system.1", "my.test.host.z.system.1"))
  }

  it should "allow to compose tuples and tuples with ~" in {
    val hosts: Hosts = "my" ~ "test" ~ "host" ~ (("abc", 100, 'z')) ~ (("one", "two")) ~ "system.1"

    val all = hosts.hosts map (_.toString())

    all.sorted should be (List("my.test.host.abc.one.system.1", "my.test.host.abc.two.system.1", "my.test.host.100.one.system.1",
      "my.test.host.100.two.system.1", "my.test.host.z.one.system.1", "my.test.host.z.two.system.1").sorted)
  }

  it should "allow to compose tuples and strings with ~" in {
    val hosts: Hosts = (("abc", 100, 'z')) ~ "my" ~ "test" ~ "host" ~ "system.1"

    val all = hosts.hosts map (_.toString())

    all should be (List("abc.my.test.host.system.1", "100.my.test.host.system.1", "z.my.test.host.system.1"))
  }

  it should "allow to compose tuples and strings with |" in {
    val hosts: Hosts = "host" | (("abc", 100, 'z')) ~ "my" ~ "test" ~ "host" ~ "system.1"

    val all = hosts.hosts map (_.toString())

    all should be (List("hostabc.my.test.host.system.1", "host100.my.test.host.system.1", "hostz.my.test.host.system.1"))
  }

  it should "allow to compose tuples and strings with | (2)" in {
    val hosts: Hosts = "host" | (("abc", 100, 'z')) | "my" ~ "test" ~ "host" ~ "system.1"

    val all = hosts.hosts map (_.toString())

    all should be (List("hostabcmy.test.host.system.1", "host100my.test.host.system.1", "hostzmy.test.host.system.1"))
  }

  it should "allow to compose tuples and strings with | (3)" in {
    val hosts: Hosts = "host" | (("abc", 100, 'z'))

    val all = hosts.hosts map (_.toString())

    all should be (List("hostabc", "host100", "hostz"))
  }

  it should "allow to compose ranges and strings with |" in {
    val hosts: Hosts = "host" | (1 to 3) ~ "my" ~ "test" ~ "host" ~ "system.1"

    val all = hosts.hosts map (_.toString())

    all should be (List("host1.my.test.host.system.1", "host2.my.test.host.system.1", "host3.my.test.host.system.1"))
  }

  it should "allow to compose ranges and strings with ~ (2)" in {
    val hosts: Hosts = "host" ~ (1 to 3) ~ "my" ~ "test" ~ "host" ~ "system.1"

    val all = hosts.hosts map (_.toString())

    all should be (List("host.1.my.test.host.system.1", "host.2.my.test.host.system.1", "host.3.my.test.host.system.1"))
  }

  it should "allow to compose ranges and strings with | (2)" in {
    val hosts: Hosts = "host" | (1 to 3) | "my" ~ "test" ~ "host" ~ "system.1"

    val all = hosts.hosts map (_.toString())

    all should be (List("host1my.test.host.system.1", "host2my.test.host.system.1", "host3my.test.host.system.1"))
  }

  it should "allow to create single host from string with .h" in {
    val host: Host = "my.test.host".h

    host.toString() should be ("my.test.host")
  }

  it should "allow to convert single string to hosts" in {
    val hosts: Hosts = "my.test.host".h

    hosts.hosts.size should be (1)
    hosts.hosts.head.toString() should be ("my.test.host")
  }

  it should "allow to define process on host" in {
    val host = "my" ~ "test" ~ "host"

    val process: Process = "tomcat" on host ~> {
      case Start => Exec("/etc/init.d/tomcat", List("start"))
      case Stop => Exec("/etc/init.d/tomcat", List("stop"))
    }

    process.name should be ("tomcat")
    process.startCmd should be (Exec("/etc/init.d/tomcat", List("start")))
    process.stopCmd should be (Exec("/etc/init.d/tomcat", List("stop")))
    process.host should be (host)
  }

  it should "allow to define process on simple host" in {
    val process: Process = "tomcat" on "my.test.host" ~> {
      case Start => Exec("/etc/init.d/tomcat", List("start"))
      case Stop => Exec("/etc/init.d/tomcat", List("stop"))
    }

    process.name should be ("tomcat")
    process.startCmd should be (Exec("/etc/init.d/tomcat", List("start")))
    process.stopCmd should be (Exec("/etc/init.d/tomcat", List("stop")))
    process.host should be (Host(List(HostPart("my.test.host"))))
  }

  it should "allow to define process on localhost" in {
    val process: Process = "tomcat" on Localhost ~> {
      case Start => Exec("/etc/init.d/tomcat", List("start"))
      case Stop => Exec("/etc/init.d/tomcat", List("stop"))
    }

    process.name should be ("tomcat")
    process.startCmd should be (Exec("/etc/init.d/tomcat", List("start")))
    process.stopCmd should be (Exec("/etc/init.d/tomcat", List("stop")))
    process.host should be (Localhost)
  }

  it should "allow to define processes on multiple hosts" in {
    val hosts: Hosts = "my" ~ "test" ~ "host" ~ (1 to 10)

    val tomcats: Processes = "tomcat" on hosts ~> {
      case Start => Exec("/etc/init.d/tomcat", List("start"))
      case Stop => Exec("/etc/init.d/tomcat", List("stop"))
    }

    tomcats.procs.size should be (10)

    tomcats.procs.map(_.host.toString).toSet.size should be (10)

    tomcats.procs.foreach { tomcat =>
      tomcat.name should be ("tomcat")
      tomcat.startCmd should be (Exec("/etc/init.d/tomcat", List("start")))
      tomcat.stopCmd should be (Exec("/etc/init.d/tomcat", List("stop")))
      tomcat.host.toString.startsWith("my.test.host") should be (true)
    }
  }

  it should "allow to define processes on multiple hosts (simple view)" in {
    val hosts = "my" ~ "test" ~ "host" ~ (1 to 10)

    val tomcats = "tomcat" on hosts ~> {
      case Start => Exec("/etc/init.d/tomcat", List("start"))
      case Stop => Exec("/etc/init.d/tomcat", List("stop"))
    }

    tomcats.procs.size should be (10)

    tomcats.procs.map(_.host.toString).toSet.size should be (10)

    tomcats.procs.foreach { tomcat =>
      tomcat.name should be ("tomcat")
      tomcat.startCmd should be (Exec("/etc/init.d/tomcat", List("start")))
      tomcat.stopCmd should be (Exec("/etc/init.d/tomcat", List("stop")))
      tomcat.host.toString.startsWith("my.test.host") should be (true)
    }
  }

  it should "allow to define processes on multiple hosts (simple view, 2)" in {
    val hosts = "my.test.host" ~ (1 to 10)

    val tomcats = "tomcat" on hosts ~> {
      case Start => Exec("/etc/init.d/tomcat", List("start"))
      case Stop => Exec("/etc/init.d/tomcat", List("stop"))
    }

    tomcats.procs.size should be (10)

    tomcats.procs.map(_.host.toString).toSet.size should be (10)

    tomcats.procs.foreach { tomcat =>
      tomcat.name should be ("tomcat")
      tomcat.startCmd should be (Exec("/etc/init.d/tomcat", List("start")))
      tomcat.stopCmd should be (Exec("/etc/init.d/tomcat", List("stop")))
      tomcat.host.toString.startsWith("my.test.host") should be (true)
    }
  }

  it should "allow to define processes on multiple hosts (simple view, 3)" in {
    val hosts = "my.test.host" ~ ((1, 2, 3, 4, 5, 6, 7, 8, 9, 10))

    val tomcats = "tomcat" on hosts ~> {
      case Start => Exec("/etc/init.d/tomcat", List("start"))
      case Stop => Exec("/etc/init.d/tomcat", List("stop"))
    }

    tomcats.procs.size should be (10)

    tomcats.procs.map(_.host.toString).toSet.size should be (10)

    tomcats.procs.foreach { tomcat =>
      tomcat.name should be ("tomcat")
      tomcat.startCmd should be (Exec("/etc/init.d/tomcat", List("start")))
      tomcat.stopCmd should be (Exec("/etc/init.d/tomcat", List("stop")))
      tomcat.host.toString.startsWith("my.test.host") should be (true)
    }
  }

  it should "allow to define tasks with processes" in {
    val tomcat: Process = "tomcat" on Localhost ~> {
      case Start => Exec("/etc/init.d/tomcat", List("start"))
      case Stop => Exec("/etc/init.d/tomcat", List("stop"))
    }

    val startTomcat = tomcat ! Start
    val stopTomcat = tomcat ! Stop

    assert(Option(startTomcat).isDefined)
    assert(Option(stopTomcat).isDefined)
  }

  it should "run processes on localhost" in {
    val procStart = "/bin/sh"
    val procStop = "/bin/sh"

    val program: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-start.sh").getPath))
      case Stop => Exec(procStop, List(getClass.getResource("/program-stop.sh").getPath))
    }

    val startShell = program ! Start
    val startResult = startShell.run()

    startResult.res.isSuccess should be (true)
    startResult.out should be (List("start test program"))

    val stopShell = program ! Stop
    val stopResult = stopShell.run()

    stopResult.res.isSuccess should be (true)
    stopResult.out should be (List("stop test program"))
  }

  it should "compose processes on localhost" in {
    val procStart = "/bin/sh"
    val procStop = "/bin/sh"

    val program: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-start.sh").getPath))
      case Stop => Exec(procStop, List(getClass.getResource("/program-stop.sh").getPath))
    }

    val startShell = program ! Start
    val stopShell = program ! Stop

    val composed = startShell andThen stopShell
    val composedResult = composed.run()

    composedResult.res.isSuccess should be (true)
    composedResult.out should be (List("start test program", "stop test program"))
  }

  it should "compose processes on monadic way" in {
    val procStart = "/bin/sh"
    val procStop = "/bin/sh"

    val program: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-start.sh").getPath))
      case Stop => Exec(procStop, List(getClass.getResource("/program-stop.sh").getPath))
    }

    val startShell = program ! Start
    val stopShell = program ! Stop

    val composed = for {
      stSh <- startShell
      stopSh <- stopShell
    } yield stopSh

    val composedResult = composed.run()

    composedResult.res.isSuccess should be (true)
    composedResult.out should be (List("start test program", "stop test program"))
  }

  it should "compose processes on monadic way (one task)" in {
    val procStart = "/bin/sh"
    val procStop = "/bin/sh"

    val program: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-start.sh").getPath))
      case Stop => Exec(procStop, List(getClass.getResource("/program-stop.sh").getPath))
    }

    val startShell = program ! Start

    val composed = for {
      stSh <- startShell
    } yield stSh

    val composedResult = composed.run()

    composedResult.res.isSuccess should be (true)
    composedResult.out should be (List("start test program"))
  }

  it should "compose processes on monadic way (failure)" in {
    val procStart = "/bin/sh"
    val procStop = "/bin/sh"

    val program: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-start.sh").getPath))
      case Stop => Exec(procStop, List(getClass.getResource("/program-stop.sh").getPath))
    }

    val startShellFailure = FailedTask(List(), List("task error"))
    val stopShell = program ! Stop

    val composed = for {
      stSh <- startShellFailure
      stopSh <- stopShell
    } yield stopSh

    val composedResult = composed.run()

    composedResult.res.isSuccess should be (false)
  }

  it should "run processes sequentially" in {
    val procStart = "/bin/sh"

    val program1: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "1"))
    }

    val program2: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "2"))
    }

    val program3: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "3"))
    }

    val program4: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "4"))
    }

    val processes: Processes = Processes(List(program1, program2, program3, program4))

    val startAllProcsSeq = processes ! Start

    val composedResult = startAllProcsSeq.run()

    composedResult.res.isSuccess should be (true)
    composedResult.out should be (List("start test program with param: 1", "start test program with param: 2",
      "start test program with param: 3", "start test program with param: 4"))
    composedResult.err should be (empty)
  }

  it should "run processes parallel" in {
    val procStart = "/bin/sh"

    val program1: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "1"))
    }

    val program2: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "2"))
    }

    val program3: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "3"))
    }

    val program4: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "4"))
    }

    val processes: Processes = Processes(List(program1, program2, program3, program4))

    val startAllProcsSeq = processes !! Start

    val composedResult = startAllProcsSeq.run()

    composedResult.res.isSuccess should be (true)
    composedResult.out should have size (4)
    composedResult.out should contain allOf ("start test program with param: 1", "start test program with param: 2",
      "start test program with param: 3", "start test program with param: 4")
    composedResult.err should be (empty)
  }

  it should "preserve the execution order" in {
    val procStart = "/bin/sh"

    val program1: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "1"))
    }

    val program2: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "2"))
    }

    val program3: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "3"))
    }

    val program4: Process = "test" on Localhost ~> {
      case Start => Exec(procStart, List(getClass.getResource("/program-param.sh").getPath, "4"))
    }

    val task1 = new TaskM[Boolean] {
      override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = (program1 ! Start).run()
    }

    val task2 = new TaskM[Boolean] {
      override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = (program2 ! Start).run()
    }

    val task3 = new TaskM[Boolean] {
      override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = (program3 ! Start).run()
    }

    val task4 = new TaskM[Boolean] {
      override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = (program4 ! Start).run()
    }

    val composed = task1 andThen task2 andThen task3 andThen task4
    val composedResult1 = composed()

    composedResult1.res.isSuccess should be (true)
    composedResult1.out should be (List("start test program with param: 1", "start test program with param: 2",
      "start test program with param: 3", "start test program with param: 4"))
    composedResult1.err should be (empty)

    val composedResult2 = composed()

    composedResult2.res.isSuccess should be (true)
    composedResult2.out should be (List("start test program with param: 1", "start test program with param: 2",
      "start test program with param: 3", "start test program with param: 4"))
    composedResult2.err should be (empty)
  }

  it should "have leftIdentity property" in {
    def f(v: Boolean): TaskM[String] = if (v) {
      new TaskM[String] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[String] = TaskResult(Success("true"), Nil, Nil)
      }
    } else {
      new TaskM[String] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[String] = TaskResult(Success("false"), Nil, Nil)
      }
    }

    val task = new TaskM[Boolean] {
      override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = TaskResult(Success(false), List("a"), List("b"))
    }

    val lhs = task.flatMap(f)

    val rhs = f(false)

    lhs().res should be (rhs().res)
  }

  it should "have rightIdentity property" in {
    def f(v: Boolean): TaskM[String] = if (v) {
      new TaskM[String] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[String] = TaskResult(Success("true"), Nil, Nil)
      }
    } else {
      new TaskM[String] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[String] = TaskResult(Success("false"), Nil, Nil)
      }
    }

    def g(v: Boolean): TaskM[String] = if (v) {
      new TaskM[String] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[String] = TaskResult(Success("a"), Nil, Nil)
      }
    } else {
      new TaskM[String] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[String] = TaskResult(Success("b"), Nil, Nil)
      }
    }

    val task = new TaskM[Boolean] {
      override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = TaskResult(Success(false), List("a"), List("b"))
    }

    val lhs = task.flatMap {v =>
      new TaskM[Boolean] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[Boolean] = TaskResult(Success(v), List("a"), List("b"))
      }
    }

    val rhs = task

    lhs().res should be (rhs().res)
  }

  it should "have associativity property" in {
    def f(v: String): TaskM[String] = if (v.isEmpty) {
      new TaskM[String] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[String] = TaskResult(Success("true"), Nil, Nil)
      }
    } else {
      new TaskM[String] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[String] = TaskResult(Success("false"), Nil, Nil)
      }
    }

    def g(v: String): TaskM[String] = if (v.isEmpty) {
      new TaskM[String] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[String] = TaskResult(Success("a"), Nil, Nil)
      }
    } else {
      new TaskM[String] {
        override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[String] = TaskResult(Success("b"), Nil, Nil)
      }
    }

    val task = new TaskM[String] {
      override def run(verbose: VerbosityLevel = NoOutput, input: Option[TaskResult[_]] = None): TaskResult[String] = TaskResult(Success(""), List("a"), List("b"))
    }

    val lhs = task.flatMap(f).flatMap(g)

    val rhs = task.flatMap(x => f(x).flatMap(g))

    lhs().res should be (rhs().res)
  }

  it should "use orElse if previous step fails" in {
    val startShellFailure = FailedTask(List(), List("task error"))

    val composed = startShellFailure orElse SuccessfulTask

    val composedResult = composed.run()

    composedResult.res.isSuccess should be (true)
  }

  it should "use orElse to compose multiple failed tasks" in {
    val startShellFailure = FailedTask(List(), List("task error"))

    val composed = startShellFailure orElse
                   startShellFailure orElse
                   startShellFailure orElse
                   startShellFailure orElse
                   startShellFailure orElse
                   startShellFailure orElse
                   SuccessfulTask

    val composedResult = composed.run()

    composedResult.res.isSuccess should be (true)
  }

  def deploymentTomcat(): Unit = {
    val hosts = "my.dev.test-host" ~ (1 to 5)
    val file = "/tmp/test.war"

    val user = NoUser

    val deployOnTomcat = for {
      _ <- RmIfExists(hosts, user.home / "test.war")
      _ <- Upload(hosts, file, user.home)
      _ <- Sudo ~ StopTomcat(hosts)
      _ <- Mv(hosts, user.home / "test.war", "/tomcat/webapp/")
      _ <- Sudo ~ StartTomcat(hosts)
      _ <- Wait(30 seconds)
      deployed <- CheckUrl(hosts, "/webapp/health")
    } yield deployed

    val deployOnTomcat2 =
        RmIfExists(hosts, user.home / "test.war") andThen
        Upload(hosts, file, user.home) andThen
        Sudo ~ StopTomcat(hosts) andThen
        Mv(hosts, user.home / "test.war", "/tomcat/webapp/") andThen
        Sudo ~ StartTomcat(hosts) andThen
        Wait(30 seconds) andThen
        CheckUrl(hosts, "/webapp/health")
  }
}
