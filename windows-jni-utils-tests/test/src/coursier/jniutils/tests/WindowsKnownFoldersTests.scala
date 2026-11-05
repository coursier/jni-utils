package coursier.jniutils.tests

import coursier.jniutils.WindowsKnownFolders
import utest._

import java.nio.file.{Files, Paths}

object WindowsKnownFoldersTests extends TestSuite {

  // FOLDERID_RoamingAppData
  private val roamingAppData = "{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}"

  private def check(): Unit = {
    val path = WindowsKnownFolders.knownFolderPath(roamingAppData)
    assert(path != null)
    assert(Files.isDirectory(Paths.get(path)))

    assert(WindowsKnownFolders.knownFolderPath("not-a-guid") == null)
  }

  val tests = Tests {
    test("default") synchronized {
      coursier.jniutils.NativeApi.set(null)
      check()
    }

    test("bootstrap") synchronized {
      coursier.bootstrap.launcher.jniutils.BootstrapNativeApi.setup()
      check()
    }

    test("lmcoursier") synchronized {
      lmcoursier.internal.jniutils.LmNativeApi.setup()
      check()
    }

    test("coursierapi") synchronized {
      coursierapi.internal.jniutils.ApiInternalNativeApi.setup()
      check()
    }

    test("invalidGuid") synchronized {
      coursier.jniutils.NativeApi.set(null)
      assert(WindowsKnownFolders.knownFolderPath(null) == null)
      assert(WindowsKnownFolders.knownFolderPath("not-a-guid") == null)
      assert(WindowsKnownFolders.knownFolderPath("{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D") == null)
      // Well-formed, but not a known folder ID: this is SHGetKnownFolderPath failing, not
      // CLSIDFromString.
      assert(WindowsKnownFolders.knownFolderPath("{00000000-0000-0000-0000-000000000000}") == null)
    }
  }
}
