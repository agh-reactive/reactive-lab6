enablePlugins(GatlingPlugin)

name := """reactive-lab6"""

version := "1.4"

scalaVersion := "2.13.6"

val akkaVersion = "2.6.16"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"     % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed"   % akkaVersion,
  "com.typesafe.akka" %% "akka-http"            % "10.2.6",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.6",
  "io.gatling"        % "gatling-http"          % "3.5.1",
  "org.scalatest"     %% "scalatest"            % "3.2.9" % "test"
)

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.5.1" % "test,it"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "3.5.1" % "test,it"
