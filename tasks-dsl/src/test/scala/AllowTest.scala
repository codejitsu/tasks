package net.codejitsu.tasks.dsl

import net.codejitsu.tasks._
import net.codejitsu.tasks.dsl.Tasks._
import org.scalatest.{FlatSpec, Matchers}

/**
 * Allow tests.
 */
class AllowTest extends FlatSpec with Matchers {

  import scala.concurrent.duration._

  implicit val timeout = 30 seconds

  "For a stage" should "not allow tasks without allow rule defined" in {
    case class TestTask1[S <: Stage](implicit user: User, stage: S, rights: S Allow TestTask1[S])
      extends GenericTask("test", "test", Localhost, "test", List(),
        false, false)

    implicit val stage = new Dev
    assertTypeError("val res = TestTask1().run()")
  }

  "For a stage" should "allow tasks with allow rule defined" in {
    case class TestTask2[S <: Stage](implicit user: User, stage: S, rights: S Allow TestTask2[S])
      extends GenericTask("test", "test", Localhost, "test", List(),
        false, false)

    implicit val stage = new Dev
    implicit val allowTestTask2: Dev Allow TestTask2[Dev] = new Allow[Dev, TestTask2[Dev]]

    TestTask2().run()

    //it should compile
  }

  "For a stage" should "select the stage rule correctly" in {
    case class TestTask3[S <: Stage](implicit user: User, stage: S, rights: S Allow TestTask3[S])
      extends GenericTask("test", "test", Localhost, "test", List(),
        false, false)

    implicit val stage = new Production
    implicit val allowDevTestTask3: Dev Allow TestTask3[Dev] = null //null is the correct value
    assertTypeError("val res = TestTask3().run()")
  }
}