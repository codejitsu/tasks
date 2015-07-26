// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

import java.io.{File, FileInputStream}
import java.util.Properties

import net.codejitsu.tasks.dsl.User.PasswordFunc

import scala.io.StdIn
import scala.util.{Failure, Success, Try}

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

final case class SshUser(username: String, keyFile: Option[File]) extends User with SshCredentials {
  lazy val password: PasswordFunc = () => StdIn.readLine("Please enter your passphrase:").toCharArray
}

final case class SshUserWithPassword(username: String, keyFile: Option[File], pwd: String) extends User with SshCredentials {
  lazy val password: PasswordFunc = () => pwd.toCharArray
}

object User {
  type PasswordFunc = () => Array[Char]

  implicit val DefaultUser: User = NoUser

  final val sshTasksJvmPropertyName = "ssh.tasks.properties.file" // Prio 1
  final val sshTasksEnvironmentVarName = "SSH_TASKS_PROPERTIES_FILE" // Prio 2
  final val sshTasksDefaultPath =
    s"/home/${System.getProperty("user.name")}/.ssh-tasks/ssh.properties" // Prio 3

  def load: User = {
    val sshT = Try {
      val sshProp = new Properties()

      val sshPath = Option(System.getProperty(sshTasksJvmPropertyName)).
                    orElse(Option(System.getenv(sshTasksEnvironmentVarName))).
                    getOrElse(sshTasksDefaultPath)

      sshProp.load(new FileInputStream(sshPath))

      val usernameOpt = Option(sshProp.getProperty("username"))

      if(usernameOpt.isEmpty) {
        throw new IllegalArgumentException("User.load error: 'username' not defined")
      }

      val keyfileOpt = Option(sshProp.getProperty("keyfile"))

      if(keyfileOpt.isEmpty) {
        throw new IllegalArgumentException("User.load error: 'keyfile' not defined")
      }

      val pwdOpt = Option(sshProp.getProperty("password"))

      if (pwdOpt.isEmpty) {
        throw new IllegalArgumentException("User.load error: 'password' not defined")
      }

      for {
        username <- usernameOpt
        keyfile <- keyfileOpt
        pwd <- pwdOpt
      } yield SshUserWithPassword(username.trim, Option(new java.io.File(keyfile.trim)), pwd.trim)
    }

    sshT match {
      case Success(Some(user)) => user
      case Success(None) => DefaultUser
      case Failure(th) =>
        //TODO add logging
        println(s"User.load error: ${th.getMessage}")

        DefaultUser
    }
  }
}
