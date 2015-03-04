name := "krispii-core"

organization := "ca.shiftfocus"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.5"

crossScalaVersions := Seq("2.10.4", "2.11.5")

// trivial change to trigger a build

resolvers ++= Seq(
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases",
  "ShiftFocus" at "https://maven.shiftfocus.ca/repositories/releases",
  "ShiftFocus Snapshots" at "https://maven.shiftfocus.ca/repositories/snapshots",
  "Sonatype" at "https://oss.sonatype.org/content/repositories/releases",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"//,
  //"Kahn's Repo" at "http://repo.kahn.ws/maven/snapshots"
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
  "org.scalaz" %% "scalaz-core" % "7.1.1",
  "org.scalaz" %% "scalaz-effect" % "7.1.1",
  "com.github.mauricio" %% "postgresql-async" % "0.2.15",
  "joda-time" % "joda-time" % "2.1",
  "net.sf.uadetector" % "uadetector-resources" % "2014.04",
  "net.debasishg" %% "redisclient" % "2.13",
  "com.github.cb372" %% "scalacache-redis" % "0.4.1",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
  "ca.shiftfocus" %% "webcrank-password" % "0.4.1",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  "ca.shiftfocus" %% "uuid" % "1.0.0",
  "ws.kahn" %% "ot" % "1.0-SNAPSHOT"
)

// -- SBT Publish settings --------
// Please ensure that your public key is appended to /home/maven/.ssh/authorized_keys for the
// maven user at maven.shiftfocus.ca. See the readme for more information.

publishMavenStyle := true

publishArtifact in (Compile, packageDoc) := false

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

parallelExecution in Test := false
