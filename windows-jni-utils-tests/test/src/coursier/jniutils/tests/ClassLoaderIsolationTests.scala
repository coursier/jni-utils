package coursier.jniutils.tests

import coursier.jniutils.LoadWindowsLibrary
import utest._

import java.net.{URL, URLClassLoader}
import java.nio.file.Files

// Loads the DLL via each NativeApi implementation, each from a class loader of its own, like
// when several (possibly shaded) copies of this library live in different class loaders of the
// same JVM. The JVM refuses to load a given DLL file from several class loaders, so this checks
// that each implementation loads the DLL from a file of its own.
object ClassLoaderIsolationTests extends TestSuite {

  // NativeApi implementation class name, DLL variant
  private val implementations = Seq(
    "coursier.jniutils.DefaultNativeApi"                      -> "",
    "coursier.bootstrap.launcher.jniutils.BootstrapNativeApi" -> "bootstrap",
    "lmcoursier.internal.jniutils.LmNativeApi"                -> "lmcoursier",
    "coursierapi.internal.jniutils.ApiInternalNativeApi"      -> "coursierapi"
  )

  private def location(className: String): URL =
    Class.forName(className, false, getClass.getClassLoader)
      .getProtectionDomain
      .getCodeSource
      .getLocation

  private lazy val dllResourcePath: String = {
    val m = classOf[LoadWindowsLibrary].getDeclaredMethod("dllResourcePath")
    m.setAccessible(true)
    m.invoke(null).asInstanceOf[String]
  }

  val tests = Tests {
    test("separate class loaders") {
      // Ensure the DLL is loaded by the class loader of the tests before changing dll-dir
      // below, so that it doesn't get extracted to the directory we check at the end
      LoadWindowsLibrary.ensureInitialized()

      val tmpDir = Files.createTempDirectory("jni-utils-tests")
      val dllDir = tmpDir.resolve("dlls")

      // Copy of the DLL resource, so that the default implementation doesn't load the DLL
      // from the same file as the class loader of the tests
      val resourceDir = tmpDir.resolve("resources")
      val dllResource = getClass.getClassLoader.getResource(dllResourcePath)
      assert(dllResource != null)
      val resourceCopy = resourceDir.resolve(dllResourcePath)
      Files.createDirectories(resourceCopy.getParent)
      val is = dllResource.openStream()
      try Files.copy(is, resourceCopy)
      finally is.close()

      val classPath =
        (Seq(resourceDir.toUri.toURL) ++
          (classOf[LoadWindowsLibrary].getName +: implementations.map(_._1)).map(location)).distinct
      val parent = ClassLoader.getSystemClassLoader.getParent

      val thread = Thread.currentThread()
      val previousCl = thread.getContextClassLoader
      val previousDllDir = Option(System.getProperty("coursier.jniutils.dll-dir"))
      val codePages =
        try {
          System.setProperty("coursier.jniutils.dll-dir", dllDir.toString)
          for ((className, _) <- implementations) yield {
            val loader = new URLClassLoader(classPath.toArray, parent)
            thread.setContextClassLoader(loader)
            // initializing the class loads the DLL
            val clazz = Class.forName(className, true, loader)
            assert(clazz.getClassLoader == loader)
            val api = clazz.getConstructor().newInstance()
            clazz.getMethod("GetOEMCodePage").invoke(api).asInstanceOf[Int]
          }
        }
        finally {
          thread.setContextClassLoader(previousCl)
          previousDllDir match {
            case Some(value) => System.setProperty("coursier.jniutils.dll-dir", value)
            case None        => System.clearProperty("coursier.jniutils.dll-dir")
          }
        }

      assert(codePages.forall(_ > 0))

      val elems    = dllResourcePath.split('/')
      val archDir  = elems(elems.length - 2)
      val baseName = elems.last.stripSuffix(".dll")
      for ((_, variant) <- implementations if variant.nonEmpty) {
        val f = dllDir.resolve(archDir).resolve(s"$baseName-$variant.dll")
        assert(Files.isRegularFile(f))
      }
    }
  }
}
