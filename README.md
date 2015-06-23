tasks
=====

Tasks is a library for building executable (shell)-scripts functional way. It is written in Scala.

Scripts can be executed locally on localhost or remote over ssh.

My `sbt` plugin `sbt-robot` (https://github.com/codejitsu/sbt-robot) uses `tasks` to install custom sbt tasks.

Usage
-----

There are some predefined tasks for Linux shell-commands like:

* `Mkdir` - create directory
* `Touch` - create file 
* `Rm` - remove file/directory
* `Cp` - copy file/directory
* `Mv` - move file/directory
* `Upload` - upload file to remote/local server(s)
* `Download` - download file on remote/local server(s)
* `CheckUrl` - check url response 
* `GetRequest` - make HTTP GET request
* `PostRequest` - make HTTP GET request
* `InstallDeb` - install Debian package
* `StartService` - start Linux service
* `StopService` - stop Linux service
* `StartTomcat` - start Tomcat
* `StopTomcat` - stop Tomcat
* `Wait` - wait for some time

You can define your custom tasks (see the `GenericTask` class).

All tasks are composable with `andThan`-operator. The result is the new `Task`-object:

```scala
  val deployTomcats =
      RmIfExists(...) andThen
      Upload(...) andThen
      StopTomcat(...) andThen
      Mv(...) andThen
      StartTomcat(hosts) andThen
      Wait(5 seconds) andThen
      CheckUrl(...) andThen
      NotifyMessageOk
```

You can chain tasks with for comprehension:

```scala
  val deployTomcats = for {
      _ <- RmIfExists(...)
      _ <- Upload(...)
      _ <- StopTomcat(...)
      _ <- Mv(...)
      _ <- StartTomcat(...)
      _ <- Wait(5 seconds)
      _ <- CheckUrl(...)
      res <- NotifyMessageOk
  } yield res
```

To start a task, call the `run`-method. 

Sudo
----
 
If a task requires `sudo`, you can use the `Sudo`-transformer for it:
  
```scala
val taskWithSudo = Sudo ~ StopTomcat(...)
```

Par
---
 
If a task consists of multiple independent steps (for example checking the same url on several hosts) you can use the `Par`-transformer
in order to run all that steps in parallel:
  
```scala
val taskWithSudoParallel = Sudo ~ Par ~ StopTomcat(...)
```

Only if all steps succeeded the task result is also success, otherwise the task result is failure.  
 
OrElse
------

To make a simple if/else-decisions you can use the `orElse`-transformer:
 
```scala
val taskStartTomcat = Sudo ~ Par ~ StartTomcat(...) orElse NotifyMessageError 
``` 

The `NotifyMessageError`-subtask will be called if the `StartTomcat` failed.

Hosts
-----

Tasks library provides a simple host definition DSL:

```scala
import net.codejitsu.tasks.dsl.Tasks._

val hostsWithPattern = "test-host" | (1 to 25) | ".my.net.com"
// test-host1.my.net.com, test-host2.my.net.com, ..., test-host25.my.net.com
```

You can use tuples and sequences: 

```scala
import net.codejitsu.tasks.dsl.Tasks._

val hostsWithPattern = "test-host" | ((1, 'z', "abc")) | ".my.net.com"
// test-host1.my.net.com, test-hostz.my.net.com, test-hostabc.my.net.com
```

You can concatenate path parts with `~`-operator (all parts will be concatenated with `.`):

```scala
import net.codejitsu.tasks.dsl.Tasks._

val hostsWithPatterns = "test-host" ~ (1 to 2) ~ "my" ~ ("com", "net")
// test-host.1.my.net, test-host.2.my.net, test-host.1.my.com, test-host.2.my.com
```

Single hosts can be created with `.h`:
 
```scala
val myHost = "my.host.net".h
``` 

Users
-----

For ssh-access you have to specify user credentials in a `ssh`-property file in `~/.ssh-tasks/ssh.properties`.

This file contains the following data:

    # simple config
    username = alice
    password = test
    keyfile = /home/alice/.ssh/key_rsa
    
`User.load` will automatically read this file and create a user.    
 
Example script
--------------

```scala
  val hosts = "host-0" | (1 to 10) | ".my.hosting.net"

  implicit val user = User.load

  val artifact = s"$name-$version.war"
  val webapps = "/tomcat7/webapps"

  val NotifyDeploymentOk = PostRequest(...)

  val NotifyDeploymentFail = PostRequest(...)

  val deployApp =
      Par ~ RmIfExists(hosts, user.home / s"$artifact*") andThen
      Par ~ Upload(hosts, targetPath / artifact, user.home) andThen
      Sudo ~ Par ~ StopTomcat(hosts) andThen
      Sudo ~ Par ~ RmIfExists(hosts, webapps / s"$name*") andThen
      Sudo ~ Par ~ Mv(hosts, user.home / artifact, webapps) andThen
      Sudo ~ Par ~ StartTomcat(hosts) andThen
      Wait(5 seconds) andThen
      Par ~ CheckUrl(hosts, s"/$name-$version/example/check/", 8080, _.contains("OK")) andThen
      NotifyDeploymentOk orElse 
      NotifyDeploymentFail
```
 