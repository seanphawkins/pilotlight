package pilotlight

import java.io.File

import com.typesafe.config.ConfigFactory
import scopt.OParser

object pilotlight extends App {

  case class Arguments(
    bridgeIp: Option[String] = None,
    userToken: Option[String] = None,
    configFile: Option[String] = None,
    command: String = "",
    lights: Seq[String] = Seq.empty,
    onOff: Option[Boolean] = None,
    brightness: Option[Int] = None,
    hue: Option[Int] = None,
    saturation: Option[Int] = None,
  )

  val builder = OParser.builder[Arguments]
  val parser1 = {
    import builder._
    OParser.sequence(
      programName("pilotlight"),
      head("pilotlight", "0.1"),
      opt[String]('i', "bridgeip")
        .valueName("<ip address>")
        .action((x, c) => c.copy(bridgeIp = Option(x)))
        .text("bridge IP address"),
      opt[String]('u', "usertoken")
        .valueName("<user token>")
        .action((x, c) => c.copy(userToken = Option(x)))
        .text("user token"),
      opt[String]('c', "config")
        .valueName("<config filename>")
        .action((x, c) => c.copy(configFile = Option(x)))
        .text("user token"),
      arg[String]("<light name>...")
        .unbounded()
        .required()
        .action((x, c) => c.copy(lights = x +: c.lights))
        .text("lights to be affected"),
      cmd("list")
        .action((_, c) => c.copy(command = "list"))
        .text("list name of available lights"),
      cmd("on")
        .action((_, c) => c.copy(command = "on"))
        .text("turn specified lights on"),
      cmd("off")
        .action((_, c) => c.copy(command = "off"))
        .text("turn specified lights off"),
      cmd("ctl")
        .action((_, c) => c.copy(command = "ctl"))
        .text("change settings of specified lights")
        .children(
          opt[Boolean]('x' , "power")
            .action((x, c) => c.copy(onOff = Option(x)))
            .text("set on/off state"),
          opt[Int]('b', "brightness")
            .action((x, c) => c.copy(brightness = Option(x)))
            .text("new brightness setting"),
          opt[Int]('h', "hue")
            .action((x, c) => c.copy(hue = Option(x)))
            .text("new hue setting"),
          opt[Int]('s', "saturation")
            .action((x, c) => c.copy(saturation = Option(x)))
            .text("new saturation setting"),
        )
    )
  }

  OParser.parse(parser1, args, Arguments()) match {
    case Some(config) =>
      val hb: HueBridge =
        (config.configFile map { fn: String =>
          val cfg = ConfigFactory.parseFile(new File(fn))
          HueBridge(cfg.getString("hue.ip"), cfg.getString("hue.user"))
        }).getOrElse((for (ip <- config.bridgeIp; ut <- config.userToken) yield HueBridge(ip, ut)).get)
      val regexTargets = config.lights.map(mkRegex)
      val targetLights = hb.lights.filter{ l => regexTargets.exists(_.matches(l._2.name))  }
      config.command match {
        case "list" =>
          targetLights foreach { l => println(l._2.name) }
        case "on" =>
          hb.update(targetLights map { case (n, l) => (n,LightStateUpdate(on = Option(true))) })
        case "off" =>
          hb.update(targetLights map { case (n, l) => (n,LightStateUpdate(on = Option(false))) })
        case "ctl" =>
          val u = LightStateUpdate(on = config.onOff, bri = config.brightness, hue = config.hue, sat = config.saturation)
          hb.update(targetLights map { case (n, l) => (n, u) })
      }
      hb.close()
    case _ =>
  }

  private def mkRegex(s: String) =
    s.replaceAll("\\.", "\\.")
      .replaceAll("\\?", ".")
      .replaceAll("\\*", ".*").r
}

