package com.kubukoz.dualshock4s

import scodec.Codec
import scodec.codecs._
import scodec.bits._
import com.kubukoz.dualshock4s.Key._
import cats.implicits._
import scodec.interop.cats._
import scodec.Attempt
import cats.data.NonEmptyList

final case class Dualshock(keys: Keys /*, touch: Touch, motion: Motion, info: Info */ )

object Dualshock {

  implicit val codec: Codec[Dualshock] = {

    val header = constant(bin"00000001")

    val analog = byte.as[Analog]

    val stick =
      (scodec.codecs.provide(Digital(true)) :: analog :: analog).as[Stick]

    val l3 = stick
    val r3 = stick

    def arrow[A <: Arrows](f: Arrows.type => A)(bin: BitVector): Codec[A] = constant(bin) ~> provide(f(Arrows))

    val arrows = (
      arrow(_.Neither)(bin"1000") :+:
        arrow(_.Up)(bin"0000") :+:
        arrow(_.Right)(bin"0010") :+:
        arrow(_.Left)(bin"0110") :+:
        arrow(_.Down)(bin"0100") :+:
        arrow(_.UpRight)(bin"0001") :+:
        arrow(_.UpLeft)(bin"0111") :+:
        arrow(_.DownLeft)(bin"0101") :+:
        arrow(_.DownRight)(bin"0011")
    ).choice.as[Arrows]

    val xoxo = sizedList(4, bool.as[Digital]).imap(_.toHList)(_.toSized[List]).as[XOXO]

    val keys =
      (l3 :: r3 :: xoxo :: arrows).as[Keys]

    (header ~> keys).as[Dualshock]
  }
}

object Key {
  final case class Digital(on: Boolean)
  final case class Analog(value: Byte)
  final case class Stick(pressed: Digital, x: Analog, y: Analog)
  sealed trait Arrows extends Product with Serializable

  object Arrows {
    case object Neither extends Arrows
    case object Left extends Arrows
    case object Up extends Arrows
    case object Right extends Arrows
    case object Down extends Arrows
    case object UpLeft extends Arrows
    case object DownLeft extends Arrows
    case object DownRight extends Arrows
    case object UpRight extends Arrows
  }
}

final case class XOXO(triangle: Key.Digital, circle: Key.Digital, cross: Key.Digital, square: Key.Digital)

final case class Keys(
  l3: Key.Stick,
  r3: Key.Stick,
  xoxo: XOXO,
  //arrows
  arrows: Key.Arrows
  //meta
  /*options: Key.Digital,
  share: Key.Digital */
)

final case class Touch()
final case class Motion()

final case class Info(headphones: Info.Headphones)

object Info {
  sealed trait Headphones extends Product with Serializable

  object Headphones {
    case object Connected extends Headphones
    case object Disconnected extends Headphones
  }
}
