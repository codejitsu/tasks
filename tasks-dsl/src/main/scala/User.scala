// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

import java.io.{FileInputStream, File}
import java.util.Properties

import net.codejitsu.tasks.dsl.User.PasswordFunc

import scala.io.StdIn
import scala.util.Try

sealed trait User {
  def username: String
  def password: PasswordFunc

  lazy val localName: String = System.getProperty("user.name")
  lazy val localPassword: PasswordFunc = () => StdIn.readLine("Please enter your passphrase:").toCharArray

  lazy val home: String = s"/home/$username"
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

object User {
  type PasswordFunc = () => Array[Char]

  implicit val DefaultUser: User = NoUser

  def load: User = {
    val sshT = Try {
      val sshProp = new Properties()

      sshProp.load(new FileInputStream(s"/home/${System.getProperty("user.name")}/.sbt-robot/ssh.properties"))

      SshUserWithPassword(sshProp.getProperty("username").trim,
        Option(new java.io.File(sshProp.getProperty("keyfile").trim)),
        sshProp.getProperty("password").trim)
    }

    sshT.getOrElse(DefaultUser)
  }
}
