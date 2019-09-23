name := "PilotLight"

version := "0.1"

scalaVersion := "2.13.0"

val akkaVersion = "2.5.23"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.4",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.9",
  "com.github.scopt" %% "scopt" % "+",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % "0.52.2" % Compile,
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "0.52.2" % Provided,

)

mainClass in assembly := some("pilotlight.Main")
assemblyJarName := "pilotlight.jar"

val meta = """META.INF(.)*""".r
assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case n if n.startsWith("reference.conf") => MergeStrategy.concat
  case n if n.endsWith(".conf") => MergeStrategy.concat
  case meta(_) => MergeStrategy.discard
  case x => MergeStrategy.first
}