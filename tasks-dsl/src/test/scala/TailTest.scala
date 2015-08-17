package net.codejitsu.tasks.dsl

import java.io.File
import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}

class TailTest extends FlatSpec with Matchers {
  import net.codejitsu.tasks._
  import net.codejitsu.tasks.dsl.Tasks._

  import scala.concurrent.duration._

  implicit val timeout = 30 seconds

  implicit val stage = new Dev

  "tail" should "print the last lines of input" in {
    val task = Tail(Localhost, Option(getClass.getResource("/test-tail.txt").getPath), params = List("-n", "1"))

    val taskResult = task.run()

    taskResult.res.isSuccess should be (true)
    taskResult.err should be (empty)
    taskResult.out should be (List("9 start test program with param: test"))
  }

  "tail" should "print the last lines of input with pipeTo" in {
    val path = getClass.getResource("/program-tail.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)

    file2create.exists should be (false)

    val pathSh = getClass.getResource("/program-tail.sh").getPath

    val task =
      Touch(Localhost, path + name) pipeTo
      ShellScript(Localhost, pathSh, List("test")) pipeTo
      Echo(Localhost, target = Option(path + name)) pipeTo
      Tail(Localhost, Option(path + name), params = List("-n", "1"))

    val taskResult = task.run()

    println(taskResult.out)

    file2create.exists should be (true)

    taskResult.res.isSuccess should be (true)
    taskResult.err should be (empty)
    taskResult.out.count(_ == "9 start test program with param: test") should be (2)

    file2create.delete
  }
}
