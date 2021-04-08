package coursier.jniutils.tests

import coursier.jniutils.WindowsKnownFolders
import utest._

object WindowsKnownFoldersTests extends TestSuite {
  val tests = Tests {
    test {
      WindowsKnownFolders.knownFolderPath("{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}")
    }
  }
}
