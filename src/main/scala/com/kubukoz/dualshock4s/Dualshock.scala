package com.kubukoz.dualshock4s

import scodec.Codec
import scodec.codecs._
import scodec.bits._
import com.kubukoz.dualshock4s.Key._
import cats.implicits._
import scodec.interop.cats._
import shapeless.ops.hlist.Prepend
import shapeless.ops.hlist.Split

final case class Dualshock(keys: Keys /*, touch: Touch, motion: Motion, info: Info */ )

object Dualshock {

  implicit val codec: Codec[Dualshock] = {

    val header = "constant 1 byte" | constant(bin"00000001")

    val analog = byte.as[Analog]

    val axes =
      "X and Y axis" | (analog :: analog)

    val l3 = "left stick position" | axes
    val r3 = "right stick position" | axes

    def arrow[A <: Arrows](f: Arrows.type => A)(bin: BitVector): Codec[A] =
      s"Arrow.${f(Arrows)}" | (constant(bin) ~> provide(f(Arrows)))

    val arrows = "arrows" | {
      (arrow(_.Up)(bin"0000") :+:
        arrow(_.UpRight)(bin"0001") :+:
        arrow(_.Right)(bin"0010") :+:
        arrow(_.DownRight)(bin"0011") :+:
        arrow(_.Down)(bin"0100") :+:
        arrow(_.DownLeft)(bin"0101") :+:
        arrow(_.Left)(bin"0110") :+:
        arrow(_.UpLeft)(bin"0111") :+:
        arrow(_.Neither)(bin"1000")).choice
    }.as[Arrows]

    val digital = "binary value" | bool.as[Digital]

    val xoxo = "action buttons" | sizedList(4, digital).imap(_.toHList)(_.toSized[List]).as[XOXO]

    import shapeless._

    val keys2: Codec[Keys] = {

      val x = (l3 :: r3).flatAppend {
        case _ :: _ =>
          (xoxo :: arrows) :: digital :: digital
      }

      val _ = x
      (??? : Codec[Keys])
    }

    val _ = keys2

    def bracketed[Prefix, Inner <: HList, Suffix, Merged <: HList, Prepended <: HList, MergedLength <: Nat](
      prefix: Codec[Prefix],
      inner: Codec[Inner],
      suffix: Codec[Suffix]
    )(
      merge: (Prefix, Suffix) => Merged
    )(
      split: Merged => (Prefix, Suffix)
    )(
      implicit prepend: Prepend.Aux[Merged, Inner, Prepended],
      splitInstance: Split.Aux[Prepended, MergedLength, Merged, Inner]
    ): Codec[Prepended] =
      (prefix :: inner :: suffix).imap { case a :: b :: c :: HNil => merge(a, c) ::: b } { prepended =>
        val (merged, b) = prepended.split[MergedLength]
        val (a, c) = split(merged)
        a :: b :: c :: HNil
      }

    val keys =
      (bracketed(
        l3 :: r3,
        xoxo :: arrows,
        ("right stick press" | digital) :: ("left stick press" | digital)
      ) {
        case ((leftAnalogs :: rightAnalogs), (r3Press :: l3Press :: HNil)) =>
          Stick.fromAnalogs(leftAnalogs)(l3Press) :: Stick.fromAnalogs(rightAnalogs)(r3Press) :: HNil
      } {
        case l3 :: r3 :: HNil =>
          val leftAnalogs = l3.x :: l3.y :: HNil
          val rightAnalogs = r3.x :: r3.y :: HNil

          (leftAnalogs :: rightAnalogs, r3.pressed :: l3.pressed :: HNil)
      } :+ ("options" | digital) :+ ("share" | digital)).as[Keys]

    (header ~> keys).as[Dualshock]
  }
}

object Key {
  final case class Digital(on: Boolean)
  final case class Analog(value: Byte)
  final case class Stick(pressed: Digital, x: Analog, y: Analog)

  object Stick {
    import shapeless._

    def fromAnalogs: Analog :: Analog :: HNil => Digital => Stick = {
      case x :: y :: HNil => pressed => Stick(pressed, x, y)
    }
  }
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
  arrows: Key.Arrows,
  //meta
  options: Key.Digital,
  share: Key.Digital
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
