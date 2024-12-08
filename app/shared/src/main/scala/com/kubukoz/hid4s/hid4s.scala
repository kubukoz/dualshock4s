package com.kubukoz.hid4s

import cats.effect.Resource

import scodec.bits.BitVector

trait HID[F[_]] {
  def getDevice(vendorId: Int, productId: Int): Resource[F, Device[F]]
}

object HID extends HIDPlatform

trait DeviceDescriptor[F[_]] {
  def open: Resource[F, Device[F]]
  def describe: F[Unit]
}

trait Device[F[_]] {
  def read(bufferSize: Int): fs2.Stream[F, BitVector]
}
