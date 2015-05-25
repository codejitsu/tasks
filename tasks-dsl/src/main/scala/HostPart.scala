// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

/**
 * Part of the host definition.
 */
case class HostPart(part: String) {
  //ValidIpAddressRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"

  val ValidHostnameRegex = """^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$""".r

  override def toString(): String = part

  def isValid: Boolean = ValidHostnameRegex.pattern.matcher(part).matches
}
