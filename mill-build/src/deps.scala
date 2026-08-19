package millbuild

import mill.*, scalalib.*

object Deps {
  def svm = mvn"org.graalvm.nativeimage:svm:25.0.4.1"
  def utest = mvn"com.lihaoyi::utest:0.9.5"
}

object Scala {
  def scala213 = "2.13.18"
}
