package com.yunjae.doobie

object Sample extends App {

  val nums = Seq(1,2,3)
  val letters = Seq('a', 'b', 'c')
  val res = for {
    n <- nums
    c <- letters
  } yield (n, c)

  println(res)

}
