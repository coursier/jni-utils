import mill._, scalalib._

object Deps {
  def svm = ivy"org.graalvm.nativeimage:svm:24.2.2"
  def utest = ivy"com.lihaoyi::utest:0.9.5"
}

object Scala {
  def scala213 = "2.13.18"
}
