enablePlugins(GatlingPlugin)

name := """reactive-lab6"""

version := "1.6"

scalaVersion := "2.13.6"

val akkaVersion = "2.7.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"     % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed"   % akkaVersion,
  "com.typesafe.akka" %% "akka-http"            % "10.4.0",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.4.0",
  "io.gatling"        % "gatling-http"          % "3.8.4",
  "org.scalatest"     %% "scalatest"            % "3.2.14" % "test"
)

dependencyOverrides ++= List(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1",
)
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.8.4" % "test,it"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "3.8.4" % "test,it"
