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
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import com.monovore.decline.Argument

object Main extends CrossPlatformIOApp {

  def retryExponentially[F[_]: Temporal: Console, A]: Pipe[F, A, A] = {
    val factor = 1.2

    def go(stream: Stream[F, A], attemptsRemaining: Int, currentDelay: FiniteDuration): Stream[F, A] =
      if (attemptsRemaining <= 1) stream
      else
        Stream.suspend {
          val newDelay = FiniteDuration((currentDelay * factor).toMillis, TimeUnit.MILLISECONDS)

          val showRetrying = Stream.exec(
            Console[F].errorln(
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
        keys.r1 -> Event.R1,
      ).collectFirst {
        case (v, event) if v.on =>
          event
      }

  }

  enum DeviceInfo(val vendorId: Int, val productId: Int) {
    case Dualsense extends DeviceInfo(0x54c, 0xce6)
    case DS4 extends DeviceInfo(0x54c, 0x9cc)
  }

  enum InputType {
    case Stdin
    case Hidapi
  }

  val devices = Map(
    "ds4" -> DeviceInfo.DS4,
    "dualsense" -> DeviceInfo.Dualsense,
  )

  val inputs = Map(
    "stdin" -> InputType.Stdin,
    "hidapi" -> InputType.Hidapi,
  )

  val stdin: Stream[cats.effect.IO, BitVector] =
    fs2
      .io
      .stdin[IO](64)
      .groupWithin(64, 1.second)
      .map(bytes => BitVector(bytes.toByteBuffer))

  def hidapi(device: DeviceInfo) = Stream
    .resource(HID.instance[IO])
    .flatMap(_.getDevice(device.vendorId, device.productId).pipe(Stream.resource).pipe(retryExponentially))
    .flatMap(_.read(64))

  def run(args: List[String]): IO[ExitCode] = {
    val opts = (
      Opts
        .option("device", "The device to look for", "d")(
          using Argument.fromMap("device", devices)
        )
        .withDefault(DeviceInfo.Dualsense),
      Opts
        .option("input", "The input method", "i")(
          using Argument.fromMap("input", inputs)
        )
        .withDefault(InputType.Hidapi),
      Opts
        .option[FiniteDuration]("rate", "The polling rate (only used with hidapi)", "r")
        .withDefault(100.millis),
    ).mapN { (device, inputType, pollingRate) =>
      val input = inputType match {
        case InputType.Stdin  => stdin
        case InputType.Hidapi => hidapi(device).metered(pollingRate)
      }

      input.through(loop).compile.drain.as(ExitCode.Success)
    }

    CommandIOApp.run[IO]("dualshock4s-cli", "Welcome to the Dualshock4s CLI")(opts, args)
  }

  val loop: fs2.Pipe[IO, BitVector, Nothing] = _.map(Dualsense.codec.decode(_))
    .map(_.toEither.map(_.value.keys).toOption.get)
    .map(Event.fromKeys)
    .changes
    .unNone
    .map(_.toAction.toCommand)
    .debug()
    .drain

}
