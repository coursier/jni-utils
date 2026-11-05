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
  }
}
