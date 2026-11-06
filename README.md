# coursier-jni-utils

*coursier-jni-utils* is a small library allowing to tap into various Windows
native APIs from Java / Scala via JNI.

[![Maven Central](https://img.shields.io/maven-central/v/io.get-coursier.jniutils/windows-jni-utils.svg)](https://central.sonatype.com/artifact/io.get-coursier.jniutils/windows-jni-utils)

It's used by [coursier](https://github.com/coursier/coursier) and related
projects, to enable ANSI output in the Windows console, get or update the user
environment variables, find well-known folders, etc.

## Usage

### Adding the dependency

The library is published to Maven Central, as
`io.get-coursier.jniutils:windows-jni-utils`. It's written in Java, and has no
dependencies.

With Mill:
```scala
mvn"io.get-coursier.jniutils:windows-jni-utils:0.4.0"
```

With sbt:
```scala
libraryDependencies += "io.get-coursier.jniutils" % "windows-jni-utils" % "0.4.0"
```

With Maven:
```xml
<dependency>
  <groupId>io.get-coursier.jniutils</groupId>
  <artifactId>windows-jni-utils</artifactId>
  <version>0.4.0</version>
</dependency>
```

### Supported platforms

- Windows only, on x86-64 and ARM64. The JAR embeds a DLL for both
  architectures, and picks the right one at runtime, based on `os.arch`.
- Java 8 or later.

On Linux or macOS, calling any method relying on native code fails, with an
`UnsatisfiedLinkError` when the DLL is loaded. Check the OS before calling
it, like
```java
if (System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("windows")) {
  // use coursier.jniutils.*
}
```

### API

All the classes below are in the `coursier.jniutils` package, and only have
static methods. The native library is loaded upon the first call to one of
them.

#### ANSI output and terminal size

```java
// Enables the processing of ANSI escape sequences in the current console.
// Returns whether the console mode needed to be changed.
boolean changed = WindowsAnsiTerminal.enableAnsiOutput();

// Size of the screen buffer of the current console, in characters
TerminalSize size = WindowsAnsiTerminal.terminalSize();
int width = size.getWidth();
int height = size.getHeight();
```

Note that `terminalSize` returns the size of the console *screen buffer*. Its
width is usually the one of the window, but its height includes the
scrollback, and can be much larger than the number of visible lines.

Both methods act on the standard output of the process, and throw an
`IOException` if it isn't a console (when it's redirected to a file or a pipe,
for example).

#### User environment variables

```java
String value = WindowsEnvironmentVariables.get("PATH"); // null if not set
WindowsEnvironmentVariables.set("FOO", "bar");
WindowsEnvironmentVariables.delete("FOO");
```

These read and write the *persistent* environment variables of the current
user, stored in the registry under `HKEY_CURRENT_USER\Environment` - not the
environment of the current process. Changes don't affect the current process
or already running ones. No `WM_SETTINGCHANGE` message is broadcast either, so
processes started from Explorer only see them once it reloads its environment
(after logging out and back in, typically). Values containing a `%` are stored as `REG_EXPAND_SZ`, and are read back
unexpanded. These methods throw an `IOException` if the registry can't be read
or written.

#### Known folders

```java
// FOLDERID_RoamingAppData
String appData = WindowsKnownFolders.knownFolderPath("{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}");
```

This calls
[`SHGetKnownFolderPath`](https://learn.microsoft.com/en-us/windows/win32/api/shlobj_core/nf-shlobj_core-shgetknownfolderpath).
The argument is a
[`KNOWNFOLDERID`](https://learn.microsoft.com/en-us/windows/win32/shell/knownfolderid)
GUID, *with* its surrounding braces. The method returns `null` if the GUID is
malformed or the folder can't be found.

#### Module file name

```java
// Path of the executable of the current process, like C:\...\bin\java.exe
String path = ModuleFileName.get();
```

From a GraalVM native image, this is the path of the native executable.

#### Code pages

```java
int oem = WindowsCodePages.oem();                  // like 437 or 850
Charset oemCharset = WindowsCodePages.oemCharset(); // like IBM437
Charset charset = WindowsCodePages.charsetFor(1252); // windows-1252
```

`oem()` returns the OEM code page of the system, which is the one consoles and
`cmd.exe` use by default, and which usually differs from the ANSI code page
that Java uses for `sun.jnu.encoding`. `charsetFor` and `oemCharset` return
`null` if the JVM has no charset for a code page. `charsetFor` doesn't rely on
native code, and can be called on any OS.

### GraalVM native image

The JARs ship with the native image configuration they need
(`META-INF/native-image`), so that the library works in GraalVM native images
without extra configuration.

Note that only the charsets included at build time are available in a native
image. Pass `-H:+AddAllCharsets` to `native-image` if `WindowsCodePages.charsetFor`
or `WindowsCodePages.oemCharset` need to resolve arbitrary code pages.

## How it works

### Modules

The `windows-jni-utils` module is the only one most users need. The other
modules are meant for coursier components that shade (relocate)
`windows-jni-utils`:

| Module                         | `NativeApi` implementation                              | Used by |
|--------------------------------|---------------------------------------------------------|---------|
| `windows-jni-utils`            | `coursier.jniutils.DefaultNativeApi`                    | everyone |
| `windows-jni-utils-bootstrap`  | `coursier.bootstrap.launcher.jniutils.BootstrapNativeApi` | coursier bootstrap launchers |
| `windows-jni-utils-lmcoursier` | `lmcoursier.internal.jniutils.LmNativeApi`              | [sbt-coursier / lm-coursier](https://github.com/coursier/sbt-coursier) |
| `windows-jni-utils-coursierapi`| `coursierapi.internal.jniutils.ApiInternalNativeApi`    | [coursier interface](https://github.com/coursier/interface) |

JNI binds `native` methods to C functions whose names are derived from the
fully qualified name of the Java class declaring them. When a project shades
`windows-jni-utils` under its own package, the `native` methods of
`DefaultNativeApi` end up in a class with a different name, and can't be bound
anymore. So each of those modules provides a `NativeApi` implementation, in a
package that the corresponding project doesn't relocate, whose `native`
methods are implemented by the same DLL as the default ones.

`NativeApi.get()` picks the implementation to use:
1. the one passed to `NativeApi.set(…)`, if any (the `setup()` method of the
   alternative implementations above calls it),
2. otherwise, the first of `BootstrapNativeApi`, `LmNativeApi`, and
   `ApiInternalNativeApi` that can be found in the class loader of `NativeApi`
   (looked up by name, so that these are picked automatically by the projects
   shading `windows-jni-utils` along with one of them),
3. otherwise, `DefaultNativeApi`.

### Loading the DLL

The name of the DLL includes the version of the library, like
`csjniutils-0.4.0.dll`, so that several versions of the library can be loaded
in the same JVM. Upon first use, `LoadWindowsLibrary.ensureInitialized()`
loads it from the first of these locations that has it:
1. a file with that name, directly on the class path,
2. the `META-INF/native/windows64` (or `windows64-arm64` on ARM64) resource
   embedded in the JAR. If the resource isn't a file, it's extracted to
   `<java.io.tmpdir>/coursier-jni-utils/<arch>/` (or to the directory in the
   `coursier.jniutils.dll-dir` Java property), and re-used by later runs,
3. `java.library.path`, via `System.loadLibrary`.

The JVM doesn't allow a given DLL file to be loaded by more than one class
loader. So that the different `NativeApi` implementations (and the shaded
copies of this library they go along with) can be used from different class
loaders of the same JVM, each of them loads the DLL from a file of its own:

| `NativeApi` implementation | DLL file                          |
|----------------------------|-----------------------------------|
| `DefaultNativeApi`         | `csjniutils-0.4.0.dll`            |
| `BootstrapNativeApi`       | `csjniutils-0.4.0-bootstrap.dll`  |
| `LmNativeApi`              | `csjniutils-0.4.0-lmcoursier.dll` |
| `ApiInternalNativeApi`     | `csjniutils-0.4.0-coursierapi.dll`|

The alternative implementations always extract the DLL to such a file (even
when it's found directly on the class path, or as a resource that is already a
file), and load it from there. Within a given class loader, the DLL is only
loaded once, by the first implementation to be initialized, and is then used
by all of them. Implementations other than the ones above can pick their own
file name by calling `LoadWindowsLibrary.ensureInitialized("<variant>")`.

Applications that already loaded the DLL by other means, or that link the
native code statically (in a GraalVM native image, say), can call
`LoadWindowsLibrary.assumeInitialized()` to skip that step.

Besides the JARs, the DLL and a static library (`.lib`) with the same native
code are published as separate artifacts of `windows-jni-utils`, with the
`x86_64-pc-win32` and `arm64-pc-win32` classifiers.

## Developer docs

*coursier-jni-utils* is built with [Mill](https://mill-build.org). It comes
with Mill launchers (`mill`, `mill.bat`), so that only a JVM and the Visual C++
compiler should be needed to build it.

The native library can only be built on Windows: building or testing the
project on Linux or macOS isn't supported.

### Requirements

#### Visual C++

A Visual Studio installation with Visual C++ is required. The build doesn't
need to be run from a Visual Studio developer prompt: it looks for
`vcvars64.bat` (or `vcvarsarm64.bat` on ARM64) by itself, in the default
installation directories of Visual Studio 18, 2022, 2019, and 2017 (Enterprise,
Community, or Build Tools editions), under both `C:\Program Files` and
`C:\Program Files (x86)`.

If Visual Studio is installed elsewhere, or to pick a specific installation,
put the path of the `vcvars` script to use in the `VCVARSALL` environment
variable, like
```text
> set VCVARSALL=D:\VisualStudio\2022\Community\VC\Auxiliary\Build\vcvars64.bat
```

### IDE

[IntelliJ IDEA](https://www.jetbrains.com/idea) is the recommended IDE to
develop on jni-utils. Prior to opening the project with IDEA, run
```text
$ ./mill mill.idea/
```

Then open the `jni-utils` directory in IDEA.

### Compiling

```text
$ ./mill __.compile
```

This compiles the Java sources. The `.c` files under
`windows-jni-utils/src/main/c` are compiled with Visual C++ when the resources
of `windows-jni-utils` are computed (when running tests, or packaging JARs).
The resulting DLL is added to the resources of `windows-jni-utils`, as
`META-INF/native/windows64/csjniutils-<version>.dll` (or under
`windows64-arm64` on ARM64), so that it can be loaded from the class path.

### JNI headers

The `.h` files under `windows-jni-utils/src/main/c` are generated by `javac -h`
from the Java classes with `native` methods. They're checked in, and
regenerated by the `headers.*` modules, which are compiled as part of
`windows-jni-utils.compile`. The generated headers are normalized to CRLF line
endings, like the checked in ones.

After adding, removing, or changing the signature of a `native` method,
compile the project and commit the updated `.h` files along with the change.

### Running tests

Tests can be run with
```text
$ ./mill __.test
```

The tests live in `windows-jni-utils-tests`. They check the code page /
charset mapping, and call the native APIs (module file name, known folders,
user environment variables) through each `NativeApi` implementation (default,
bootstrap, lmcoursier, coursierapi). They also load the DLL via each
implementation from class loaders of their own, to check that these don't
conflict.

Note that the environment variable tests write to the actual user environment
in the registry (`HKEY_CURRENT_USER\Environment`). They use randomly-named
`COURSIER_JNIUTILS_TEST_*` variables, and delete them afterwards.

On CI, the tests are run both on x86-64 and ARM64 Windows runners.

### Testing the published JARs

The tests can also be run against the published artifacts rather than against
the modules of the build. Package them in a local Maven repository archive,
then point `REPO_ARCHIVE` at it:
```text
$ ./mill ci.packageLocalM2 --output m2-repo.zip
$ REPO_ARCHIVE=m2-repo.zip ./mill __.test
```

### Continuous integration

The [CI workflow](.github/workflows/ci.yml):
1. builds the DLL and runs the tests on x86-64 and ARM64 Windows,
2. puts the DLLs of both architectures in the JARs, and packages everything in
   a local Maven repository archive (`ci.packageLocalM2`),
3. runs the tests again against that archive, on both architectures,
4. on pushes to `main` and on tags, publishes the artifacts to Maven Central,
   and to the GitHub Packages Maven repository of this repository.

To embed DLLs built elsewhere (like on the runner of the other architecture)
rather than building them, set `JNI_UTILS_WINDOWS_X86_64_ARTIFACTS_DIR` and / or
`JNI_UTILS_WINDOWS_ARM64_ARTIFACTS_DIR` to directories containing
`csjniutils.dll` and `csjniutils.lib` (as written by
`ci.collectNativeArtifacts`). The DLL of the current architecture is then taken
from there rather than built, and the one of the other architecture is added
to the JAR (and published) too.

### Releasing

The version is computed from git tags:
- if `HEAD` has a `v*` tag, like `v0.4.0`, the version is the tag without its
  `v` (`0.4.0`),
- otherwise, it's the next patch version after the latest `v*` tag, as a
  snapshot (`0.4.1-SNAPSHOT`).

Each push to `main` publishes a snapshot. To cut a release, push a `v*` tag
(like `v0.4.1`) on a commit of `main`: CI then uploads that version to Sonatype
Central, as a `coursier-jni-utils-0.4.1` deployment. That deployment isn't
released automatically: publish it by hand from
<https://central.sonatype.com/publishing/deployments> once it's validated.

## License

*coursier-jni-utils* is licensed under the Apache License, version 2.0. See
[`LICENSE`](LICENSE).
