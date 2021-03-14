package com.kubukoz.hid4s

import cats.effect.Resource
import cats.effect.Sync
import cats.effect.MonadCancel
import cats.effect.std.Console
import org.hid4java.HidManager
import org.hid4java.HidServices
import org.hid4java.HidDevice
import cats.implicits._
import scodec.bits.BitVector
import fs2.Stream
import scala.jdk.CollectionConverters._

trait HID[F[_]] {
  def getDevices: F[List[DeviceDescriptor[F]]]
  def getDevice(vendorId: Int, productId: Int): Resource[F, Device[F]]
}

object HID {

  def instance[F[_]: Sync: Console]: Resource[F, HID[F]] = Resource
    .make(Sync[F].delay {
      HidManager.getHidServices()
    })(services => Sync[F].delay(services.shutdown()))
    .map { services =>
      new HID[F] {
        def getDevices: F[List[DeviceDescriptor[F]]] =
          Sync[F].delay(services.getAttachedHidDevices).map(_.asScala.toList.map(DeviceDescriptor.fromRaw(_)))

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

trait DeviceDescriptor[F[_]] {
  def open: Resource[F, Device[F]]
  def describe: F[Unit]
}

object DeviceDescriptor {
  def fromRaw[F[_]: Sync: Console](device: HidDevice)(using MonadCancel[F, _]): DeviceDescriptor[F] = new DeviceDescriptor[F]{
    val open: Resource[F, Device[F]] = Resource.pure(device).map(Device.fromRaw).onFinalize(Sync[F].delay(device.close()))
    val describe: F[Unit] = Console[F].println(device)
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
