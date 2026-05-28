package millbuild

import mill.*, scalalib.*
import mill.scalalib.publish.PublishInfo
import mill.util.VcsVersion
import org.codehaus.plexus.archiver.zip.ZipUnArchiver

import java.util.Locale

trait GenerateHeaders extends JavaModule {
  def cDirectory = Task.Source("src/main/c")
  override def javacOptions = Task{
    val dest = cDirectory().path
    Seq("-h", dest.toNIO.toAbsolutePath.toString)
  }
  def compile = Task {
    val res = super.compile()
    val dir = cDirectory().path
    val headerFiles = Seq(dir).flatMap { dir =>
      if (os.isDir(dir))
        os.walk(dir)
          .filter(p => os.isFile(p) && p.last.endsWith(".h"))
      else
        Nil
    }
    for (f <- headerFiles) {
      val content = os.read.bytes(f)
      for (updatedContent <- toCrLfOpt(content))
        os.write.over(f, updatedContent)
    }
    res
  }
}

def toCrLfOpt(content: Array[Byte]): Option[Array[Byte]] = {
  val cr = '\r'.toByte
  val lf = '\n'.toByte
  val indices = content
    .iterator
    .zipWithIndex
    .collect {
      case (`lf`, idx) if idx > 0 && content(idx - 1) != cr => idx
    }
    .toVector
  if (indices.isEmpty)
    None
  else {
    val updatedContent = Array.ofDim[Byte](content.length + indices.length)
    var i = 0 // content index
    var j = 0 // updatedContent index
    var n = 0 // indices index
    while (n < indices.length) {
      val idx = indices(n)

      System.arraycopy(content, i, updatedContent, j, idx - i)
      j += idx - i
      i = idx

      updatedContent(j) = cr
      updatedContent(j + 1) = lf
      i += 1
      j += 2

      n += 1
    }

    val idx = content.length
    System.arraycopy(content, i, updatedContent, j, content.length - i)
    j += idx - i
    i = idx

    assert(i == content.length)
    assert(j == updatedContent.length)
    Some(updatedContent)
  }
}

private def isArm64: Boolean =
  sys.props.get("os.arch").map(_.toLowerCase(Locale.ROOT)).exists {
    case "aarch64" | "arm64" => true
    case _ => false
  }
private lazy val (windowsNativeResourceDir, windowsOtherNativeResourceDir) =
  if (isArm64) ("windows64-arm64", "windows64")
  else ("windows64", "windows64-arm64")
private lazy val (windowsClassifier, windowsOtherClassifier) =
  if (isArm64) ("arm64-pc-win32", "x86_64-pc-win32")
  else ("x86_64-pc-win32", "arm64-pc-win32")

private lazy val (platformArtifactsDirOpt, otherPlatformArtifactsDirOpt): (Option[os.Path], Option[os.Path]) = {
  val windowsX86_64ArtifactsDirOpt = Option(System.getenv("JNI_UTILS_WINDOWS_X86_64_ARTIFACTS_DIR"))
    .filter(_.nonEmpty)
    .map(os.Path(_, os.pwd))
  val windowsArm64ArtifactsDirOpt = Option(System.getenv("JNI_UTILS_WINDOWS_ARM64_ARTIFACTS_DIR"))
    .filter(_.nonEmpty)
    .map(os.Path(_, os.pwd))
  if (isArm64) (windowsArm64ArtifactsDirOpt, windowsX86_64ArtifactsDirOpt)
  else (windowsX86_64ArtifactsDirOpt, windowsArm64ArtifactsDirOpt)
}

private def vcVersions = Seq("18", "2022", "2019", "2017")
private def vcEditions = Seq("Enterprise", "Community", "BuildTools")
private def vcvarsBat =
  if (isArm64) "vcvarsarm64.bat" else "vcvars64.bat"
private def progFiles = Seq(
  """C:\Program Files""",
  """C:\Program Files (x86)"""
)
private lazy val vcvarsCandidates = Option(System.getenv("VCVARSALL")) ++ {
  for {
    progFiles0 <- progFiles
    version <- vcVersions
    edition <- vcEditions
  } yield progFiles0 + """\Microsoft Visual Studio\""" + version + "\\" + edition + """\VC\Auxiliary\Build\""" + vcvarsBat
}

private def vcvarsOpt: Option[os.Path] =
  vcvarsCandidates
    .iterator
    .map(os.Path(_, os.pwd))
    .find(os.exists)

private lazy val vcvars = vcvarsOpt.getOrElse {
  sys.error(s"$vcvarsBat not found. Ensure Visual Studio is installed, or put the $vcvarsBat path in VCVARSALL.")
}

private def q = "\""

trait HasCSources extends JavaModule with PublishModule {

  def windowsJavaHome: T[String]
  def baseDllName: T[String]
  def dllSuffix: T[String]

  def linkingLibs = Task(Seq.empty[String])

  def cSources = Task.Sources("src/main/c")
  def cCompile = Task(persistent = true) {
    val destDir = Task.dest / "obj"
    val cFiles = cSources().flatMap { dir =>
      if (os.isDir(dir.path))
        os.walk(dir.path)
          .filter(p => os.isFile(p) && p.last.endsWith(".c"))
      else
        Nil
    }
    val javaHome0 = os.Path(windowsJavaHome())
    for (f <- cFiles) yield {
      if (!os.exists(destDir))
        os.makeDir.all(destDir)
      val path = f.relativeTo(os.pwd).toString
      val output = destDir / s"${f.last.stripSuffix(".c")}.obj"
      val needsUpdate = !os.isFile(output) || os.mtime(output) < os.mtime(f)
      if (needsUpdate) {
        val script =
         s"""@call "$vcvars"
            |if %errorlevel% neq 0 exit /b %errorlevel%
            |cl /I $q${javaHome0 / "include"}$q /I $q${javaHome0 / "include/win32"}$q /utf-8 /c $q$f$q
            |""".stripMargin
        val scriptPath = Task.dest / "run-cl.bat"
        os.write.over(scriptPath, script.getBytes, createFolders = true)
        os.proc(scriptPath).call(cwd = destDir)
      }
      PathRef(output.resolveFrom(os.pwd))
    }
  }
  def cLib = platformArtifactsDirOpt match {
    case Some(platformArtifactsDir) =>
      Task {
        val path = platformArtifactsDir / s"${baseDllName()}.lib"
        if (os.isFile(path))
          PathRef(path)
        else
          Task.fail(s"$path not found")
      }
    case None =>
      Task(persistent = true) {
        val allObjFiles = cCompile().map(_.path)
        val fileName = s"${baseDllName()}.lib"
        val output = Task.dest / fileName
        val libNeedsUpdate = !os.isFile(output) || allObjFiles.exists(f => os.mtime(output) < os.mtime(f))
        if (libNeedsUpdate) {
          val script =
           s"""@call "$vcvars"
              |if %errorlevel% neq 0 exit /b %errorlevel%
              |lib "/out:$fileName" ${allObjFiles.map(f => "\"" + f.toString + "\"").mkString(" ")}
              |""".stripMargin
          val scriptPath = Task.dest / "run-lib.bat"
          os.write.over(scriptPath, script.getBytes, createFolders = true)
          os.proc(scriptPath).call(cwd = Task.dest)
          if (!os.isFile(output))
            sys.error(s"Error: $output not created")
        }
        PathRef(output)
      }
  }

  def dll = platformArtifactsDirOpt match {
    case Some(platformArtifactsDir) =>
      Task {
        val path = platformArtifactsDir / s"${baseDllName()}.dll"
        if (os.isFile(path))
          PathRef(path)
        else
          Task.fail(s"$path not found")
      }
    case None =>
      Task(persistent = true) {
        val destDir = Task.dest / "dlls"
        if (!os.exists(destDir))
          os.makeDir.all(destDir)
        val dest = destDir / s"${baseDllName()}.dll"
        val relDest = dest.relativeTo(os.pwd)
        val objs = cCompile()
        val objsArgs = objs.map(o => o.path.relativeTo(os.pwd).toString).distinct
        val libsArgs = linkingLibs().map(l => l + ".lib")
        val needsUpdate = !os.isFile(dest) || {
          val destMtime = os.mtime(dest)
          objs.exists(o => os.mtime(o.path) > destMtime)
        }
        if (needsUpdate) {
          val script =
           s"""@call "$vcvars"
              |if %errorlevel% neq 0 exit /b %errorlevel%
              |link /DLL "/OUT:$dest" ${libsArgs.mkString(" ")} ${objs.map(f => "\"" + f.path.toString + "\"").mkString(" ")}
              |""".stripMargin
          val scriptPath = Task.dest / "run-cl.bat"
          os.write.over(scriptPath, script.getBytes, createFolders = true)
          os.proc(scriptPath).call(cwd = Task.dest)
        }
        PathRef(dest)
      }
  }
  def resources = Task {
    val dll0 = dll().path
    val dllDir = Task.dest / "META-INF/native" / windowsNativeResourceDir
    val finalDllName = s"${baseDllName()}${dllSuffix()}.dll"
    os.copy(dll0, dllDir / finalDllName, replaceExisting = true, createFolders = true)
    for (otherDll <- otherPlatformArtifactsDirOpt.map(_ / s"${baseDllName()}.dll")) {
      val otherDir = Task.dest / "META-INF/native" / windowsOtherNativeResourceDir
      os.copy(otherDll, otherDir / finalDllName, replaceExisting = true, createFolders = true)
    }
    super.resources() ++ Seq(PathRef(Task.dest))
  }

  def extraPublish = Task {
    super.extraPublish() ++
    Seq(
      PublishInfo(
        file = dll(),
        ivyConfig = "compile",
        classifier = Some(windowsClassifier),
        ext = "dll",
        ivyType = "dll"
      ),
      PublishInfo(
        file = cLib(),
        ivyConfig = "compile",
        classifier = Some(windowsClassifier),
        ext = "lib",
        ivyType = "lib"
      )
    ) ++
    otherPlatformArtifactsDirOpt.map { dir =>
      PublishInfo(
        file = PathRef(dir / s"${baseDllName()}.dll"),
        ivyConfig = "compile",
        classifier = Some(windowsOtherClassifier),
        ext = "dll",
        ivyType = "dll"
      )
    } ++
    otherPlatformArtifactsDirOpt.map { dir =>
      PublishInfo(
        file = PathRef(dir / s"${baseDllName()}.lib"),
        ivyConfig = "compile",
        classifier = Some(windowsOtherClassifier),
        ext = "lib",
        ivyType = "lib"
      )
    }
  }
}

trait JniUtilsPublishVersion extends Module {
  def publishVersion = Task {
    val state = VcsVersion.vcsState()
    if (state.commitsSinceLastTag > 0) {
      val versionOrEmpty = state.lastTag
        .map(_.stripPrefix("v"))
        .map { tag =>
          val idx = tag.lastIndexOf(".")
          if (idx >= 0) tag.take(idx + 1) + (tag.drop(idx + 1).toInt + 1).toString + "-SNAPSHOT"
          else ""
        }
        .getOrElse("0.0.1-SNAPSHOT")
      Some(versionOrEmpty)
        .filter(_.nonEmpty)
        .getOrElse(state.format())
    } else
      state
        .lastTag
        .getOrElse(state.format())
        .stripPrefix("v")
  }
}

trait JniUtilsPublishModule extends PublishModule with JniUtilsPublishVersion {
  import mill.scalalib.publish._
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "io.get-coursier.jniutils",
    url = "https://github.com/coursier/jni-utils",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("coursier", "jni-utils"),
    developers = Seq(
      Developer("alexarchambault", "Alex Archambault","https://github.com/alexarchambault")
    )
  )
}

trait WithDllNameJava extends JavaModule {
  def generatedSources = Task {
    val f = Task.dest / "coursier/jniutils/DllName.java"
    val dllName0 = baseDllName() + dllSuffix()
    val content =
     s"""package coursier.jniutils;
        |
        |final class DllName {
        |  static String name = "$dllName0";
        |}
        |""".stripMargin
    os.write(f, content, createFolders = true)
    Seq(PathRef(f))
  }

  def publishVersion: T[String]
  def baseDllName = Task("csjniutils")
  def dllSuffix = Task("-" + publishVersion())
}
