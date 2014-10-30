name := "krispii-core"

organization := "com.shiftfocus"

version := "1.0.x"

scalaVersion := "2.11.2"

resolvers ++= Seq(
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases"
)

// Scala compiler options
scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-language:higherKinds", // enable higher kinded types
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Xlint", // recommended additional warnings
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.3.5",
  "com.github.mauricio" %% "postgresql-async" % "0.2.15",
  "joda-time" % "joda-time" % "2.1",
  "ca.shiftfocus" %% "webcrank-password" % "0.4",
  "net.sf.uadetector" % "uadetector-resources" % "2014.04",
  "net.debasishg" %% "redisclient" % "2.13",
  "org.scala-lang" %% "scala-pickling" % "0.9.0",
  "com.github.cb372" %% "scalacache-redis" % "0.4.1"
)
