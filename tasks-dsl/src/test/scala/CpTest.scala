package net.codejitsu.tasks.dsl

import java.io.File
import java.util.UUID
import org.scalatest.{FlatSpec, Matchers}

/**
 * Cp tests.
 */
class CpTest extends FlatSpec with Matchers {
  import scala.concurrent.duration._
  import net.codejitsu.tasks._
  import net.codejitsu.tasks.dsl.Tasks._

  implicit val timeout = 30 seconds

  implicit val stage = new Dev {}

  "Cp task" should "copy a file with given name on given host" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val namecopy = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)
    val file2copy = new File(path + namecopy)

    file2create.exists should be (false)
    file2copy.exists should be (false)

    val touchTask = Touch(Localhost, path + name)

    val touchResult = touchTask.run()

    touchResult.res.isSuccess should be (true)
    touchResult.out should be (empty)
    touchResult.err should be (empty)

    file2create.exists should be (true)

    val copyTask = Cp(Localhost, path + name, path + namecopy)

    val copyResult = copyTask.run()

    copyResult.res.isSuccess should be (true)
    copyResult.out should be (empty)
    copyResult.err should be (empty)

    file2copy.exists should be (true)
  }

  it should "compose with the touch task" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val namecopy = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)
    val file2copy = new File(path + namecopy)

    file2create.exists should be (false)
    file2copy.exists should be (false)

    val task = for {
      tr <- Touch(Localhost, path + name)
      cr <- Cp(Localhost, path + name, path + namecopy)
    } yield cr

    val result = task.run()

    result.res.isSuccess should be (true)
    result.out should be (empty)
    result.err should be (empty)

    file2copy.exists should be (true)
  }

  it should "compose with the touch task with `andThen`" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val namecopy = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)
    val file2copy = new File(path + namecopy)

    file2create.exists should be (false)
    file2copy.exists should be (false)

    val task =
      Touch(Localhost, path + name) andThen
      Cp(Localhost, path + name, path + namecopy)

    val result = task.run()

    result.res.isSuccess should be (true)
    result.out should be (empty)
    result.err should be (empty)

    file2copy.exists should be (true)
  }

  it should "return error if file don't exists" in {
    val path = getClass.getResource("/program-param.sh").getPath.split("/").init.mkString("/") + "/"
    val name = s"${UUID.randomUUID().toString}testfile.txt"
    val namecopy = s"${UUID.randomUUID().toString}testfile.txt"
    val file2create = new File(path + name)
    val file2copy = new File(path + namecopy)

    file2create.exists should be (false)
    file2copy.exists should be (false)

    val task = for {
      tr  <- Touch(Localhost, path + name)
      cp <- Cp(Localhost, path + name + "1", path + namecopy)
    } yield cp

    val result = task.run()

    result.res.isSuccess should be (false)

    file2copy.exists should be (false)
  }
}
