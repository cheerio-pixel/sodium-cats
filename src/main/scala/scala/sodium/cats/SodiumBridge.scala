package scala.sodium.cats

import cats._
import scala.{sodium => sd}
import scala.sodium.Transaction
import collection.mutable

object SodiumBridge {
  implicit object CellMonad extends Monad[sd.Cell] {
    @inline override def map[A, B](fa: sd.Cell[A])(f: A => B): sd.Cell[B] =
      fa.map(f)

    override def flatMap[A, B](fa: sd.Cell[A])(f: A => sd.Cell[B]): sd.Cell[B] =
      sd.Cell.switchC(fa.map(f))

    override def tailRecM[A, B](
        a: A
    )(f: A => sd.Cell[Either[A, B]]): sd.Cell[B] = {
      var current: sd.Cell[Either[A, B]] = f(a)

      var result: Option[sd.Cell[B]] = None

      // Iterate until we get a Right value
      while (result.isEmpty) {
        current = sd.Cell.switchC(current.map {
          case Left(nextA) =>
            f(nextA)
          case Right(b) =>
            result = Some(new sd.Cell(b))
            new sd.Cell(Right(b))
        })
      }

      result.get
    }

    override def pure[A](x: A): sd.Cell[A] =
      new sd.Cell(x)
  }
  implicit object StreamMonad extends Monad[sd.Stream] {

    override def tailRecM[A, B](a: A)(
        f: A => sd.Stream[Either[A, B]]
    ): sd.Stream[B] = {
      val resultStream = new sd.StreamWithSend[B]()
      val queue = mutable.Queue[A](a)

      def processQueue(): Unit = {
        while (queue.nonEmpty) {
          val currentA = queue.dequeue()
          val stream = f(currentA)

          val listener = stream.listen {
            case Left(nextA) =>
              queue.enqueue(nextA) // Enqueue the next value for processing
            case Right(b) =>
              Transaction { trans =>
                resultStream.send(trans, b) // Emit the result
              }
          }
          val _ = resultStream.unsafeAddCleanup(listener)
        }
      }

      processQueue()
      resultStream
    }

    @inline override def map[A, B](fa: sd.Stream[A])(f: A => B): sd.Stream[B] =
      fa.map(f)

    override def flatMap[A, B](fa: sd.Stream[A])(
        f: A => sd.Stream[B]
    ): sd.Stream[B] =
      sd.Cell.switchS(fa.map(f).hold(new sd.Stream()))

    override def pure[A](x: A): sd.Stream[A] = {
      val s = new sd.StreamSink[A]()
      s.send(x)
      s
    }
  }

}
