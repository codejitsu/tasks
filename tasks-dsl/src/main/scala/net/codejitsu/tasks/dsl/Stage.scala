// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

trait Stage {
  def name: String
  override def toString: String = name
}

trait Dev extends Stage {
  override val name: String = "Development"
}

trait Test extends Stage {
  override val name: String = "Test"
}

trait QA extends Stage {
  override val name: String = "QA"
}

trait Production extends Stage {
  override val name: String = "Production"
}

object Stage {
  implicit val defaultStage: Stage = new Stage {
    override def name: String = "Unstaged"
  }
}