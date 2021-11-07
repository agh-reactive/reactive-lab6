package agh.reactive.routers_demo.group

import akka.actor.typed._
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey

/**
 * Simple worker that logs the content of the received work and stops;
 * Registers under `WorkerKey`
 */
object GroupWorker {
  val WorkerKey: ServiceKey[Work] = ServiceKey[Work]("Worker")
  case class Work(work: String)

  def apply(): Behavior[Work] = Behaviors.setup { context =>
    context.system.receptionist ! Receptionist.Register(WorkerKey, context.self)

    Behaviors.receive[Work]((context, msg) =>
      msg match {
        case Work(work) =>
          context.log.info(s"Actor: ${context.self}; I got to work on $work")
          Behaviors.same
      }
    )
  }

}

/**
 * Master that spawns `nbOfRoutees` local workers registered under WorkKey
 * creates group router and distributes the work between workers
 * @see
 *   https://doc.akka.io/docs/akka/current/typed/routers.html#group-router
 */
object GroupMaster {
  case class WorkToDistribute(work: String)

  def apply(nbOfRoutees: Int): Behavior[WorkToDistribute] = Behaviors.setup { context =>
    (1 to nbOfRoutees).foreach(i => context.spawn(GroupWorker(), s"worker-$i"))

    val group = Routers.group[GroupWorker.Work](GroupWorker.WorkerKey)
    val router = context.spawn(group, "worker-group")
    context.watch(router)

    Behaviors
      .receiveMessage[WorkToDistribute] { case WorkToDistribute(work) =>
        router ! GroupWorker.Work(work)
        Behaviors.same
      }
      .receiveSignal { case (context, Terminated(router)) =>
        context.log.info("Router is terminated.")
        context.system.terminate()
        Behaviors.stopped
      }
  }
}

/**
 * Runs the Master actor that demonstrates the behaviour of local pool routers
 */
object LocalGroupRoutersDemo extends App {
  val nbOfRoutees = 5

  ActorSystem(
    Behaviors.setup[Any] { context =>
      val master = context.spawn(GroupMaster(nbOfRoutees), "master")

      // send work to distribute
      (1 to nbOfRoutees).foreach { i =>
        master ! GroupMaster.WorkToDistribute(s"some work $i")
      }

      Behaviors.same
    },
    "ReactiveRouters"
  )
}
