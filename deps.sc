import $file.settings, settings.isWindows
import mill._, scalalib._

object Deps {
  def svm = ivy"org.graalvm.nativeimage:svm:21.0.0.2"
  def utest = ivy"com.lihaoyi::utest:0.7.5"
}

object Scala {
  def scala213 = "2.13.5"
}

object WindowsJvm {
  val url = "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.10%2B9/OpenJDK11U-jdk_x64_windows_hotspot_11.0.10_9.zip"
  val archiveName = url.drop(url.lastIndexOf('/') + 1)
}

object MingwCommands {

  def msysShell =
    if (isWindows) {
      def msys2Entrypoint = "C:/msys64/msys2_shell.cmd"
      if (!os.isFile(os.Path(msys2Entrypoint))) {
        sys.error(s"$msys2Entrypoint not found, adjust the msys2 entrypoint path in settings.sc")
      }
      Seq(msys2Entrypoint, "-defterm", "-here", "-no-start", "-mingw64", "-c")
    }
    else Nil

  def gcc = Seq(if (isWindows) "gcc" else "x86_64-w64-mingw32-gcc")

}
