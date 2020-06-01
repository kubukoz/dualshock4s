package com.kubukoz.dualshock4s

import scodec.Codec
import scodec.codecs._
import scodec.bits._
import com.kubukoz.dualshock4s.Key._
import cats.implicits._
import scodec.interop.cats._
import shapeless.ops.hlist.Prepend
import shapeless.ops.hlist.Split
import shapeless.HList
import shapeless.::
import shapeless.HNil
import shapeless.Nat
import com.kubukoz.dualshock4s.Key.Bumper.NotPressed
import com.kubukoz.dualshock4s.Key.Bumper.Pressed
import scodec.Attempt

final case class Dualshock(keys: Keys /*, touch: Touch, motion: Motion, info: Info */ )

object Dualshock {

  /**
    * This codec will read prefix, inner, then suffix, then merge the prefix & suffix into a single HList,
    * which will be prepended to the inner part. Merging and splitting is done using the functions provided by the user.
    */
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
    (prefix :: inner :: suffix).imap {
      case a :: b :: c :: HNil =>
        merge(a, c) ::: b
    } { prepended =>
      val ((a, c), b) = prepended.split[MergedLength].leftMap(split)
      a :: b :: c :: HNil
    }

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

    def sticks[Between <: HList](between: Codec[Between]): Codec[Stick :: Stick :: Between] =
      bracketed(
        l3 :: r3,
        between,
        ("right stick press" | digital) :: ("left stick press" | digital)
      ) {
        case ((leftAnalogs :: rightAnalogs), (r3Press :: l3Press :: HNil)) =>
          Stick.fromAnalogs(leftAnalogs)(l3Press) :: Stick.fromAnalogs(rightAnalogs)(r3Press) :: HNil
      } {
        case l3 :: r3 :: HNil =>
          val leftAnalogs = l3.x :: l3.y :: HNil
          val rightAnalogs = r3.x :: r3.y :: HNil

          (leftAnalogs :: rightAnalogs, r3.pressed :: l3.pressed :: HNil)
      }

    def bumperCodec(pressed: Boolean): Codec[Bumper] =
      either(
        provide(pressed),
        analog.unit(Analog(0)),
        analog
      ).imap(Bumper.fromEither)(_.toEither)

    def bumpers[Between <: HList](betweenCodec: Codec[Between]): Codec[Bumper :: Bumper :: Between] = {
      val prefix = ("R2" | digital) :: ("L2" | digital)

      prefix
        .consume {
          case r2Pressed :: l2Pressed :: HNil => betweenCodec :: bumperCodec(l2Pressed.on) :: bumperCodec(r2Pressed.on)
        } {
          case _ :: l2 :: r2 :: HNil => r2.isOn :: l2.isOn :: HNil
        }
        .imap {
          case between :: l2 :: r2 :: HNil => l2 :: r2 :: between
        } {
          case l2 :: r2 :: between => between :: l2 :: r2 :: HNil
        }
    }

    val extras = "counter, tpad, ps button: todo" | byte.unit(0)

    val keys = {
      sticks(between = xoxo :: arrows) ::: {
        ("options" | digital) ::
          ("share" | digital) ::
          bumpers(betweenCodec = ("R1" | digital) :: ("L1" | digital) <~ extras)
      }
    }.as[Keys]

    (header ~> keys).as[Dualshock]
  }
}

object Key {
  final case class Digital(on: Boolean)
  final case class Analog(value: Byte)
  final case class Stick(pressed: Digital, x: Analog, y: Analog)

  sealed trait Bumper extends Product with Serializable {

    def isOn: Digital = Digital(fold(false, _ => true))

    def fold[A](notPressed: => A, pressed: Analog => A): A = this match {
      case NotPressed        => notPressed
      case Pressed(strength) => pressed(strength)
    }
    def toEither: Either[Unit, Analog] = fold(Left(()), _.asRight)
  }

  object Bumper {
    case object NotPressed extends Bumper
    final case class Pressed(strength: Analog) extends Bumper

    val fromEither: Either[Unit, Analog] => Bumper = _.fold(_ => NotPressed, Pressed)
  }

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
  share: Key.Digital,
  l2: Key.Bumper,
  r2: Key.Bumper,
  r1: Key.Digital,
  l1: Key.Digital
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
