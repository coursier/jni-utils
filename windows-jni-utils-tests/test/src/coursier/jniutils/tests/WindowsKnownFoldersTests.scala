package coursier.jniutils.tests

import coursier.jniutils.WindowsKnownFolders
import utest._

object WindowsKnownFoldersTests extends TestSuite {
  val tests = Tests {
    test("default") synchronized {
      coursier.jniutils.NativeApi.set(null);
      WindowsKnownFolders.knownFolderPath("{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}")
    }

    test("bootstrap") synchronized {
      coursier.bootstrap.launcher.jniutils.BootstrapNativeApi.setup()
      WindowsKnownFolders.knownFolderPath("{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}")
    }

    test("lmcoursier") synchronized {
      lmcoursier.internal.jniutils.LmNativeApi.setup()
      WindowsKnownFolders.knownFolderPath("{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}")
    }

    test("coursierapi") synchronized {
      coursierapi.internal.jniutils.ApiInternalNativeApi.setup()
      WindowsKnownFolders.knownFolderPath("{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}")
    }
  }
}
