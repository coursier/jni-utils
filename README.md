# coursier-jni-utils

*coursier-jni-utils* is a small library allowing to tap into various Windows
native APIs from Java / Scala via JNI.


## Developer docs

*coursier-jni-utils* is built with
[Mill](https://com-lihaoyi.github.io/mill). It comes up with Mill launchers
(`mill`, `mill.bat`), so that only a JVM and [mingw](https://www.mingw-w64.org)
should be needed to build it.

### Requirements

#### JVM

A JVM, such as AdoptOpenJDK 8 or 11, is required.
To check if such a JVM is installed, run
`java -version`, and check that it prints a version
higher than or equal to `8`.

#### Visual C++

A Visual Studio installation with Visual C++ is required.

### IDE

[IntelliJ IDEA](https://www.jetbrains.com/idea) is the recommended IDE to
develop on jni-utils. Prior to opening the project with IDEA, run
```text
$ ./mill mill.scalalib.GenIdea/idea
```

Then open the `jni-utils` directory in IDEA.

### Compiling

```text
$ ./mill __.compile
```

This should automatically compile `.h` / `.c` files with mingw, create a
corresponding `.dll`, and make it available as a resource from Java.

### Running tests

Some simple tests can be run with
```text
$ ./mill __.test
```
