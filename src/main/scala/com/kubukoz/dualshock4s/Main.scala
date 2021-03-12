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
import scala.util.chaining._

trait HID[F[_]] {
  def getDevice(vendorId: Int, productId: Int): Resource[F, Device[F]]
}

object HID {

  def instance[F[_]: Sync]: Resource[F, HID[F]] = Resource
    .make(Sync[F].delay {
      HidManager.getHidServices()
    })(services => Sync[F].delay(services.shutdown()))
    .map { services =>
      new HID[F] {
        def getDevice(vendorId: Int, productId: Int): Resource[F, Device[F]] = {
          val findDevice = Sync[F]
            .delay(Option(services.getHidDevice(vendorId, productId, null)))
            .flatMap(_.liftTo[F](new Throwable("Device not found")))

          Resource
            .make(findDevice)(device => Sync[F].delay(device.close()))
            .map(Device.fromRaw)
        }
      }
    }

}

trait Device[F[_]] {
  def read(bufferSize: Int): fs2.Stream[F, BitVector]
}

object Device {

  def fromRaw[F[_]: Sync](device: HidDevice): Device[F] = new Device[F] {

    def read(bufferSize: Int): fs2.Stream[F, BitVector] = Stream
      .eval {
        Sync[F].delay {
          Array.fill[Byte](bufferSize)(0)
        }
      }
      .flatMap { buffer =>
        val loadBuffer = Sync[F].blocking(device.read(buffer))
        val readBuffer = Sync[F].delay(BitVector(buffer))

        Stream.repeatEval(loadBuffer *> readBuffer)
      }

  }

}

object Main extends IOApp {

  val vendorId = Integer.parseInt("54c", 16) //1356
  val productId = Integer.parseInt("9cc", 16) //2508

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
      .resource(HID.instance[IO])
      .flatMap(_.getDevice(vendorId, productId).pipe(Stream.resource).pipe(retryExponentially))
      .flatMap(_.read(64))
      .map(Dualshock.codec.decode(_))
      .map {
        _.map { result =>
          result.map(ds4 => (ds4, result.remainder.take(8).splitAt(4).bimap(_.toInt(), _.toInt())))
        }
      }
      .map(_.toOption.get.value._1)
      // .metered(10.millis)
      .takeWhile(!_.keys.xoxo.circle.on)
      .map(_.keys)
      .map(ds => (ds.xoxo, ds.arrows))
      .map(_.toString())
      .changes
      .showLinesStdOut
      .compile
      .drain
      .as(ExitCode.Success)

}
