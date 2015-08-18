package net.codejitsu.tasks.dsl

import org.scalatest.{FlatSpec, Matchers}

class PipeToTest extends FlatSpec with Matchers {
  import net.codejitsu.tasks._
  import net.codejitsu.tasks.dsl.Tasks._

  import scala.concurrent.duration._

  implicit val timeout = 30 seconds

  implicit val stage = new Dev

  "pipeTo" should "feed the next task with output from current task" in {
    val pathSh = getClass.getResource("/program-param.sh").getPath

    val task = ShellScript(Localhost, pathSh, List("test")) pipeTo Cat(Localhost)
    val taskResult = task.run()

    taskResult.res.isSuccess should be (true)
    taskResult.err should be (empty)
    taskResult.out should be (List("start test program with param: test"))
  }
}
