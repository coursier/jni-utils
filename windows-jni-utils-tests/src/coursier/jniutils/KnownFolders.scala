package coursier.jniutils

import coursier.jniutils.windowsknownfolders.WindowsKnownFolders

object KnownFolders {
  def main(args: Array[String]): Unit =
    for (arg <- args) {
      val path = WindowsKnownFolders.knownFolderPath("{" + arg + "}")
      println(path)
    }
}
