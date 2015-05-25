// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

import java.io.File

import net.codejitsu.tasks.dsl.User.PasswordFunc

import scala.io.StdIn

sealed trait User {
  def username: String
  def password: PasswordFunc
}

case object NoUser extends User {
  override def username: String = throw new IllegalStateException("no username")

  override def password: PasswordFunc = () => Array.empty[Char]
}

case class SshUser(username: String, keyFile: Option[File]) extends User with SshCredentials {
  lazy val password: PasswordFunc = () => StdIn.readLine("Please enter your passphrase:").toCharArray
}

case class SshUserWithPassword(username: String, keyFile: Option[File], pwd: String) extends User with SshCredentials {
  lazy val password: PasswordFunc = () => pwd.toCharArray
}

case class LocalUser(username: String) extends User {
  lazy val password: PasswordFunc = () => StdIn.readLine("Please enter your passphrase:").toCharArray
}

object User {
  type PasswordFunc = () => Array[Char]

  implicit val DefaultUser: User = NoUser
}
