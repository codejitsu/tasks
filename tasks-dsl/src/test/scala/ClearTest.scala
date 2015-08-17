package net.codejitsu.tasks.dsl

import java.io.File
import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}

class ClearTest extends FlatSpec with Matchers {
  import net.codejitsu.tasks._
  import net.codejitsu.tasks.dsl.Tasks._

  import scala.concurrent.duration._

  implicit val timeout = 30 seconds

  implicit val stage = new Dev

  "clear" should "remove all previous output" in {
    val path = getClass.getResource("/program-tail.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)

    file2create.exists should be (false)

    val pathSh = getClass.getResource("/program-tail.sh").getPath

    val task =
        Touch(Localhost, path + name) andThen
        ShellScript(Localhost, pathSh, List("test")) pipeTo
        Echo(Localhost, target = Option(path + name)) andThen
        Tail(Localhost, Option(path + name), params = List("-n", "1"))
        Clear()

    val taskResult = task.run()

    file2create.exists should be (true)

    taskResult.res.isSuccess should be (true)
// FIXME
//    taskResult.err should be (empty)
//    taskResult.out should be (List("9 start test program with param: test"))

    file2create.delete
  }
}
