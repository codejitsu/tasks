// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

sealed trait OperationSystem
case object MacOS extends OperationSystem
case object Linux extends OperationSystem
case object Solaris extends OperationSystem
case object Windows extends OperationSystem
case object UnknownOs extends OperationSystem

/**
 * OS detection.
 */
object OS {
  def getCurrentOs(): OperationSystem = {
    val osName = System.getProperty("os.name").toLowerCase()

    if(osName.contains("linux")) {
      Linux
    } else if(osName.contains("windows")) {
      Windows
    } else if(osName.contains("solaris") || osName.contains("sunos")) {
      Solaris
    } else if(osName.contains("mac os") || osName.contains("macos") || osName.contains("darwin")) {
      MacOS
    } else {
      UnknownOs
    }
  }

  def isLinux(): Boolean = getCurrentOs() == Linux
}
