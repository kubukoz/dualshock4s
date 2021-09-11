package com.kubukoz.dualshock4s

import cats.effect._
import cats.effect.std.Console
import fs2.Stream
import cats.implicits._
import scodec.bits._
import scala.concurrent.duration._
import fs2.Pipe
import java.util.concurrent.TimeUnit
import scala.util.chaining._
import com.kubukoz.hid4s._

object Main extends IOApp.Simple {

  val vendorId = 0x54c
  val productId = 0x9cc

  def retryExponentially[F[_]: Temporal: Console, A]: Pipe[F, A, A] = {
    val factor = 1.2

    def go(stream: Stream[F, A], attemptsRemaining: Int, currentDelay: FiniteDuration): Stream[F, A] =
      if (attemptsRemaining <= 1) stream
      else
        Stream.suspend {
          val newDelay = FiniteDuration((currentDelay * factor).toMillis, TimeUnit.MILLISECONDS)

          val showRetrying = Stream.exec(
            Console[F].println(
              s"Device not available, retrying ${attemptsRemaining - 1} more times in $newDelay..."
            )
          )

          stream.handleErrorWith(_ => showRetrying ++ go(stream, attemptsRemaining - 1, newDelay).delayBy(currentDelay))
        }

    go(_, 10, 1.second)
  }

  enum Event {
    case Cross
    case Square
    case Triangle
    case Circle

    def toCommand: String = this match {
      case Cross    => "s"
      case Square   => "j"
      case Triangle => "f 20"
      case Circle   => "d"
    }

  }

  object Event {

    given cats.Eq[Event] = cats.Eq.fromUniversalEquals

    def fromXOXO(xoxo: XOXO): Option[Event] = List(xoxo.cross, xoxo.square, xoxo.triangle, xoxo.circle).zipWithIndex.collectFirst {
      case (v, index) if v.on =>
        Event.fromOrdinal(index)
    }

  }

  def run: IO[Unit] =
    Stream
      .resource(HID.instance[IO])
      .flatMap(_.getDevice(vendorId, productId).pipe(Stream.resource).pipe(retryExponentially))
      .flatMap(_.read(64))
      .map(Dualshock.codec.decode(_))
      .map(_.toEither.map(_.value.keys.xoxo).toOption.get)
      .map(Event.fromXOXO)
      .changes
      // .debug()
      .unNone
      .map(_.toCommand)
      // .map {
      //   _.map { result =>
      //     result.map(ds4 => (ds4, result.remainder.take(8).splitAt(4).bimap(_.toInt(), _.toInt())))
      //   }
      // }
      // .map(_.toOption.get.value._1)
      // .metered(10.millis)
      // .takeWhile(!_.keys.xoxo.circle.on)
      // .map(_.keys)
      // .map(ds => (ds.xoxo, ds.arrows))
      .debug()
      .compile
      .drain

}
