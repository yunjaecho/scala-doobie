package com.yunjae.doobie

import doobie._
import doobie.implicits._
import cats.effect.IO
import doobie.ConnectionIO

object CountrySelectSample extends App {
  case class Country(code: String, name: String, population: Long)

  val xa = Transactor.fromDriverManager[IO](
    "com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/world?useUnicode=true&autoReconnect=true&useTimezone=true&serverTimezone=UTC&connectTimeout=3000&socketTimeout=3000", "root", "wofl07"
  )

  def find(name: String): ConnectionIO[Option[Country]] =
    sql"SELECT CODE, NAME, POPULATION FROM COUNTRY WHERE NAME = $name".query[Country].option

  val result = find("France").transact(xa).unsafeRunSync()
}
