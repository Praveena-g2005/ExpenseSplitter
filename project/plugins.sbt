// Use the correct Play plugin repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Play Framework SBT plugin (for Play 2.9.x)
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.4")

// ScalaPB plugin for gRPC
// addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")

// Optional: Code formatting
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// Optional: Dependency updates checker
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.14"
