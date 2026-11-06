package coursier.jniutils.tests

import coursier.jniutils.WindowsCodePages
import utest._

object WindowsCodePagesTests extends TestSuite {

  private def charsetName(codePage: Int): String =
    Option(WindowsCodePages.charsetFor(codePage)).map(_.name()).orNull

  val tests = Tests {

    // Several code pages also resolve under a "cp" or "IBM" name, but to IBM's variant of
    // the encoding rather than the one Windows uses - "cp932" is x-IBM942C, not windows-31j.
    test("picks the Microsoft variant for CJK code pages") {
      assert(charsetName(932) == "windows-31j")
      assert(charsetName(936) == "GBK")
      assert(charsetName(949) == "x-windows-949")
      assert(charsetName(950) == "x-windows-950")
    }

    test("maps the OEM code pages") {
      assert(charsetName(437) == "IBM437")
      assert(charsetName(850) == "IBM850")
    }

    test("maps the ANSI code pages") {
      assert(charsetName(1252) == "windows-1252")
      assert(charsetName(1251) == "windows-1251")
    }

    test("maps UTF-8") {
      assert(charsetName(65001) == "UTF-8")
    }

    test("returns null for an unknown code page") {
      assert(charsetName(12345) == null)
    }

    // Any ANSI code page (1252 on the CI runners, say) should have a charset in the JDK
    test("ansi") {
      val codePage = WindowsCodePages.ansi()
      assert(codePage > 0)
      assert(WindowsCodePages.ansiCharset() != null)
    }

    // 0 when the process has no console, which can happen on CI
    test("consoleOutput") {
      val codePage = WindowsCodePages.consoleOutput()
      assert(codePage >= 0)
      if (codePage == 0)
        assert(WindowsCodePages.consoleOutputCharset() == null)
    }
  }
}
