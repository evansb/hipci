package edu.nus.hipci.daemon

import akka.actor.ActorSystem
import sorm._
import edu.nus.hipci.core._

object Main {
 object defaultDatabase extends Instance(
    entities = Set(Entity[DbEntity]()),
    url = "jdbc:h2:./hipci",
    initMode = InitMode.Create,
    poolSize = 20)
}

/**
 * A `Runnable` that starts a Daemon actor.
 */
class Main(database: Instance) extends Runnable {
  override def run(): Unit = {
    val system = ActorSystem(AppName, AppConfiguration.getServerConfig())
    if (Daemon.get(system).isEmpty) {
      val (daemon, _) = Daemon.withDbAccess(database)
      daemon.register(system)
      system.actorOf(daemon.props, "Daemon")
      Console.out.println("Daemon started")
      while (!Thread.interrupted()) {
      }
      system.shutdown()
    }
  }
}
