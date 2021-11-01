package agh.reactive.routers_demo
import io.gatling.core.Predef.{Simulation, StringBody, jsonFile, rampUsers, scenario, _}
import io.gatling.http.Predef.http

import scala.concurrent.duration._

class HttpWorkerGatlingTest extends Simulation {

  val httpProtocol = http  //values here are adjusted to cluster_demo.sh script
    .baseUrls("http://localhost:9001", "http://localhost:9002", "http://localhost:9003")
    //.baseUrls("http://localhost:9001")
    .acceptHeader("text/plain,text/html,application/json,application/xml;")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val scn = scenario("BasicSimulation")
    .feed(jsonFile(classOf[HttpWorkerGatlingTest].getResource("/data/work_data.json").getPath).random)
    .exec(
      http("work_basic")
        .post("/work")
        .body(StringBody("""{ "work": "${work}" }"""))
        .asJson
    )
    .pause(1)

  setUp(
    scn.inject(constantConcurrentUsers(1000).during(30.seconds))
  ).protocols(httpProtocol)
}
