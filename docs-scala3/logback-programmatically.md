---
title: Logback programmatically
cls: logback
date: 2024-08-10
---
# Logback programmatically

[Logback](https://logback.qos.ch/manual/index.html "Logback website") is a logging library for the JVM. It is 
conventional to [configure](https://logback.qos.ch/manual/configuration.html "Logback configuration")
logback using *logback.xml* configuration files. However, logback can also be configured programmatically. Here
are some benefits of doing so:

- Because the configuration is code, it's easier to reuse common logback configurations across applications.
- Better compile-time checks for configuration errors.
- You can use your favorite IDE to discover the available logback API and make changes.
- Applying different logging settings for different app environments (test, dev, prod, etc.) is easily done in code, compared to making sure the correct XML file is applied at runtime.
- There is no context switching between coding your application and tweaking XML for logback.

This post shows how to configure Logback programmatically.

## Configuring

Add the following dependency to your application:

```scala ignore
libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.5.6"
)
```

Consider the following utility methods to configure logback programmatically:

```scala mdoc:silent
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{AsyncAppender, Level, Logger, LoggerContext}
import ch.qos.logback.core.{Appender, ConsoleAppender, CoreConstants}
import org.slf4j.LoggerFactory

import java.util
import scala.reflect.{ClassTag, classTag}

object LogbackUtils:
  def init(
    pattern: String = """%d{HH:mm:ss.SSS} %-5level %logger{72} %msg%n""",
    rootLevel: Level = Level.INFO,
    levelsByLogger: Map[String, Level] = Map.empty
  ): LoggerContext =
    val lc = loggerContext
    lc.reset()
    val ple = PatternLayoutEncoder()
    ple.setPattern(pattern)
    ple.setContext(lc)
    ple.start()
    val console = new ConsoleAppender[ILoggingEvent]()
    console.setContext(loggerContext)
    console.setEncoder(ple)
    if !console.isStarted then console.start()
    val appender = new AsyncAppender
    appender.setContext(LogbackUtils.loggerContext)
    appender.setName("ASYNC")
    appender.addAppender(console)
    installAppender(appender)
    val root = lc.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    root.setLevel(rootLevel)
    levelsByLogger.foreach: (loggerName, level) =>
      lc.getLogger(loggerName).setLevel(level)
    lc

  def appender[T](
    appenderName: String,
    loggerName: String = org.slf4j.Logger.ROOT_LOGGER_NAME
  ): Option[T] =
    Option(
      LoggerFactory
        .getLogger(loggerName)
        .asInstanceOf[Logger]
        .getAppender(appenderName)
        .asInstanceOf[T]
    )

  def getAppender[T](appenderName: String, loggerName: String = "ROOT"): T =
    appender[T](appenderName, loggerName)
      .getOrElse(
        throw new NoSuchElementException(
          s"Unable to find appender with name: $appenderName"
        )
      )

  def installAppender(
    appender: Appender[ILoggingEvent],
    loggerName: String = org.slf4j.Logger.ROOT_LOGGER_NAME
  ): Unit =
    if appender.getContext == null then appender.setContext(loggerContext)
    if !appender.isStarted then appender.start()
    val logger = LoggerFactory.getLogger(loggerName).asInstanceOf[Logger]
    logger.addAppender(appender)

  def installConverter[T <: ClassicConverter: ClassTag](
    conversionWord: String
  ): Unit =
    val lc = loggerContext
    val map = Option(lc.getObject(CoreConstants.PATTERN_RULE_REGISTRY))
      .map(_.asInstanceOf[util.HashMap[String, String]])
      .getOrElse(new util.HashMap())
    map.put(conversionWord, classTag[T].runtimeClass.getName)
    lc.putObject(CoreConstants.PATTERN_RULE_REGISTRY, map)

  def loggerContext = LoggerFactory
    .getILoggerFactory
    .asInstanceOf[LoggerContext]
```

The *init* method initializes a baseline logback configuration that:

- Logs to the console.
- Uses the *INFO* level for the root logger.

Your application should call *LogbackUtils.init()* immediately upon app startup.

This is similar to the default logback.xml presented in the official logback [configuration guide](https://logback.qos.ch/manual/configuration.html).
You may wish to customize the implementation, provide custom log levels per logger in the *levelsByLogger* parameter, or use method *installAppender* to add further appenders.

## Advanced usage

It is common to convert a message or stacktrace before it is written. For example, a stack trace typically contains 
multiple lines, but this may be inconvenient if your log management solution ingests one line per log event. In that 
case you may wish to remove any newlines from the messages.

Here are two logback converters that replace any sequence of whitespace with a simple space from the log message and 
stacktrace, respectively:

```scala mdoc:silent
import ch.qos.logback.classic.pattern.{
  MessageConverter, ThrowableProxyConverter
}
import ch.qos.logback.classic.spi.{ILoggingEvent, IThrowableProxy}

class WhitespaceConverter extends MessageConverter:
  override def convert(event: ILoggingEvent): String =
    val msg = super.convert(event)
    msg.trim.replaceAll("\\s+", " ")

class StacktraceConverter extends ThrowableProxyConverter:
  override def throwableProxyToString(tp: IThrowableProxy): String =
    super.throwableProxyToString(tp).trim.replaceAll("\\s+", " ")
```

Applying these converters to your logback configuration programmatically goes as follows:

```scala mdoc:silent
LogbackUtils.installConverter[WhitespaceConverter]("oneLine")
LogbackUtils.installConverter[StacktraceConverter]("ex")
```

Observe the *oneLine* and *ex* conversion words given as parameters. With these converters installed, 
you can reference them in your log pattern using the conversion word:

```scala mdoc:compile-only
LogbackUtils.init(pattern = 
  """%d{HH:mm:ss.SSS} %-5level %logger{72} %thread %oneLine %ex%n"""
)
```

## Conclusion

This post has shown how to configure logback programmatically, without using any XML files. Enjoy!
