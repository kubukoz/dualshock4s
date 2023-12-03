package com.kubukoz.hid4s

import cats.effect.Resource
import cats.effect.Sync
import cats.effect.MonadCancel
import cats.effect.std.Console

import cats.implicits._
import scodec.bits.BitVector
import fs2.Stream
import scala.jdk.CollectionConverters._

trait HID[F[_]] {
  def getDevices: F[List[DeviceDescriptor[F]]]
  def getDevice(vendorId: Int, productId: Int): Resource[F, Device[F]]
}

trait DeviceDescriptor[F[_]] {
  def open: Resource[F, Device[F]]
  def describe: F[Unit]
}

object HID extends HIDPlatform

object DeviceDescriptor extends DeviceDescriptorPlatform

trait Device[F[_]] {
  def read(bufferSize: Int): fs2.Stream[F, BitVector]
}

object Device extends DevicePlatform
