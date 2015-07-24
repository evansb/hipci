package edu.nus.hipci.daemon

import akka.actor.ActorSystem
import sorm._
import edu.nus.hipci.core._

/**
 * A `Runnable` that starts a Daemon actor.
 */
class Main() extends Runnable {

  override def run(): Unit = {
    val system = ActorSystem(AppName, DefaultServerConfig)
    if (Daemon.get(system).isEmpty) {
      object defaultDatabase extends Instance(
        entities = Set(Entity[DbEntity]()),
        url = "jdbc:h2:./hipci",
        initMode = InitMode.Create,
        poolSize = 20)
      val (daemon, _) = Daemon.withDbAccess(defaultDatabase)
      daemon.register(system)
      system.actorOf(daemon.props, "Daemon")
      Console.out.println("Daemon started")
      while (!Thread.interrupted()) {
      }
      system.shutdown()
    }
  }
}
