package net.codejitsu.tasks.dsl

import java.io.File
import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}

class PipeToTest extends FlatSpec with Matchers {
  import net.codejitsu.tasks._
  import net.codejitsu.tasks.dsl.Tasks._

  import scala.concurrent.duration._

  implicit val timeout = 30 seconds

  implicit val stage = new Dev

  "pipeTo" should "feed the next task with output from current task" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)

    file2create.exists should be (false)

    val pathSh = getClass.getResource("/program-param.sh").getPath

    val task = Touch(Localhost, path + name) andThen
               ShellScript(Localhost, pathSh, List("test")) pipeTo
               Cat(Localhost)

    val taskResult = task.run()

    file2create.exists should be (true)

    taskResult.res.isSuccess should be (true)
    taskResult.err should be (empty)
    taskResult.out should be (List("start test program with param: test"))

    file2create.delete
  }
}
