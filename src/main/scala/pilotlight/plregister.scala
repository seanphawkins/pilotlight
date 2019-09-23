package pilotlight

import scopt.OParser

object plregister extends App {

  case class Arguments(bridgeIp: String = "", userName: String = "")

  val builder = OParser.builder[Arguments]
  val parser1 = {
    import builder._
    OParser.sequence(
      programName("plregister"),
      head("plregister", "0.1"),
      opt[String]('i', "bridgeip")
        .required()
        .valueName("<ip address>")
        .action((x, c) => c.copy(bridgeIp = x))
        .text("bridge IP address"),
      arg[String]("<username>")
        .required()
        .action((x, c) => c.copy(userName = x))
        .text("user name to register"),
    )
  }

  OParser.parse(parser1, args, Arguments()) match {
    case Some(config) =>
      val (ut, hb) = HueBridge.registerUser(config.bridgeIp, config.userName)
      println("User token -> " + ut)
      hb.close()
    case _ =>
      println("Illegal arguments")
  }
}
