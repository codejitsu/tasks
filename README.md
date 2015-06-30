tasks
=====

Tasks is a library for building executable (shell)-scripts functional way. It is written in Scala.

Scripts can be executed locally on localhost or remote over ssh (or in memory if you don't have any system tool calls).

My `sbt` plugin `sbt-robot` (https://github.com/codejitsu/sbt-robot) uses `tasks` to define and install custom sbt tasks 
for projects built with `sbt` tool.

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

All tasks are composable with `andThan`-operator. The result is a new `Task`:

```scala
  val deployApp =
      RmIfExists(...) andThen
      Upload(...) andThen
      StopTomcat(...) andThen
      Mv(...) andThen
      StartTomcat(hosts) andThen
      Wait(5 seconds) andThen
      CheckUrl(...) andThen
      NotifyMessageOk
```

You can chain tasks with for comprehension like:

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

Error handling
--------------
 
Functional tasks composition is also short circuit upon error - when an error occurs during a task 
then the rest of the script logic isn't executed and the result is a type containing the first error encountered. 

Sudo
----
 
If a task requires `sudo`, you can use the `Sudo`-transformer for it:
  
```scala
val taskWithSudo = Sudo ~ StopTomcat(...)
```

Par
---
 
If a task consists of multiple independent steps (for example checking the same app monitoring url on several hosts) 
you can use the `Par`-transformer in order to run all that steps in parallel:
  
```scala
val taskWithSudoParallel = Sudo ~ Par ~ StopTomcat(...)
```

Only if all steps succeeded the task result is also `success`, otherwise the task result is `failure`.  
 
orElse
------

To make if/else-decisions use the `orElse`-operator:
 
```scala
val taskStartTomcat = Sudo ~ Par ~ StartTomcat(...) orElse NotifyMessageTomcatError 
``` 

The `NotifyMessageTomcatError`-subtask will be executed only if the `StartTomcat`-subtask failed.

Hosts
-----

Many tasks intended to run in parallel on several remote hosts (For example `Rm` task removes files on specified remote `Hosts`). 
To simplify such tasks there is a nice `Hosts` DSL: 

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

For `ssh`-access you have to specify user credentials in a `ssh`-properties file in `~/.ssh-tasks/ssh.properties`.

This file contains the following data:

    # simple config
    username = alice
    password = test
    keyfile = /home/alice/.ssh/key_rsa
    
`User.load` will automatically read this file and create a user.    
 

Compile time access control
--------------------------- 

All scripts are designed to run on some defined `stage`. The following stages are defined (you can define your own):

* `Dev`
* `Test`
* `QA`
* `Production`

You have to provide a implicit stage in your scripts:
 
```scala
implicit val stage = new Dev
```

If there is no explicit run permission for a task (or stage not defined), you get a compile time error like this:

    Error:(22, 14) Stage is not allowed to run task 'TestTask[S]'.
        TestTask().run()
                 ^

Each task can be explicitly allowed to run on some stage:  

```scala
implicit val stage = new Production

implicit val allowStopTomcatInProd: Production Allow StopTomcat[Production] = 
    new Allow[Production, StopTomcat[Production]]
```

All tasks from `net.codejitsu.tasks` package can be run on `Dev` stage per default.
 
Example script
--------------

```scala
  val hosts = "host-0" | (1 to 10) | ".my.hosting.net"

  implicit val user = User.load
  implicit val stage = new Dev
  
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
 