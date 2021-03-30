package coursier.jniutils

import coursier.jniutils.windowsenvironmentvariables.WindowsEnvironmentVariables

object EnvironmentVariable {
  def main(args: Array[String]): Unit =
    args match {
      case Array(key) =>
        val value = WindowsEnvironmentVariables.get(key)
        println(value)
      case Array(key, value) =>
        WindowsEnvironmentVariables.set(key, value)
      case _ =>
        System.err.println("Usage: environment-variable key [value]")
        sys.exit(1)
    }
}
