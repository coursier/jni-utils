package coursier.jniutils

object KnownFolders {
  def main(args: Array[String]): Unit =
    for (arg <- args) {
      val path = WindowsKnownFolders.knownFolderPath("{" + arg + "}")
      println(path)
    }
}
