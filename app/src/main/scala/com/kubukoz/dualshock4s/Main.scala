package com.kubukoz.dualshock4s

import cats.effect._
import cats.effect.std.Console
import cats.implicits._
import com.kubukoz.hid4s._
import fs2.Pipe
import fs2.Stream
import scodec.bits._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.util.chaining._
import io.chrisdavenport.crossplatformioapp.CrossPlatformIOApp

object Main extends CrossPlatformIOApp.Simple {

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
    case Cross, Square, Triangle, Circle, R1

    def toAction: Action = this match {
      case Cross    => Action.Skip
      case Square   => Action.Jump
      case Triangle => Action.FastForward(20)
      case Circle   => Action.Drop
      case R1       => Action.Switch
    }

  }

  enum Action {
    case Skip, Jump, Drop, Switch
    case FastForward(len: Int)

    def toCommand: String = this match {
      case Skip             => "s"
      case Jump             => "j"
      case FastForward(len) => s"f $len"
      case Drop             => "d"
      case Switch           => "w"
    }

  }

  object Event {

    given cats.Eq[Event] = cats.Eq.fromUniversalEquals

    def fromKeys(keys: Keys): Option[Event] =
      List(
        keys.xoxo.cross -> Event.Cross,
        keys.xoxo.square -> Event.Square,
        keys.xoxo.triangle -> Event.Triangle,
        keys.xoxo.circle -> Event.Circle,
        keys.r1 -> Event.R1
      ).collectFirst {
        case (v, event) if v.on =>
          event
      }

  }

  val stdin: Stream[cats.effect.IO, BitVector] =
    fs2
      .io
      .stdin[IO](64)
      .groupWithin(64, 1.second)
      .map(bytes => BitVector(bytes.toByteBuffer))

  val hidapi = Stream
    .resource(HID.instance[IO])
    .flatMap(_.getDevice(vendorId, productId).pipe(Stream.resource).pipe(retryExponentially))
    .flatMap(_.read(64))

  def run: IO[Unit] =
    hidapi
      // stdin
      .map(Dualshock.codec.decode(_))
      .map(_.toEither.map(_.value.keys).toOption.get)
      .map(Event.fromKeys)
      .changes
      // .debug()
      .unNone
      .map(_.toAction.toCommand)
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
