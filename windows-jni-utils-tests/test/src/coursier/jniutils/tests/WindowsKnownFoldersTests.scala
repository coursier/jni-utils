package coursier.jniutils.tests

import coursier.jniutils.WindowsKnownFolders
import utest._

import java.nio.file.{Files, Paths}

object WindowsKnownFoldersTests extends TestSuite {

  // FOLDERID_RoamingAppData, spelled out rather than taken from WindowsKnownFolders
  private val roamingAppData = "{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}"

  // Folders that exist on any Windows install the tests run on
  private val existingFolders = Seq(
    "ROAMING_APP_DATA"   -> WindowsKnownFolders.ROAMING_APP_DATA,
    "LOCAL_APP_DATA"     -> WindowsKnownFolders.LOCAL_APP_DATA,
    "LOCAL_APP_DATA_LOW" -> WindowsKnownFolders.LOCAL_APP_DATA_LOW,
    "PROGRAM_DATA"       -> WindowsKnownFolders.PROGRAM_DATA,
    "PROFILE"            -> WindowsKnownFolders.PROFILE,
    "DESKTOP"            -> WindowsKnownFolders.DESKTOP,
    "DOCUMENTS"          -> WindowsKnownFolders.DOCUMENTS,
    "DOWNLOADS"          -> WindowsKnownFolders.DOWNLOADS,
    "PROGRAM_FILES"      -> WindowsKnownFolders.PROGRAM_FILES,
    "SYSTEM"             -> WindowsKnownFolders.SYSTEM,
    "WINDOWS"            -> WindowsKnownFolders.WINDOWS,
    "STARTUP"            -> WindowsKnownFolders.STARTUP,
    "START_MENU"         -> WindowsKnownFolders.START_MENU,
    "PROGRAMS"           -> WindowsKnownFolders.PROGRAMS
  )

  private def check(): Unit = {
    val path = WindowsKnownFolders.knownFolderPath(roamingAppData)
    assert(path != null)
    assert(Files.isDirectory(Paths.get(path)))

    assert(WindowsKnownFolders.knownFolderPath("not-a-guid") == null)
  }

  private def samePath(path: String, envVar: String): Unit = {
    val expected = System.getenv(envVar)
    assert(expected != null)
    assert(path != null)
    assert(Paths.get(path).toRealPath() == Paths.get(expected).toRealPath())
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

    test("constants") synchronized {
      coursier.jniutils.NativeApi.set(null)
      val failures = existingFolders.flatMap { case (name, rfid) =>
        val path = WindowsKnownFolders.knownFolderPath(rfid)
        if (path == null) Seq(s"$name: no path")
        else if (!Files.isDirectory(Paths.get(path))) Seq(s"$name: $path is not a directory")
        else Nil
      }
      if (failures.nonEmpty)
        throw new java.lang.AssertionError(failures.mkString("\n", "\n", ""))
      // Only created once something gets installed there, and knownFolderPath returns null
      // for folders that don't exist
      val userProgramFiles =
        WindowsKnownFolders.knownFolderPath(WindowsKnownFolders.USER_PROGRAM_FILES)
      if (userProgramFiles != null)
        assert(
          Paths.get(userProgramFiles).toRealPath() ==
            Paths.get(WindowsKnownFolders.localAppData()).toRealPath().resolve("Programs")
        )
    }

    test("envVars") synchronized {
      coursier.jniutils.NativeApi.set(null)
      assert(WindowsKnownFolders.ROAMING_APP_DATA == roamingAppData)
      samePath(WindowsKnownFolders.roamingAppData(), "APPDATA")
      samePath(WindowsKnownFolders.localAppData(), "LOCALAPPDATA")
      samePath(WindowsKnownFolders.programData(), "ProgramData")
      samePath(WindowsKnownFolders.profile(), "USERPROFILE")
      samePath(WindowsKnownFolders.knownFolderPath(WindowsKnownFolders.WINDOWS), "SystemRoot")
    }

    test("programFiles") synchronized {
      coursier.jniutils.NativeApi.set(null)
      samePath(
        WindowsKnownFolders.knownFolderPath(WindowsKnownFolders.PROGRAM_FILES),
        "ProgramFiles"
      )
      // ProgramFilesX64 isn't supported from 32-bit processes
      val x64 = WindowsKnownFolders.knownFolderPath(WindowsKnownFolders.PROGRAM_FILES_X64)
      if (System.getProperty("sun.arch.data.model") == "32")
        assert(x64 == null)
      else if (System.getProperty("os.arch") == "amd64" || x64 != null)
        samePath(x64, "ProgramFiles")
      samePath(
        WindowsKnownFolders.knownFolderPath(WindowsKnownFolders.PROGRAM_FILES_X86),
        "ProgramFiles(x86)"
      )
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
