import mill._, scalalib._

object Deps {
  def svm = ivy"org.graalvm.nativeimage:svm:25.0.2"
  def utest = ivy"com.lihaoyi::utest:0.8.9"
}

object Scala {
  def scala213 = "2.13.18"
}
