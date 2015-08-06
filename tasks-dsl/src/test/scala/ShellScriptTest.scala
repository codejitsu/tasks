package net.codejitsu.tasks.dsl

import org.scalatest.{FlatSpec, Matchers}

class ShellScriptTest extends FlatSpec with Matchers {
  import net.codejitsu.tasks._
  import net.codejitsu.tasks.dsl.Tasks._

  import scala.concurrent.duration._

  implicit val timeout = 30 seconds

  implicit val stage = new Dev

  "shell script task" should "run shell scripts" in {
    val path = getClass.getResource("/program-param.sh").getPath

    val task = ShellScript(Localhost, path, List("test", "1", "-z"))

    val result = task.run()

    result.res.isSuccess should be (true)
    result.out should be (List("start test program with param: test 1 -z"))
    result.err should be (empty)
  }
}
