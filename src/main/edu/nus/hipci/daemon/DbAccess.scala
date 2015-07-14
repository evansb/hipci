package edu.nus.hipci.daemon

import akka.actor.Props
import sorm._

import edu.nus.hipci.common._

/**
 * Actor for accessing database
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */

object DbAccess {

  object defaultDatabase extends Instance(
    entities = Set(Entity[GenTest](), Entity[TestConfiguration]()),
    url = "jdbc:h2:mem:hipci-test")

  def apply(db: Instance) = {
    new ComponentDescriptor  {
      val name = "DbAccess"
      val props = Props.create(classOf[DbAccess], db)
      val subComponents = List.empty
    }
  }
}

class DbAccess(db: Instance) extends Component {
  import request._
  import response._

  protected val descriptor = DbAccess(db)

  private val logger = Logging.toStdout()

  private def saveTestConfiguration(entity: TestConfiguration) = {
    val persistedTests = entity.tests.mapValues { s => s.map { t => db.save(t) } }
    db.save(entity.copy(tests = persistedTests))
  }

  protected def handleQuery(query: DbQuery) = query match {
    case Post(entity) =>
      logger.good(s"POST ${ entity.testID }")
      QueryOk(saveTestConfiguration(entity))
    case Put(testID, newEntity) =>
      val get = db.query[TestConfiguration]
        .whereEqual("testID", testID)
        .fetchOne()
      get match {
        case Some(_) =>
          logger.good(s"PUT ${ newEntity.testID }")
          QueryOk(saveTestConfiguration(newEntity))
        case None =>
          logger.bad(s"404 ${ newEntity.testID }")
          QueryNotFound
      }
    case Get(testID) =>
      logger.good(s"GET ${ testID }")
      val get = db.query[TestConfiguration]
        .whereEqual("testID", testID)
        .fetchOne()
      get match {
        case Some(e) =>
          logger.good(s"OK ${ testID }")
          QueryOk(e)
        case None =>
          logger.bad(s"404 ${ testID }")
          QueryNotFound
      }
  }

  override def receive = {
    case query:DbQuery => sender ! handleQuery(query)
    case other => super.receive(other)
  }
}
