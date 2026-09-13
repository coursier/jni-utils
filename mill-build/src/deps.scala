package millbuild

import mill.*, scalalib.*

object Deps {
  def utest = mvn"com.lihaoyi::utest:0.9.5"
}

object Scala {
  def scala213 = "3.9.0"
}
