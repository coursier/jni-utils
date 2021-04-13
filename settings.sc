import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version_mill0.9:0.1.1`
import $ivy.`org.codehaus.plexus:plexus-archiver:4.2.2`

import de.tobiasroeser.mill.vcs.version.VcsVersion
import mill._, scalalib._
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

lazy val isWindows = System.getProperty("os.name")
  .toLowerCase(java.util.Locale.ROOT)
  .contains("windows")

trait HasCSources extends JavaModule {

  def windowsJavaHome: T[String]
  def dllName: T[String]

  def linkingLibs = T{ Seq.empty[String] }

  def msysShell: Seq[String]
  def gcc: Seq[String]

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
    val escapedJavaHome =
      if (isWindows) "/" + javaHome0.replace("\\", "/")
      else javaHome0
    for (f <- cFiles) yield {
      if (!os.exists(destDir))
        os.makeDir.all(destDir)
      val path = f.relativeTo(os.pwd).toString
      val output = destDir / s"${f.last.stripSuffix(".c")}.o"
      val needsUpdate = !os.isFile(output) || os.mtime(output) < os.mtime(f)
      if (needsUpdate) {
        val relOutput = output.relativeTo(os.pwd)
        val q = "\""
        val command =
          if (isWindows) Seq(s"${gcc.mkString(" ")} -c -Wall -fPIC $q-I$escapedJavaHome/include$q $q-I$escapedJavaHome/include/win32$q $q$path$q -o $relOutput")
          else gcc ++ Seq("-c", "-Wall", "-fPIC", s"-I$escapedJavaHome/include", s"-I$escapedJavaHome/include/win32", path, "-o", relOutput.toString)
        System.err.println(s"Running ${command.mkString(" ")}")
        val res = os
          .proc((msysShell ++ command).map(x => x: os.Shellable): _*)
          .call(stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)
        if (res.exitCode != 0)
          sys.error(s"${gcc.mkString(" ")} command exited with code ${res.exitCode}")
      }
      PathRef(output.resolveFrom(os.pwd))
    }
  }
  def dll = T.persistent {
    val dllName0 = dllName()
    val destDir = T.ctx().dest / "dlls"
    if (!os.exists(destDir))
      os.makeDir.all(destDir)
    val dest = destDir / s"$dllName0.dll"
    val relDest = dest.relativeTo(os.pwd)
    val objs = cCompile()
    val q = "\""
    val objsArgs = objs.map(o => o.path.relativeTo(os.pwd).toString).distinct
    val libsArgs = linkingLibs().map(l => "-l" + l)
    val needsUpdate = !os.isFile(dest) || {
      val destMtime = os.mtime(dest)
      objs.exists(o => os.mtime(o.path) > destMtime)
    }
    if (needsUpdate) {
      val command =
        if (isWindows) Seq(s"${gcc.mkString(" ")} -s -shared -o $q$relDest$q ${objsArgs.map(o => q + o + q).mkString(" ")} -municode ${libsArgs.map(l => q + l + q).mkString(" ")}")
        else gcc ++ Seq("-s", "-shared", "-o", relDest.toString) ++ objsArgs ++ Seq("-municode") ++ libsArgs
      System.err.println(s"Running ${command.mkString(" ")}")
      val res = os
        .proc((msysShell ++ command).map(x => x: os.Shellable): _*)
        .call(stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)
      if (res.exitCode != 0)
        sys.error(s"${gcc.mkString(" ")} command exited with code ${res.exitCode}")
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
}

def downloadWindowsJvmArchive(windowsJvmUrl: String, windowsJvmArchiveName: String)(implicit ctx: mill.util.Ctx.Dest) = {
  val destDir = ctx.dest / "download"
  val dest = destDir / windowsJvmArchiveName
  if (!os.isFile(dest)) {
    os.makeDir.all(destDir)
    val tmpDest = destDir / s"$windowsJvmArchiveName.part"
    mill.modules.Util.download(windowsJvmUrl, tmpDest.relativeTo(ctx.dest))
    os.move(tmpDest, dest)
  }
  PathRef(dest)
}

def unpackWindowsJvmArchive(windowsJvmArchive: os.Path, windowsJvmArchiveName: String)(implicit ctx: mill.util.Ctx.Dest): os.Path = {
  val destDir = ctx.dest / windowsJvmArchiveName.stripSuffix(".zip")
  if (!os.isDir(destDir)) {
    val tmpDir = ctx.dest / (windowsJvmArchiveName.stripSuffix(".zip") + ".part")

    val unArchiver = {
      val u = new ZipUnArchiver
      u.enableLogging {
        import org.codehaus.plexus.logging.{AbstractLogger, Logger}
        new AbstractLogger(Logger.LEVEL_DISABLED, "foo") {
          def debug(message: String, throwable: Throwable) = ()
          def info(message: String, throwable: Throwable) = ()
          def warn(message: String, throwable: Throwable) = ()
          def error(message: String, throwable: Throwable) = ()
          def fatalError(message: String, throwable: Throwable) = ()
          def getChildLogger(name: String) = this
        }
      }
      u.setOverwrite(false)
      u
    }
    unArchiver.setSourceFile(windowsJvmArchive.toIO)
    unArchiver.setDestDirectory(tmpDir.toIO)

    os.makeDir.all(tmpDir)
    unArchiver.extract()
    os.move(tmpDir, destDir)
  }
  os.list(destDir).head
}

trait JniUtilsPublishModule extends PublishModule {
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
