package coursier.jniutils

object AnsiTerminal {
  def main(args: Array[String]): Unit = {
    val verbose = args match {
      case Array() => false
      case Array("-v" | "--verbose") => true
      case _ =>
        System.err.println("Usage: ansi-terminal [-v|--verbose]")
        sys.exit(1)
    }
    val changed = WindowsAnsiTerminal.enableAnsiOutput()
    if (verbose && changed)
      System.err.println("ANSI terminal enabled")
  }
}
