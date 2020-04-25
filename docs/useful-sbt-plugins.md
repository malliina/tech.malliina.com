---
title: Useful sbt plugins
date: 2020-04-25
---
# Useful sbt plugins

[sbt](https://www.scala-sbt.org/) is a build tool for Scala. This post lists sbt plugins I have found useful in many
projects.

## sbt-bloop

A quick feedback loop is essential to productivity. Get quick build feedback directly to your IDE with 
[Bloop](https://scalacenter.github.io/bloop/). To install:

```scala ignore
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.4.0-RC1")
```

Then run:

```scala ignore
sbt bloopInstall
``` 

Finally, import your Scala project as a BSP project instead of an sbt project in 
[IntelliJ IDEA](https://www.jetbrains.com/idea/). 

Bloop is fast and accurate, and removes the need to have a separate terminal window open for compilation purposes.

## sbt-scalafmt

Use [Scalafmt](https://scalameta.org/scalafmt/) to format your codebase consistently. To install:

```scala ignore
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.4")
```

I recommend using the *reformat on file save* option in [IntelliJ IDEA](https://www.jetbrains.com/idea/) as described 
in Scalafmt's [installation instructions](https://scalameta.org/scalafmt/docs/installation.html).

You can also format the entire codebase in one command with:

    sbt scalafmtAll

## sbt-buildinfo

You often want to use build metadata in your application. For example, your app may display the current 
version number, or the git hash from which the app was built. Use [sbt-buildinfo](https://github.com/sbt/sbt-buildinfo)
for that.

Installation:

```scala ignore
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
```

The plugin generates a Scala source file with an object that will be compiled with the rest of your code. You then
access the *BuildInfo* object to read the parameters that you wish to display in your app. Follow the instructions 
in [https://github.com/sbt/sbt-buildinfo](https://github.com/sbt/sbt-buildinfo) for details.

## sbt-mdoc

Use [sbt-mdoc](https://scalameta.org/mdoc/) to typecheck your Scala code samples embedded in Markdown documentation. 
The plugin can also inject build parameters to the documentation. This is useful when you want your documentation to
always refer to the latest version number of your software, for example in your Markdown-based installation instructions 
like README.md.

Installation:

```scala ignore
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.1.5")
```

Follow the instructions in [mdoc's installation instructions](https://scalameta.org/mdoc/docs/installation.html) to 
get started.

## Build your own plugin

If you find yourself repeating the same sbt configurations over multiple projects, consider refactoring your settings
 into an sbt plugin.

To get started, add an object to `project/MyPlugin.scala` that extends `sbt.AutoPlugin`:

```scala ignore
object MyPlugin extends AutoPlugin {
  object autoImport {
    val myGreeting = settingKey[String]("My greeting")
  }

  override def projectSettings: Seq[Setting[_]] = Seq(
    myGreeting := "Hello, World!"
  )
}
```

Now when you enable *MyPlugin* for your module in *build.sbt*, you can use the plugin's settings in your build:

```scala ignore
val project = project.in(file("."))
  .enablePlugins(MyPlugin)
  .settings(myGreeting := "Hello, You!")
```

Enjoy!
