package millbuild

import mill.*, scalalib.*

object Deps {
  def svm = mvn"org.graalvm.nativeimage:svm:25.0.2"
  def utest = mvn"com.lihaoyi::utest:0.9.5"
}

object Scala {
  def scala213 = "3.9.0"
}
