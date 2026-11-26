package coursier.jniutils

import utest._

import java.io.File
import java.nio.file.{Files, Path}
import java.util.Comparator

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

// Checks the logic of LoadWindowsLibrary.load with a fake loader, so this doesn't load
// any DLL (in the same package as LoadWindowsLibrary, to access its package-private members)
object LoadCopiesTests extends TestSuite {

  private def withDllDir[T](f: (Path, Path) => T): T = {
    val tmpDir = Files.createTempDirectory("jni-utils-load-copies")
    val previous = Option(System.getProperty("coursier.jniutils.dll-dir"))
    try {
      System.setProperty("coursier.jniutils.dll-dir", tmpDir.resolve("dlls").toString)
      val archDir = LoadWindowsLibrary.extractionDir().toPath
      Files.createDirectories(archDir)
      val dll = archDir.resolve(LoadWindowsLibrary.dllBaseName("") + ".dll")
      Files.write(dll, Array[Byte](1, 2, 3))
      f(archDir, dll)
    }
    finally {
      previous match {
        case Some(value) => System.setProperty("coursier.jniutils.dll-dir", value)
        case None        => System.clearProperty("coursier.jniutils.dll-dir")
      }
      Files.walk(tmpDir).sorted(Comparator.reverseOrder[Path]()).iterator().asScala
        .foreach(Files.delete(_))
    }
  }

  private def alreadyLoaded(path: String) =
    new UnsatisfiedLinkError(s"Native Library $path already loaded in another classloader")

  // Fake loader, failing like the JVM does for the paths in alreadyLoadedPaths
  private class FakeLoader(alreadyLoadedPaths: String => Boolean) extends LoadWindowsLibrary.PathLoader {
    val attempts = new ListBuffer[String]
    def load(path: String): Unit = {
      attempts += path
      if (alreadyLoadedPaths(path))
        throw alreadyLoaded(path)
    }
  }

  private def name(path: String) = new File(path).getName

  val tests = Tests {
    test("no copy if loading works") {
      withDllDir { (dir, dll) =>
        val loader = new FakeLoader(_ => false)
        LoadWindowsLibrary.load(dll.toFile, "", loader)
        assert(loader.attempts.toList == List(dll.toFile.getAbsolutePath))
        assert(Files.list(dir).count() == 1L)
      }
    }

    test("copies until loading works") {
      withDllDir { (dir, dll) =>
        val base = LoadWindowsLibrary.dllBaseName("")
        val taken = Set(s"$base.dll", s"$base-1.dll", s"$base-2.dll")
        val loader = new FakeLoader(p => taken(name(p)))
        LoadWindowsLibrary.load(dll.toFile, "", loader)
        assert(loader.attempts.map(name).toList == List(s"$base.dll", s"$base-1.dll", s"$base-2.dll", s"$base-3.dll"))
        val copy = dir.resolve(s"$base-3.dll")
        assert(Files.readAllBytes(copy).toSeq == Seq[Byte](1, 2, 3))
      }
    }

    test("copies named after the variant") {
      withDllDir { (dir, dll) =>
        val base = LoadWindowsLibrary.dllBaseName("bootstrap")
        val loader = new FakeLoader(p => name(p) == name(dll.toString))
        LoadWindowsLibrary.load(dll.toFile, "bootstrap", loader)
        assert(name(loader.attempts.last) == s"$base-1.dll")
        assert(Files.isRegularFile(dir.resolve(s"$base-1.dll")))
      }
    }

    test("other errors don't trigger copies") {
      withDllDir { (dir, dll) =>
        class Loader extends LoadWindowsLibrary.PathLoader {
          var count = 0
          def load(path: String): Unit = {
            count += 1
            throw new UnsatisfiedLinkError(s"$path: Can't find dependent libraries")
          }
        }
        val loader = new Loader
        val ex = assertThrows[UnsatisfiedLinkError] {
          LoadWindowsLibrary.load(dll.toFile, "", loader)
        }
        assert(ex.getMessage.contains("Can't find dependent libraries"))
        assert(loader.count == 1)
        assert(Files.list(dir).count() == 1L)
      }
    }

    test("other errors on a copy stop the attempts") {
      withDllDir { (_, dll) =>
        class Loader extends LoadWindowsLibrary.PathLoader {
          val attempts = new ListBuffer[String]
          def load(path: String): Unit = {
            attempts += path
            if (attempts.length == 1) throw alreadyLoaded(path)
            else throw new UnsatisfiedLinkError(s"$path: Access is denied")
          }
        }
        val loader = new Loader
        val ex = assertThrows[UnsatisfiedLinkError] {
          LoadWindowsLibrary.load(dll.toFile, "", loader)
        }
        assert(ex.getMessage.contains("Access is denied"))
        assert(loader.attempts.length == 2)
        assert(ex.getSuppressed.exists {
          case e: UnsatisfiedLinkError => LoadWindowsLibrary.isLoadedByAnotherClassLoader(e)
          case _                       => false
        })
      }
    }

    test("custom maximum number of copies") {
      withDllDir { (dir, dll) =>
        val loader = new FakeLoader(_ => true)
        val ex = assertThrows[UnsatisfiedLinkError] {
          LoadWindowsLibrary.load(dll.toFile, "", loader, 3)
        }
        assert(loader.attempts.length == 4)
        assert(ex.getMessage.contains("any of its 3 copies"))
        assert(Files.list(dir).count() == 4L)
      }
    }

    test("no copies") {
      withDllDir { (dir, dll) =>
        val loader = new FakeLoader(_ => true)
        val ex = assertThrows[UnsatisfiedLinkError] {
          LoadWindowsLibrary.load(dll.toFile, "", loader, 0)
        }
        assert(LoadWindowsLibrary.isLoadedByAnotherClassLoader(ex))
        assert(loader.attempts.length == 1)
        assert(Files.list(dir).count() == 1L)
      }
    }

    test("max copies property") {
      val default = LoadWindowsLibrary.defaultMaxCopies
      assert(LoadWindowsLibrary.parseMaxCopies(null) == default)
      assert(LoadWindowsLibrary.parseMaxCopies("") == default)
      assert(LoadWindowsLibrary.parseMaxCopies(" 4 ") == 4)
      assert(LoadWindowsLibrary.parseMaxCopies("0") == 0)
      assert(LoadWindowsLibrary.parseMaxCopies("-1") == default)
      assert(LoadWindowsLibrary.parseMaxCopies("foo") == default)
      assert(LoadWindowsLibrary.maxCopiesProperty == "coursier.jniutils.max-dll-copies")
    }

    test("max copies property read at initialization") {
      val location = classOf[LoadWindowsLibrary].getProtectionDomain.getCodeSource.getLocation
      val previous = Option(System.getProperty(LoadWindowsLibrary.maxCopiesProperty))
      val value =
        try {
          System.setProperty(LoadWindowsLibrary.maxCopiesProperty, "5")
          // fresh copy of LoadWindowsLibrary, initialized with the property set above
          val loader = new java.net.URLClassLoader(
            Array(location),
            ClassLoader.getSystemClassLoader.getParent
          )
          val clazz = Class.forName(classOf[LoadWindowsLibrary].getName, true, loader)
          assert(clazz.getClassLoader == loader)
          val field = clazz.getDeclaredField("maxCopies")
          field.setAccessible(true)
          field.getInt(null)
        }
        finally {
          previous match {
            case Some(v) => System.setProperty(LoadWindowsLibrary.maxCopiesProperty, v)
            case None    => System.clearProperty(LoadWindowsLibrary.maxCopiesProperty)
          }
        }
      assert(value == 5)
    }

    test("bounded number of copies") {
      withDllDir { (dir, dll) =>
        val loader = new FakeLoader(_ => true)
        val ex = assertThrows[UnsatisfiedLinkError] {
          LoadWindowsLibrary.load(dll.toFile, "", loader)
        }
        assert(loader.attempts.length == 1 + LoadWindowsLibrary.maxCopies)
        assert(ex.getMessage.contains(s"any of its ${LoadWindowsLibrary.maxCopies} copies"))
        assert(ex.getCause.isInstanceOf[UnsatisfiedLinkError])
        assert(Files.list(dir).count() == 1L + LoadWindowsLibrary.maxCopies)
      }
    }
  }
}
