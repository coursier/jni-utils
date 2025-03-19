import mill._, scalalib._

object Deps {
  def svm = ivy"org.graalvm.nativeimage:svm:24.2.0"
  def utest = ivy"com.lihaoyi::utest:0.8.5"
}

object Scala {
  def scala213 = "2.13.16"
}
