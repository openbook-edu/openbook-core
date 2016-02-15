name := "krispii-core"

organization := "ca.shiftfocus"

version := scala.io.Source.fromFile("VERSION").mkString("").trim

scalaVersion := "2.11.6"

crossScalaVersions := Seq("2.10.4", "2.11.6")

resolvers ++= Seq(
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases",
  "ShiftFocus" at "https://maven.shiftfocus.ca/repositories/releases",
  "ShiftFocus Snapshots" at "https://maven.shiftfocus.ca/repositories/snapshots",
  "Sonatype" at "https://oss.sonatype.org/content/repositories/releases",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
  "Linter Repository" at "https://hairyfotr.github.io/linteRepo/releases"
)

// Scala compiler options
scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xlint",
  "-Ybackend:GenBCode",
  "-Ywarn-adapted-args",
  "-Ywarn-value-discard",
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code",
  "-P:linter:disable:OptionOfOption+PreferIfToBooleanMatch"
)

libraryDependencies ++= Seq(
  // We depend on several parts of the Play project
  "com.typesafe.play" %% "play-json" % "2.4.0",
  "com.typesafe.play" %% "play-jdbc-evolutions" % "2.4.0",
  // We heavily depend on scalaz's \/ and associated types
  "org.scalaz" %% "scalaz-core" % "7.1.2",
  "com.github.mauricio" %% "postgresql-async" % "0.2.18",
  "joda-time" % "joda-time" % "2.1",
  "net.sf.uadetector" % "uadetector-resources" % "2014.04",
  "com.github.cb372" %% "scalacache-redis" % "0.6.1",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
  "ca.shiftfocus" %% "webcrank-password" % "0.4.1",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  "ca.shiftfocus" %% "sflib" % "1.0.6",
  "junit" % "junit" % "4.12" % "test",
  "ws.kahn" %% "ot" % "1.0-SNAPSHOT"
)

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.12")

import scalariform.formatter.preferences._
scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(CompactControlReadability, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, false)

// -- SBT Publish settings --------
// Please ensure that your public key is appended to /home/maven/.ssh/authorized_keys for the
// maven user at maven.shiftfocus.ca. See the readme for more information.

publishMavenStyle := true

//publishArtifact in (Compile, packageDoc) := false

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
