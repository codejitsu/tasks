package net.codejitsu.tasks.dsl

import java.io.File
import java.util.UUID
import org.scalatest.{FlatSpec, Matchers}

/**
 * RmTask tests.
 */
class RmTest extends FlatSpec with Matchers {
  import scala.concurrent.duration._
  import net.codejitsu.tasks._
  import net.codejitsu.tasks.dsl.Tasks._

  implicit val timeout = 30 seconds

  implicit val stage = new Dev {}

  "Rm task" should "remove a file with given name on given host" in {
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

    val rmTask: TaskM[Boolean] = Rm(Localhost, path + name)

    val rmResult = rmTask.run()

    rmResult.res.isSuccess should be (true)
    rmResult.out should be (empty)
    rmResult.err should be (empty)

    file2create.exists should be (false)
  }

  it should "compose with the touch task" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)

    file2create.exists should be (false)

    val task = for {
      tr <- Touch(Localhost, path + name)
      rr <- Rm(Localhost, path + name)
    } yield rr

    val result = task.run()

    result.res.isSuccess should be (true)
    result.out should be (empty)
    result.err should be (empty)

    file2create.exists should be (false)
  }

  it should "compose with the touch task with `andThen`" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)

    file2create.exists should be (false)

    val task =
      Touch(Localhost, path + name) andThen
      Rm(Localhost, path + name)

    val result = task.run()

    result.res.isSuccess should be (true)
    result.out should be (empty)
    result.err should be (empty)

    file2create.exists should be (false)
  }

  it should "return error if file don't exists" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)

    file2create.exists should be (false)

    val task = for {
      tr  <- Touch(Localhost, path + name)
      rr1 <- Rm(Localhost, path + name)
      rr2 <- Rm(Localhost, path + name)
    } yield rr2

    val result = task.run()

    result.res.isSuccess should be (false)
    file2create.exists should be (false)
  }
}
