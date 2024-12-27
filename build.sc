import $file.deps, deps.{Deps, Scala, WindowsJvm}
import $file.settings, settings.{GenerateHeaders, HasCSources, JniUtilsPublishModule, JniUtilsPublishVersion, WithDllNameJava}

import mill._, scalalib._

import scala.concurrent.duration._


object `windows-jni-utils` extends MavenModule with JniUtilsPublishVersion with HasCSources with JniUtilsPublishModule with WithDllNameJava {
  def linkingLibs = Seq("ole32", "shell32", "advapi32", "kernel32")

  def compile = T{
    headers.`windows-jni-utils`.compile()
    headers.`windows-jni-utils-bootstrap`.compile()
    headers.`windows-jni-utils-lmcoursier`.compile()
    headers.`windows-jni-utils-coursierapi`.compile()
    super.compile()
  }

  def windowsJavaHome = T{
    import java.io.File
    val value = sys.props("java.home")
    val dir = new File(value)
    // Seems required with Java 8
    if (dir.getName == "jre") dir.getParent
    else value
  }

  def javacOptions = super.javacOptions() ++ Seq(
    "--release", "8"
  )
}

object `windows-jni-utils-graalvm` extends WindowsUtils with JniUtilsPublishModule {
  def moduleDeps = Seq(`windows-jni-utils`)
}

object `windows-jni-utils-bootstrap` extends WindowsUtils with JniUtilsPublishModule {
  def moduleDeps = Seq(`windows-jni-utils`)
}

object `windows-jni-utils-lmcoursier` extends WindowsUtils with JniUtilsPublishModule {
  def moduleDeps = Seq(`windows-jni-utils`)
}

object `windows-jni-utils-coursierapi` extends WindowsUtils with JniUtilsPublishModule {
  def moduleDeps = Seq(`windows-jni-utils`)
}

object `windows-jni-utils-tests` extends ScalaModule with JniUtilsPublishModule {
  def scalaVersion = Scala.scala213
  def moduleDeps = Seq(`windows-jni-utils`)
  object test extends ScalaTests {
    def moduleDeps = super.moduleDeps ++ Seq(
      `windows-jni-utils-bootstrap`,
      `windows-jni-utils-lmcoursier`,
      `windows-jni-utils-coursierapi`
    )
    def ivyDeps = Agg(Deps.utest)
    def testFramework = "utest.runner.Framework"
  }
}

// compile these projects to generate or update JNI header files
object headers extends Module {
  object `windows-jni-utils` extends WindowsUtils with GenerateHeaders with WithDllNameJava
  object `windows-jni-utils-bootstrap` extends WindowsUtils with GenerateHeaders {
    def moduleDeps = Seq(`windows-jni-utils`)
    def cDirectory = `windows-jni-utils`.cDirectory()
  }
  object `windows-jni-utils-lmcoursier` extends WindowsUtils with GenerateHeaders {
    def moduleDeps = Seq(`windows-jni-utils`)
    def cDirectory = `windows-jni-utils`.cDirectory()
  }
  object `windows-jni-utils-coursierapi` extends WindowsUtils with GenerateHeaders {
    def moduleDeps = Seq(`windows-jni-utils`)
    def cDirectory = `windows-jni-utils`.cDirectory()
  }

  implicit def millModuleBasePath: define.Ctx.BasePath =
    define.Ctx.BasePath(super.millModuleBasePath.value / os.up)
}

trait WindowsUtils extends MavenModule with JniUtilsPublishVersion {
  def compileIvyDeps = Agg(Deps.svm)
  def javacOptions = super.javacOptions() ++ Seq(
    "--release", "8"
  )
}

def publishSonatype(tasks: mill.main.Tasks[PublishModule.PublishData]) =
  T.command {
    val timeout = 10.minutes
    val credentials = sys.env("SONATYPE_USERNAME") + ":" + sys.env("SONATYPE_PASSWORD")
    val pgpPassword = sys.env("PGP_PASSWORD")
    val data = define.Task.sequence(tasks.value)()

    settings.publishSonatype(
      credentials = credentials,
      pgpPassword = pgpPassword,
      data = data,
      timeout = timeout,
      log = T.ctx().log,
      workspace = T.workspace,
      env = T.env
    )
  }
