name := "krispii-core"

organization := "ca.shiftfocus"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.2")

// trivial change to trigger a build

resolvers ++= Seq(
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases",
  "ShiftFocus" at "https://maven.shiftfocus.ca/repositories/releases",
  "Sonatype" at "https://oss.sonatype.org/content/repositories/releases",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
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
  "net.sf.uadetector" % "uadetector-resources" % "2014.04",
  "net.debasishg" %% "redisclient" % "2.13",
  "com.github.cb372" %% "scalacache-redis" % "0.4.1",
  "org.mockito" % "mockito-all" % "1.9.5" % "test",
  "org.specs2" %% "specs2" % "2.4.11" % "test",
  "ca.shiftfocus" %% "webcrank-password" % "0.4.1",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2"
)

// -- SBT Publish settings --------
// Please ensure that your public key is appended to /home/maven/.ssh/authorized_keys for the
// maven user at maven.shiftfocus.ca. See the readme for more information.

publishMavenStyle := true

publishTo := {
  val privateKeyFile = new java.io.File(sys.env("HOME") + "/.ssh/id_rsa")
  Some(Resolver.sftp(
    "ShiftFocus Maven Repository",
    "maven.shiftfocus.ca",
    50022,
    "/var/www/maven.shiftfocus.ca/repositories/" + {
      if (isSnapshot.value) "snapshots" else "releases"
    }
  ) as ("maven", privateKeyFile))
}
