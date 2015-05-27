package net.codejitsu.tasks.dsl

import java.io.File
import org.scalatest.{FlatSpec, Matchers}

/**
 * RmIfExists tests.
 */
class RmIfExistsTest extends FlatSpec with Matchers {
  import scala.concurrent.duration._
  import Dsl._

  implicit val timeout = 30 seconds

  "RmIfExists task" should "remove a file with given name on given host" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val file2create = new File(path + "testfile.txt")

    file2create.exists should be (false)

    val touchTask = Touch(Localhost, path + "testfile.txt")

    val touchResult = touchTask.run()

    touchResult._1.isSuccess should be (true)
    touchResult._2 should be (empty)
    touchResult._3 should be (empty)

    file2create.exists should be (true)

    val rmTask = RmIfExists(Localhost, path + "testfile.txt")

    val rmResult = rmTask.run()

    rmResult._1.isSuccess should be (true)
    rmResult._2 should be (empty)
    rmResult._3 should be (empty)

    file2create.exists should be (false)
  }

  it should "compose with the touch task" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val file2create = new File(path + "testfile.txt")

    file2create.exists should be (false)

    val task = for {
      tr <- Touch(Localhost, path + "testfile.txt")
      rr <- RmIfExists(Localhost, path + "testfile.txt")
    } yield rr

    val result = task.run()

    result._1.isSuccess should be (true)
    result._2 should be (empty)
    result._3 should be (empty)

    file2create.exists should be (false)
  }

  it should "compose with the touch task with `andThen`" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val file2create = new File(path + "testfile.txt")

    file2create.exists should be (false)

    val task =
      Touch(Localhost, path + "testfile.txt") andThen
        RmIfExists(Localhost, path + "testfile.txt")

    val result = task.run()

    result._1.isSuccess should be (true)
    result._2 should be (empty)
    result._3 should be (empty)

    file2create.exists should be (false)
  }

  it should "not return error if file dont exists" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val file2create = new File(path + "testfile.txt")

    file2create.exists should be (false)

    val task = for {
      tr  <- Touch(Localhost, path + "testfile.txt")
      rr1 <- RmIfExists(Localhost, path + "testfile.txt")
      rr2 <- RmIfExists(Localhost, path + "testfile.txt")
    } yield rr2

    val result = task.run()

    result._1.isSuccess should be (true)
    result._2 should be (empty)
    result._3 should be (empty)

    file2create.exists should be (false)
  }
}
