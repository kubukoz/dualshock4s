package com.kubukoz.hid4s

import cats.effect.Resource
import cats.effect.Sync
import cats.implicits.*
import libhidapi.all.*
import scodec.bits.BitVector

import scalanative.unsafe.*
import scalanative.unsigned.*
import scalanative.*

trait HIDPlatform {

  def instance[F[_]: Sync]: Resource[F, HID[F]] = Resource.make(
    Sync[F].delay {
      val init = hid_init()
      require(init == 0, s"hid_init should be 0, was $init")

      new HID[F] {
        override def getDevice(vendorId: Int, productId: Int): Resource[F, Device[F]] =
          Resource
            .make(Sync[F].delay(hid_open(vendorId.toUShort, productId.toUShort, null)))(device => Sync[F].delay(hid_close(device)).void)
            .map { ptr =>
              require(ptr != null, "device should not be null")
              ptr
            }
            .map(DevicePlatform.fromRaw)
      }
    }
  )(_ => Sync[F].delay(hid_exit()).void)

}

object DevicePlatform {

  trait NativeDevice[F[_]] extends Device[F] {
    def raw: Ptr[hid_device]
  }

  def fromRaw[F[_]: Sync](device: Ptr[hid_device]): Device[F] = new NativeDevice[F] {

    override def raw: Ptr[hid_device] = device

    def read(bufferSize: Int): fs2.Stream[F, BitVector] = {
      def load: F[BitVector] = Sync[F].blocking {
        val buf = stackalloc[CUnsignedChar](64.toUInt)

        val mem = Array.ofDim[Byte](64)
        hid_read(device, buf, 64.toUInt)
        for {
          i <- 0 until 64
        } mem(i) = buf(i).toByte
        BitVector(mem)
      }

      fs2.Stream.repeatEval(load)
    }

  }

}

// object Demo extends IOApp.Simple {

//   val vendorId = 0x54c
//   val productId = 0x9cc

//   def run: IO[Unit] =
//     HID.instance[IO].use { hid =>
//       hid.getDevice(vendorId,productId).use { device =>
//         device.read(64).take(10).debug().compile.drain
//       }
//     }

// }
