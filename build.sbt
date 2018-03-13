name := "krispii-core"

organization := "ca.shiftfocus"

version := scala.io.Source.fromFile("VERSION").mkString("").trim

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.10.4", "2.11.6", "2.12.4")

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
//  "-Ybackend:GenBCode",
  "-Ywarn-adapted-args",
  "-Ywarn-value-discard",
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code",
  "-P:linter:disable:OptionOfOption+PreferIfToBooleanMatch"
)

libraryDependencies ++= Seq(
  // We depend on several parts of the Play project
  "com.typesafe.play" %% "play-ws" % "2.6.7",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "com.typesafe.play" %% "play-jdbc-evolutions" % "2.6.7",
  "com.typesafe.play" %% "play-mailer" % "6.0.1",
  "com.typesafe.play" %% "play-json-joda" % "2.6.7",
  // We heavily depend on scalaz's \/ and associated types
  "com.github.mauricio" %% "postgresql-async" % "0.2.21",
  "joda-time" % "joda-time" % "2.9.9",
  "net.sf.uadetector" % "uadetector-resources" % "2014.04",
  "com.github.cb372" %% "scalacache-redis" % "0.22.0",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "org.clapper" %% "grizzled-slf4j" % "1.3.2",
  "junit" % "junit" % "4.12" % "test",
  "com.stripe" % "stripe-java" % "5.33.2",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "test",
  "ca.shiftfocus" % "webcrank-password_2.11" % "0.4.1",
  "org.scalaz" %% "scalaz-core" % "7.2.20",
  "ca.shiftfocus" %% "sflib" % "1.0.8",
  "ca.shiftfocus" %% "otlib" % "1.0.0"
)

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")

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
