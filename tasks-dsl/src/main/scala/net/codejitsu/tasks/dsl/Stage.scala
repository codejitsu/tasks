// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

abstract class Stage {
  def name: String
  override def toString: String = name
}

final class Dev extends Stage {
  override val name: String = "Development"
}

final class Test extends Stage {
  override val name: String = "Test"
}

final class QA extends Stage {
  override val name: String = "QA"
}

final class Production extends Stage {
  override val name: String = "Production"
}

object Stage {
  implicit val defaultStage: Stage = new Stage {
    override def name: String = "Unstaged"
  }
}
