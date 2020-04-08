# Intro

Hi, @NAME@! This is version @VERSION@.

## Code

```scala mdoc:invisible
import java.nio.file.Paths
import scala.concurrent.Future
```

Here's some code:

```scala
val a = 42
val file = Paths.get("hmm.txt")
// This is a Future
val fut: Future[String] = Future.successful("Yes.")
```

Right!
