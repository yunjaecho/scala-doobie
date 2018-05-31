package com.yunjae.doobie.example

import cats.implicits._
import cats.effect.IO
import fs2.Stream
import doobie._, doobie.implicits._

object FirstExample {
  // Our data model
  final case class Supplier(id: Int, name: String, street: String, city: String, state: String, zip: String)
  final case class Coffee(name: String, supId: Int, price: Double, sales: Int, total: Int)

  // Some suppliers
  val suppliers = List(
    Supplier(101, "Acme, Inc.",      "99 Market Street", "Groundsville", "CA", "95199"),
    Supplier( 49, "Superior Coffee", "1 Party Place",    "Mendocino",    "CA", "95460"),
    Supplier(150, "The High Ground", "100 Coffee Lane",  "Meadows",      "CA", "93966")
  )

  // Some coffees
  val coffees = List(
    Coffee("Colombian",         101, 7.99, 0, 0),
    Coffee("French_Roast",       49, 8.99, 0, 0),
    Coffee("Espresso",          150, 9.99, 0, 0),
    Coffee("Colombian_Decaf",   101, 8.99, 0, 0),
    Coffee("French_Roast_Decaf", 49, 9.99, 0, 0)
  )

  def examples: ConnectionIO[String] = {
    // Create and populate
    for {
      _ <- DAO.create
      ns <- DAO.insertSuppliers(suppliers)
      nc <- DAO.insertCoffees(coffees)
      _  <- putStrLn(s"Inserted $ns suppliers and $nc coffees.")

      // Select and stream the coffees to stdout
      _ <- DAO.allCoffees.evalMap(c => putStrLn(s"$c")).compile.drain

      // Get the names and supplier names for all coffees costing less than $9.00,
      // again streamed directly to stdout
      _ <- DAO.coffeesLessThan(9.0).evalMap(p => putStrLn(s"$p")).compile.drain

      // Same thing, but read into a list this time
      l <- DAO.coffeesLessThan(9.0).compile.toList
      _ <- putStrLn(l.toString)

      // Read into a vector this time, with some stream processing
      v <- DAO.coffeesLessThan(9.0).take(2).map(p => p._1 + "*" + p._2).compile.toVector
      _ <- putStrLn(v.toString)
    } yield "All done!"

  }

  def runc: IO[Unit] = {
    val db = Transactor.fromDriverManager[IO](
      "com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/world?useUnicode=true&autoReconnect=true&useTimezone=true&serverTimezone=UTC&connectTimeout=3000&socketTimeout=3000", "root", "wofl07"
    )
    for {
      a <- examples.transact(db).attempt
      - <- IO(println(s"$a"))
    } yield()
  }

  object DAO {
    def coffeesLessThan(price: Double): Stream[ConnectionIO, (String, String)] =
      Queries.coffeesLessThan(price).stream


    // bulk insert
    def insertSuppliers(ss: List[Supplier]): ConnectionIO[Int] =
      Queries.insertSupplier.updateMany(ss)

    def insertCoffees(cs: List[Coffee]): ConnectionIO[Int] =
      Queries.insertCoffee.updateMany(cs)

    def allCoffees: Stream[ConnectionIO, Coffee] =
      Queries.allCoffees.stream

    def create: ConnectionIO[Unit] =
      Queries.create.run.void
  }

  object Queries {
    def coffeesLessThan(price: Double): Query0[(String, String)] =
      sql"""
           |SELECT cof_name, sup_name
           |        FROM coffees JOIN suppliers ON coffees.sup_id = suppliers.sup_id
           |        WHERE price < $price
        """.query[(String, String)]

    val insertSupplier: Update[Supplier] =
      Update[Supplier]("INSERT INTO suppliers VALUES (?, ?, ?, ?, ?, ?)", None)

    val insertCoffee: Update[Coffee] =
      Update[Coffee]("INSERT INTO coffees VALUES (?, ?, ?, ?, ?)", None)

    def allCoffees[A]: Query0[Coffee] =
      sql"SELECT cof_name, sup_id, price, sales, total FROM coffees".query[Coffee]

    def create: Update0 =
      sql"""
        CREATE TABLE suppliers (
          sup_id   INT     NOT NULL PRIMARY KEY,
          sup_name VARCHAR(1000) NOT NULL,
          street   VARCHAR(1000) NOT NULL,
          city     VARCHAR(1000) NOT NULL,
          state    VARCHAR(1000) NOT NULL,
          zip      VARCHAR(1000) NOT NULL
        );
        CREATE TABLE coffees (
          cof_name VARCHAR(1000) NOT NULL,
          sup_id   INT     NOT NULL,
          price    DOUBLE  NOT NULL,
          sales    INT     NOT NULL,
          total    INT     NOT NULL
        );
        ALTER TABLE coffees
        ADD CONSTRAINT coffees_suppliers_fk FOREIGN KEY (sup_id) REFERENCES suppliers(sup_id);
      """.update
  }

  def putStrLn(s: => String): ConnectionIO[Unit] =
    FC.delay(println(s))

  def main(args: Array[String]): Unit = {
    runc.unsafeRunSync()
  }

}
