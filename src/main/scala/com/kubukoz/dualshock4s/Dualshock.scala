package com.kubukoz.dualshock4s

import scodec.Codec
import scodec.codecs._
import scodec.bits._
import com.kubukoz.dualshock4s.Key._
import cats.implicits._
import scodec.interop.cats._
import com.kubukoz.dualshock4s.Key.Bumper.NotPressed
import com.kubukoz.dualshock4s.Key.Bumper.Pressed
import scodec.Attempt

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

    val arrows = Codec[Arrows]

    val digital = "binary value" | bool.as[Digital]

    // would've been nice to use sizedList(4)
    val xoxo = "action buttons" | (digital :: digital :: digital :: digital).as[XOXO]

    def sticks[Between <: Tuple](between: Codec[Between]): Codec[Tuple.Concat[(Stick, Stick), Between]] =
      (
        l3 :: r3 :: between :: ("right stick press" | digital) :: ("left stick press" | digital)
      ).imap { (l3_, r3_, between_, rightStick, leftStick) =>
        Stick.fromAnalogs(l3_)(leftStick) *: Stick.fromAnalogs(r3_)(rightStick) *: between_
      } { case l3 *: r3 *: between_ =>
        val leftAnalogs = (l3.x, l3.y)
        val rightAnalogs = (r3.x, r3.y)

        (leftAnalogs, rightAnalogs, between_, r3.pressed, l3.pressed)
      }

    def bumperCodec(pressed: Boolean): Codec[Bumper] =
      either(
        provide(pressed),
        analog.unit(Analog(0)),
        analog
      ).imap(Bumper.fromEither)(_.toEither)

    def bumpers[Between <: Tuple](betweenCodec: Codec[Between]): Codec[Tuple.Concat[(Bumper, Bumper), Between]] = {
      val prefix = ("R2" | digital) :: ("L2" | digital)

      prefix
        .consume { (r2Pressed, l2Pressed) =>
          betweenCodec :: bumperCodec(l2Pressed.on) :: bumperCodec(r2Pressed.on)
        } { (_, l2, r2) =>
          (r2.isOn, l2.isOn)
        }
        .imap { (between, l2, r2) =>
          l2 *: r2 *: between
        } { case l2 *: r2 *: between =>
          (between, l2, r2)
        }
    }

    val extras = "counter, tpad, ps button: todo" | byte.unit(0)

    val keys = {
      sticks(between = xoxo :: arrows) ++
        (("options" | digital) :: ("share" | digital)) ++
        bumpers(
          betweenCodec = ("R1" | digital) :: ("L1" | digital) <~ extras
        )
    }.as[Keys]

    (header ~> keys).as[Dualshock]
  }

}

object Key {

  final case class Digital(on: Boolean)
  final case class Analog(value: Byte)
  final case class Stick(pressed: Digital, x: Analog, y: Analog)

  enum Bumper {
    case NotPressed
    case Pressed(strength: Analog)

    def isOn: Digital = Digital(fold(false, _ => true))

    def fold[A](notPressed: => A, pressed: Analog => A): A = this match {
      case NotPressed        => notPressed
      case Pressed(strength) => pressed(strength)
    }

    def toEither: Either[Unit, Analog] = fold(Left(()), _.asRight)
  }

  object Bumper {
    val fromEither: Either[Unit, Analog] => Bumper = _.fold(_ => NotPressed, Pressed.apply)
  }

  object Stick {

    def fromAnalogs: (Analog, Analog) => Digital => Stick = { (x, y) => pressed => Stick(pressed, x, y) }

  }

  enum Arrows {
    case Neither
    case Left
    case Up
    case Right
    case Down
    case UpLeft
    case DownLeft
    case DownRight
    case UpRight
  }

  object Arrows {
    given Codec[Arrows] = "arrows" | mappedEnum(
      uint4,
      Arrows.Up -> 0,
      Arrows.UpRight -> 1,
      Arrows.Right -> 2,
      Arrows.DownRight -> 3,
      Arrows.Down -> 4,
      Arrows.DownLeft -> 5,
      Arrows.Left -> 6,
      Arrows.UpLeft -> 7,
      Arrows.Neither -> 8
    )

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
  enum Headphones {
    case Connected extends Headphones
    case Disconnected extends Headphones
  }
}
