package com.kubukoz.dualshock4s

import cats.effect._
import cats.effect.std.Console
import org.hid4java.HidManager
import org.hid4java.HidServices
import fs2.Stream
import cats.implicits._
import scodec.bits._
import org.hid4java.HidDevice
import scala.concurrent.duration._
import cats.effect.Sync
import fs2.Pipe
import java.util.concurrent.TimeUnit

object Main extends IOApp {

  val vendorId = Integer.parseInt("54c", 16) //1356
  val productId = Integer.parseInt("9cc", 16) //2508

  val hidServices: Resource[IO, HidServices] = Resource.make(IO {
    HidManager.getHidServices()
  })(services => IO(services.shutdown()))

  def useDevice(services: HidServices): Resource[IO, HidDevice] = {
    val findDevice =
      IO(Option(services.getHidDevice(vendorId, productId, null)))
        .flatMap(IO.fromOption(_)(new Throwable("Device not found")))

    Resource.make(findDevice)(device => IO(device.close()))
  }

  def readDevice(device: HidDevice): fs2.Stream[IO, BitVector] =
    Stream
      .eval(IO {
        Array.fill[Byte](64)(0)
      })
      .flatMap { buffer =>
        val loadBuffer = IO.blocking(device.read(buffer))
        val readBuffer = IO(BitVector(buffer))

        Stream.repeatEval(loadBuffer *> readBuffer)
      }

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

  def run(args: List[String]): IO[ExitCode] =
    fs2
      .Stream
      .resource(hidServices)
      .flatMap((useDevice _).andThen(Stream.resource(_).through(retryExponentially)))
      .flatMap(readDevice(_))
      .map(Dualshock.codec.decode(_))
      .map {
        _.map { result =>
          result.map(d => (d, result.remainder.take(8).splitAt(4).bimap(_.toInt(), _.toInt())))
        }
      }
      .map(_.toOption.get.value._1)
      .metered(10.millis)
      .takeWhile(!_.keys.xoxo.circle.on)
      .map(_.keys)
      .map(_.toString())
      .changes
      .showLinesStdOut
      .compile
      .drain
      .as(ExitCode.Success)

}
