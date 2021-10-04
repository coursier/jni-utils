package coursier.jniutils.tests

import coursier.jniutils.ModuleFileName
import utest._

object ModuleFileNameTests extends TestSuite {
  val tests = Tests {
    test("java") {
      val value = ModuleFileName.get()
      System.err.println(value)
      assert(value.endsWith("java.exe"))
    }
  }
}
