import AssemblyPlugin._
import net.virtualvoid.sbt.graph.Plugin.graphSettings

lazy val commonSettings = Seq(
  organization := "org.evansb",
  version := "0.0.1",
  scalaVersion := "2.11.6"
)

lazy val scopt = "com.github.scopt" %% "scopt" % "3.3.0"

lazy val scalaTest = ("org.scalatest" %% "scalatest" % "2.2.4" % "test").excludeAll(
  ExclusionRule(name = "scala-xml"),
  ExclusionRule(name = "scala-parser-combinators")
)

lazy val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT"

lazy val akkaRemote = "com.typesafe.akka" %% "akka-remote" % "2.4-SNAPSHOT"

lazy val typesafeConfig = "com.typesafe" % "config" % "1.3.0"

lazy val config = "com.github.kxbmap" %% "configs" % "0.2.4"

lazy val log4s = "org.log4s" %% "log4s" % "1.1.5"

lazy val scalaRainbow = "pl.project13.scala" %% "rainbow" % "0.2"

lazy val sorm = "org.sorm-framework" % "sorm" % "0.3.18"

lazy val hipci = (project in file(".")).
  settings(commonSettings: _*).
  settings(assemblySettings: _*).
  settings(graphSettings: _*).
  settings(
    name := "hipci",
    version := "1.0",
    resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/",
    logLevel := Level.Error,
    conflictWarning := ConflictWarning.disable,
    libraryDependencies ++= Seq(scalaTest, akkaActor, akkaRemote, typesafeConfig, log4s, scopt,
      config, scalaRainbow, sorm),
    scalaSource in Compile := baseDirectory.value / "src/main",
    scalaSource in Test := baseDirectory.value / "src/test",
    scalacOptions ++= Seq("-deprecation", "-feature"),
    mainClass in (Compile, run) := Some("scala.hipci.cli.CLIApp"),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(defaultShellScript)),
    assemblyJarName in assembly := "hipci",
    test in assembly := {},
    parallelExecution in Test := false,
    mainClass in assembly := Some("scala.hipci.cli.CLIApp"),
    assemblyOutputPath in assembly := baseDirectory.value / "bin" / "hipci"
  )


