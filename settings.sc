import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version_mill0.9:0.1.1`
import $ivy.`org.codehaus.plexus:plexus-archiver:4.2.2`

import de.tobiasroeser.mill.vcs.version.VcsVersion
import mill._, scalalib._
import mill.scalalib.publish.PublishInfo
import org.codehaus.plexus.archiver.zip.ZipUnArchiver

import scala.concurrent.duration.Duration

trait GenerateHeaders extends JavaModule {
  def cDirectory = T{
    millSourcePath / "src" / "main" / "c"
  }
  def javacOptions = T{
    val dest = cDirectory()
    Seq("-h", dest.toNIO.toAbsolutePath.toString)
  }
  def compile = T{
    val res = super.compile()
    val dir = cDirectory()
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

private def vcVersions = Seq("2019", "2017")
private def vcEditions = Seq("Enterprise", "Community", "BuildTools")
private lazy val vcvarsCandidates = Option(System.getenv("VCVARSALL")) ++ {
  for {
    version <- vcVersions
    edition <- vcEditions
  } yield """C:\Program Files (x86)\Microsoft Visual Studio\""" + version + "\\" + edition + """\VC\Auxiliary\Build\vcvars64.bat"""
}

private def vcvarsOpt: Option[os.Path] =
  vcvarsCandidates
    .iterator
    .map(os.Path(_, os.pwd))
    .filter(os.exists(_))
    .toStream
    .headOption

private lazy val vcvars = vcvarsOpt.getOrElse {
  sys.error("vcvars64.bat not found. Ensure Visual Studio is installed, or put the vcvars64.bat path in VCVARSALL.")
}

private def q = "\""

trait HasCSources extends JavaModule with PublishModule {

  def windowsJavaHome: T[String]
  def dllName: T[String]

  def linkingLibs = T{ Seq.empty[String] }

  def cSources = T.sources {
    Seq(PathRef(millSourcePath / "src" / "main" / "c"))
  }
  def cCompile = T.persistent {
    val destDir = T.ctx().dest / "obj"
    val cFiles = cSources().flatMap { dir =>
      if (os.isDir(dir.path))
        os.walk(dir.path)
          .filter(p => os.isFile(p) && p.last.endsWith(".c"))
      else
        Nil
    }
    val javaHome0 = windowsJavaHome()
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
            |cl /I $q$javaHome0/include$q /I $q$javaHome0/include/win32$q /utf-8 /c $q$f$q
            |""".stripMargin
        val scriptPath = T.dest / "run-cl.bat"
        os.write.over(scriptPath, script.getBytes, createFolders = true)
        os.proc(scriptPath).call(cwd = destDir)
      }
      PathRef(output.resolveFrom(os.pwd))
    }
  }
  def cLib = T.persistent {
    val allObjFiles = cCompile().map(_.path)
    val fileName = "csjniutils.lib"
    val output = T.dest / fileName
    val libNeedsUpdate = !os.isFile(output) || allObjFiles.exists(f => os.mtime(output) < os.mtime(f))
    if (libNeedsUpdate) {
      val script =
       s"""@call "$vcvars"
          |if %errorlevel% neq 0 exit /b %errorlevel%
          |lib "/out:$fileName" ${allObjFiles.map(f => "\"" + f.toString + "\"").mkString(" ")}
          |""".stripMargin
      val scriptPath = T.dest / "run-lib.bat"
      os.write.over(scriptPath, script.getBytes, createFolders = true)
      os.proc(scriptPath).call(cwd = T.dest)
      if (!os.isFile(output))
        sys.error(s"Error: $output not created")
    }
    PathRef(output)
  }

  def dll = T.persistent {
    val dllName0 = dllName()
    val destDir = T.ctx().dest / "dlls"
    if (!os.exists(destDir))
      os.makeDir.all(destDir)
    val dest = destDir / s"$dllName0.dll"
    val relDest = dest.relativeTo(os.pwd)
    val objs = cCompile()
    val objsArgs = objs.map(o => o.path.relativeTo(os.pwd).toString).distinct
    val libsArgs = linkingLibs().map(l => l + ".lib")
    val needsUpdate = !os.isFile(dest) || {
      val destMtime = os.mtime(dest)
      objs.exists(o => os.mtime(o.path) > destMtime)
    }
    if (needsUpdate) {
      val libPath = Seq("C:", "Program Files (x86)", "Windows Kits", "10", "Lib", "10.0.19041.0", "um", "x64").mkString("\\")
      val script = //  $q/LIBPATH:$libPath$q
       s"""@call "$vcvars"
          |if %errorlevel% neq 0 exit /b %errorlevel%
          |link /DLL "/OUT:$dest" ${libsArgs.mkString(" ")} ${objs.map(f => "\"" + f.path.toString + "\"").mkString(" ")}
          |""".stripMargin
      val scriptPath = T.dest / "run-cl.bat"
      os.write.over(scriptPath, script.getBytes, createFolders = true)
      os.proc(scriptPath).call(cwd = T.dest)
    }
    PathRef(dest)
  }
  def resources = T.sources {
    val dll0 = dll().path
    val dir = T.ctx().dest / "dll-resources"
    val dllDir = dir / "META-INF" / "native" / "windows64"
    os.copy(dll0, dllDir / dll0.last, replaceExisting = true, createFolders = true)
    super.resources() ++ Seq(PathRef(dir))
  }

  def extraPublish = super.extraPublish() ++ Seq(
    PublishInfo(
      file = dll(),
      ivyConfig = "compile",
      classifier = Some("x86_64-pc-win32"),
      ext = "dll",
      ivyType = "dll"
    ),
    PublishInfo(
      file = cLib(),
      ivyConfig = "compile",
      classifier = Some("x86_64-pc-win32"),
      ext = "lib",
      ivyType = "lib"
    )
  )
}

trait JniUtilsPublishVersion extends Module {
  def publishVersion = T{
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

def publishSonatype(
  credentials: String,
  pgpPassword: String,
  data: Seq[PublishModule.PublishData],
  timeout: Duration,
  log: mill.api.Logger
): Unit = {

  val artifacts = data.map {
    case PublishModule.PublishData(a, s) =>
      (s.map { case (p, f) => (p.path, f) }, a)
  }

  val isRelease = {
    val versions = artifacts.map(_._2.version).toSet
    val set = versions.map(!_.endsWith("-SNAPSHOT"))
    assert(set.size == 1, s"Found both snapshot and non-snapshot versions: ${versions.toVector.sorted.mkString(", ")}")
    set.head
  }
  val publisher = new publish.SonatypePublisher(
               uri = "https://oss.sonatype.org/service/local",
       snapshotUri = "https://oss.sonatype.org/content/repositories/snapshots",
       credentials = credentials,
            signed = true,
           gpgArgs = Seq("--detach-sign", "--batch=true", "--yes", "--pinentry-mode", "loopback", "--passphrase", pgpPassword, "--armor", "--use-agent"),
       readTimeout = timeout.toMillis.toInt,
    connectTimeout = timeout.toMillis.toInt,
               log = log,
      awaitTimeout = timeout.toMillis.toInt,
    stagingRelease = isRelease
  )

  publisher.publishAll(isRelease, artifacts: _*)
}

trait WithDllNameJava extends JavaModule {
  def generatedSources = T{
    val f = T.ctx().dest / "coursier" / "jniutils" / "DllName.java"
    val dllName0 = dllName()
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
  def dllName = T{
    val ver = publishVersion()
    s"csjniutils-$ver"
  }
}
