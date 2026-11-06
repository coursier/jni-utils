package coursier.jniutils.tests

import coursier.jniutils.{TerminalSize, WindowsAnsiTerminal}
import utest._

import java.io.IOException

// The standard output and error of the tests may or may not be consoles (they're not on CI),
// so getting their size should either work, or fail with an IOException.
object WindowsAnsiTerminalTests extends TestSuite {

  private def check(name: String, size: => TerminalSize): Unit =
    try {
      val size0 = size
      System.err.println(s"$name: ${size0.getWidth}x${size0.getHeight}")
      assert(size0.getWidth > 0)
      assert(size0.getHeight > 0)
    }
    catch {
      case e: IOException =>
        System.err.println(s"$name: $e")
    }

  val tests = Tests {
    test("stdout") {
      check("stdout", WindowsAnsiTerminal.terminalSize())
    }
    test("stderr") {
      check("stderr", WindowsAnsiTerminal.stderrTerminalSize())
    }
  }
}
