package agh.reactive.routers_demo
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, post}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.Try

/**
 * A [[RegisteredHttpWorker]] that registers itself with the receptionist
 * @see
 *   https://doc.akka.io/docs/akka/current/typed/actor-discovery.html#receptionist
 */
object RegisteredHttpWorker {
  val HttpWorkerKey: ServiceKey[HttpWorker.Command] = ServiceKey("HttpWorker")

  def apply(): Behavior[HttpWorker.Command] = Behaviors.setup { context =>
    context.system.receptionist ! Receptionist.Register(HttpWorkerKey, context.self)
    context.log.info(s"New worker spawned, on actor system: ${context.system.name}")

    Behaviors.receive((context, msg) =>
      msg match {
        case HttpWorker.Work(work, replyTo) =>
          context.log.info(s"I got to work on $work")
          replyTo ! HttpWorker.WorkerResponse("Done")
          Behaviors.same
      }
    )
  }
}

/**
 * Spawns a node with workers registered under recepcionist
 */
object WorkerClusterNodeApp extends App {
  private val config = ConfigFactory.load()
  private val httpWorkersNodeCount = 2

  val system = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { ctx =>
      // spawn workers
      val workersNodes = for (i <- 1 to httpWorkersNodeCount) yield ctx.spawn(RegisteredHttpWorker(), s"worker$i")
      Behaviors.same
    },
    "ClusterWorkRouters",
    config
      .getConfig(Try(args(0)).getOrElse("cluster-default"))
      .withFallback(config.getConfig("cluster-default"))
  )

  Await.ready(system.whenTerminated, Duration.Inf)
}

/**
 * Start HTTP server with Group Router
 */
object WorkHttpClusterNodeApp extends App {
  val workHttpServerInCluster = new WorkHttpServerInCluster()
  workHttpServerInCluster.run(args(0).toInt)
}

/**
 * The server that distributes all of the requests to the workers registered in the cluster via the Group Router under
 * recepcionist ServiceKey. Seed nodes have to be spawned separately, see ClusterNodeApp
 * @see
 *   https://doc.akka.io/docs/akka/current/typed/routers.html#group-router
 */
class WorkHttpServerInCluster() extends JsonSupport {
  private val config = ConfigFactory.load()

  implicit val system = ActorSystem[Nothing](
    Behaviors.empty,
    "ClusterWorkRouters",
    config.getConfig("cluster-default")
  )

  implicit val scheduler = system.scheduler
  implicit val executionContext = system.executionContext

  // distributed Group Router, workers possibly on different nodes
  val workers = system.systemActorOf(Routers.group(RegisteredHttpWorker.HttpWorkerKey), "clusterWorkerRouter")

  implicit val timeout: Timeout = 5.seconds

  def routes: Route = path("work") {
    post {
      entity(as[WorkDTO]) { workDto =>
        complete {
          println(s"Work to be distributed [${workDto.work}]")
          workers.ask(replyTo => HttpWorker.Work(workDto.work, replyTo))
        }
      }
    }
  }

  def run(port: Int): Unit = {
    val bindingFuture = Http().newServerAt("localhost", port).bind(routes)
    println(s"Server now online.\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete { _ =>
        system.terminate()
      } // and shutdown when done
  }
}
