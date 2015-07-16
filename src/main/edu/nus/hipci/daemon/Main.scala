package edu.nus.hipci.daemon

import akka.actor.ActorSystem
import sorm._

import edu.nus.hipci.core._

/**
 * Entry point to Daemon process.
 */
class Main() extends Runnable {
  override def run(): Unit = {
    object defaultDatabase extends Instance(
      entities = Set(Entity[GenTest](), Entity[TestConfiguration]()),
      url = "jdbc:h2:./hipci",
      initMode = InitMode.Create,
      poolSize = 20)
    val (daemon, _) = Daemon.withDbAccess(defaultDatabase)
    val system = ActorSystem(AppName, DefaultServerConfig)
    daemon.register(system)
    system.actorOf(daemon.props, "Daemon")
  }
}
