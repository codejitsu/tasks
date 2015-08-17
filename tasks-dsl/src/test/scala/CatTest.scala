package net.codejitsu.tasks.dsl

import java.io.File
import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}

class CatTest extends FlatSpec with Matchers {
  import net.codejitsu.tasks._
  import net.codejitsu.tasks.dsl.Tasks._

  import scala.concurrent.duration._

  implicit val timeout = 30 seconds

  implicit val stage = new Dev

  "cat" should "display content of a file" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)

    file2create.exists should be (false)

    val pathSh = getClass.getResource("/program-param.sh").getPath

    val task =
      Touch(Localhost, path + name) andThen
      ShellScript(Localhost, pathSh, List("test")) andThen
      Cat(Localhost, Option(path + name))

    val taskResult = task.run()

    taskResult.res.isSuccess should be (true)

    taskResult.err should be (empty)
    taskResult.out should be (List("start test program with param: test"))

    file2create.exists should be (true)

    file2create.delete
  }
}
