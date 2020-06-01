package com.kubukoz.dualshock4s

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import org.hid4java.HidManager
import cats.effect.Resource
import org.hid4java.HidServices
import fs2.Stream
import cats.implicits._
import scodec._
import scodec.bits._
import org.hid4java.HidDevice
import java.io.InputStream
import _root_.cats.effect.Blocker
import scala.concurrent.duration._

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

  def readDevice(device: HidDevice)(blocker: Blocker): fs2.Stream[IO, BitVector] =
    Stream
      .eval(IO {
        Array.fill[Byte](64)(0)
      })
      .flatMap { buffer =>
        val loadBuffer = blocker.delay[IO, Int](device.read(buffer))
        val readBuffer = IO(BitVector(buffer))

        Stream.repeatEval(loadBuffer *> readBuffer)
      }

  def run(args: List[String]): IO[ExitCode] =
    Stream
      .resource(Blocker[IO])
      .flatMap { blocker =>
        fs2.Stream.resource(hidServices.flatMap(useDevice)).flatMap(readDevice(_)(blocker))
      }
      .map(Dualshock.codec.decode(_))
      .map {
        _.map { result =>
          result.map(d => (d, result.remainder.take(8).splitAt(4).bimap(_.toInt(), _.toInt())))
        }
      }
      .metered(10.millis)
      // .map(_.toOption.get.value._1.keys.xoxo.toString())
      // .changes
      .debug()
      .takeWhile(!_.toOption.get.value._1.keys.xoxo.circle.on)
      .compile
      .drain
      .as(ExitCode.Success)
}
