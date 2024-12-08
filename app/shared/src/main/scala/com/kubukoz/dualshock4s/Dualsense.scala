package com.kubukoz.dualshock4s

import scodec.Codec
import scodec.codecs._
import com.kubukoz.dualshock4s.Key._
import cats.implicits._
import scodec.Iso

final case class Dualsense(
  keys: Keys,
  micMute: Digital,
  touchPanelPress: Digital,
  psButton: Digital,
)

object Dualsense {

  val codec: Codec[Dualsense] = {
    import codecs.*

    (
      header
        :: l3.as[Analog2]
        :: r3.as[Analog2]
        :: ("L2 axis" | analog)
        :: ("R2 axis" | analog)
        :: ignore(8)
        :: xoxo
        :: arrows
        :: ("r3 press" | digital)
        :: ("l3 press" | digital)
        :: ("options" | digital)
        :: ("create" | digital)
        :: ("R2" | digital)
        :: ("L2" | digital)
        :: ("R1" | digital)
        :: ("L1" | digital)
        :: ignore(5)
        :: ("mic mute" | digital)
        :: ("touch panel press" | digital)
        :: ("ps button" | digital)
    )
      .dropUnits
      .as[Underlying]
      .xmap(fromUnderlying, toUnderlying)
  }

  private def fromUnderlying(underlying: Underlying): Dualsense = Dualsense(
    keys = Keys(
      l3 = Stick.from(underlying.l3, underlying.l3Button),
      r3 = Stick.from(underlying.r3, underlying.r3Button),
      xoxo = underlying.xoxo,
      arrows = underlying.arrows,
      options = underlying.options,
      share = underlying.create,
      l2 = Bumper.from(underlying.l2, underlying.l2Button),
      r2 = Bumper.from(underlying.r2, underlying.r2Button),
      r1 = underlying.r1,
      l1 = underlying.l1,
    ),
    micMute = underlying.micMute,
    touchPanelPress = underlying.touchpanelPress,
    psButton = underlying.ps,
  )

  private def toUnderlying(dualsense: Dualsense): Underlying =
    Underlying(
      l3 = dualsense.keys.l3.analogs,
      r3 = dualsense.keys.r3.analogs,
      l2 = dualsense.keys.l2.analog,
      r2 = dualsense.keys.r2.analog,
      xoxo = dualsense.keys.xoxo,
      arrows = dualsense.keys.arrows,
      r3Button = dualsense.keys.r3.pressed,
      l3Button = dualsense.keys.l3.pressed,
      options = dualsense.keys.options,
      create = dualsense.keys.share,
      r2Button = dualsense.keys.r2.isOn,
      l2Button = dualsense.keys.l2.isOn,
      r1 = dualsense.keys.r1,
      l1 = dualsense.keys.l1,
      micMute = dualsense.micMute,
      touchpanelPress = dualsense.touchPanelPress,
      ps = dualsense.psButton,
    )

  case class Underlying(
    l3: Analog2,
    r3: Analog2,
    l2: Analog,
    r2: Analog,
    xoxo: XOXO,
    arrows: Arrows,
    r3Button: Digital,
    l3Button: Digital,
    options: Digital,
    create: Digital,
    r2Button: Digital,
    l2Button: Digital,
    r1: Digital,
    l1: Digital,
    micMute: Digital,
    touchpanelPress: Digital,
    ps: Digital,
  )

}
