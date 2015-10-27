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

  it should "accept special characters in pattern" in {
    val task = Grep(Localhost, Option(getClass.getResource("/test-grep.txt").getPath), params = List("-o"),
      pattern = Option("<title"))

    val taskResult = task.run()

    taskResult.res.isSuccess should be (true)
    taskResult.err should be (empty)
    taskResult.out.filter(_.startsWith("<title")).size should be (31)
  }

  it should "accept space characters in pattern" in {
    val task = Grep(Localhost, Option(getClass.getResource("/test-grep.txt").getPath), params = List("-o"),
      pattern = Option("<title type="))

    val taskResult = task.run()

    taskResult.res.isSuccess should be (true)
    taskResult.err should be (empty)
    taskResult.out.filter(_.startsWith("<title")).size should be (31)
  }

  it should "accept regex in pattern" in {
    val task = Grep(Localhost, Option(getClass.getResource("/test-grep.txt").getPath), params = List("-o"),
      pattern = Option("<title type=\"text\">[^<]*"))

    val taskResult = task.run()

    taskResult.res.isSuccess should be (true)
    taskResult.err should be (empty)
    taskResult.out.filter(_.startsWith("<title")).size should be (31)
  }
//
//  it should "support piping" in {
//    val taskOne = Grep(Localhost, Option(getClass.getResource("/test-grep.txt").getPath), params = List("-o"),
//      pattern = Option("<title type=\"text\">[^<]*"))
//
//    val taskTwo = Grep(Localhost, params = List("-o"), pattern = Option("[^>]*$"))
//
//    val task = taskOne pipeTo taskTwo
//
//    val taskResult = task.run()
//
//    println(taskResult)
//
//    taskResult.res.isSuccess should be (true)
//    taskResult.err should be (empty)
//    taskResult.out.filter(_.startsWith("<title")).size should be (0)
//  }
}
