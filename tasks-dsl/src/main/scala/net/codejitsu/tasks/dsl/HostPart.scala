// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

/**
 * Part of the host definition.
 */
final case class HostPart(part: String) {
  val ValidHostnameRegex = """^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$""".r

  override def toString(): String = part

  def isValid: Boolean = ValidHostnameRegex.pattern.matcher(part).matches
}
