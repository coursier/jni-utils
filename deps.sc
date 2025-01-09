import mill._, scalalib._

object Deps {
  def svm = ivy"org.graalvm.nativeimage:svm:21.3.12"
  def utest = ivy"com.lihaoyi::utest:0.7.5"
}

object Scala {
  def scala213 = "2.13.15"
}
