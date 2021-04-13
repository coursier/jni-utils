package coursier.jniutils

object EnvironmentVariable {
  def main(args: Array[String]): Unit =
    args match {
      case Array(key) if key.startsWith("-") =>
        val key0 = key.stripPrefix("-")
        WindowsEnvironmentVariables.delete(key0)
      case Array(key) =>
        val value = Option(WindowsEnvironmentVariables.get(key)).getOrElse("")
        println(value)
      case Array(key, value) =>
        WindowsEnvironmentVariables.set(key, value)
      case _ =>
        System.err.println("Usage: environment-variable key [value]")
        sys.exit(1)
    }
}
