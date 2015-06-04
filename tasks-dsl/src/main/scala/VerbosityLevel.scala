// Copyright (C) 2015, codejitsu.

package net.codejitsu.tasks.dsl

/**
 * Task verbosity level
 */
sealed trait VerbosityLevel

case object NoOutput extends VerbosityLevel
case object Verbose extends VerbosityLevel
case object FullOutput extends VerbosityLevel
