
name := "expenseservice"
organization := "com.expense"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.16"

libraryDependencies ++= Seq(
  guice,
  "com.typesafe.play" %% "play-slick" % "5.1.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0",
  "mysql" % "mysql-connector-java" % "8.0.33",
  "com.typesafe.play" %% "play-json" % "2.10.0",

  // gRPC / ScalaPB
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % "0.11.13",
  "io.grpc" % "grpc-netty-shaded" % "1.62.2",

  // Test dependencies
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,
  "org.scalatest"          %% "scalatest"          % "3.2.19" % Test,
  "org.mockito"            %% "mockito-scala"      % "1.17.14" % Test,
  "com.typesafe.akka"      %% "akka-stream"        % "2.8.0"
)

dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "2.2.0"

import sbtprotoc.ProtocPlugin.autoImport.{PB, _}

Compile / PB.targets := Seq(
  scalapb.gen(grpc = true) -> (Compile / sourceManaged).value / "scalapb"
)
Compile / PB.protoSources := Seq(file("src/main/protobuf"))