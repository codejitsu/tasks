package net.codejitsu.tasks.dsl

import org.scalatest.{FlatSpec, Matchers}

class GrepTest extends FlatSpec with Matchers {
  import net.codejitsu.tasks._
  import net.codejitsu.tasks.dsl.Tasks._

  import scala.concurrent.duration._

  implicit val timeout = 30 seconds

  implicit val stage = new Dev

  "grep" should "find all pattern occurrences in input" in {
    val task = Grep(Localhost, Option(getClass.getResource("/test-grep.txt").getPath), params = List("-o"),
      pattern = Option("title"))

    val taskResult = task.run()

    taskResult.res.isSuccess should be (true)
    taskResult.err should be (empty)
    taskResult.out.filter(_.startsWith("title")).size should be (65)
  }
}
