package net.codejitsu.tasks.dsl

import java.io.File
import java.util.UUID

import org.scalatest.{Matchers, FlatSpec}

/**
 * TouchTask tests.
 */
class TouchTest extends FlatSpec with Matchers {
  import scala.concurrent.duration._
  import net.codejitsu.tasks._
  import net.codejitsu.tasks.dsl.Tasks._

  implicit val timeout = 30 seconds

  implicit val stage = new Dev

  "Touch task" should "create a file with given name on given host" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)

    file2create.exists should be (false)

    val touchTask: TaskM[Boolean] = Touch(Localhost, path + name)

    val touchResult = touchTask.run()

    touchResult.res.isSuccess should be (true)
    touchResult.out should be (empty)
    touchResult.err should be (empty)

    file2create.exists should be (true)

    file2create.delete
  }
}

