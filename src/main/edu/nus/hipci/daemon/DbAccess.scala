package edu.nus.hipci.daemon

import akka.actor.Props
import sorm._
import edu.nus.hipci.core._

/**
 * Request handled by [[DbAccess]] instance
 *
 * Note the different naming conventions.
 */
sealed trait DbQuery

/** Post a test configuration to the database */
case class Post(content : TestConfiguration) extends DbQuery

/** Get a test configuration from the database */
case class Get(testID: String) extends DbQuery

/** Update a test configuration from the database */
case class Put(testID: String, content: TestConfiguration) extends DbQuery

/**
 * Response emitted by [[DbAccess]] instance
 *
 * Note the different naming conventions.
 */
sealed trait DbQueryResult

/** Indicates a successful query */
case class QueryOk(entity: TestConfiguration) extends DbQueryResult

/** Indicates that an element is not found */
case object QueryNotFound extends DbQueryResult

/**
 * @constructor Database entity for test configuration
 *
 * @param testID The test id
 * @param json The test configuration in JSON string format
 */
case class DbEntity(testID: String, json: String)

/** Factory object for [[DbAccess]]  descriptor */
object DbAccess {
  def apply(db: Instance) = {
    new ComponentDescriptor[DbAccess] {
      override val props = Props.create(classOf[DbAccess], db)
    }
  }
}

/** Process database queries. */
class DbAccess(db: Instance) extends Component {
  protected val descriptor = DbAccess(db)

  protected def handleQuery(query: DbQuery) = query match {
    case Post(entity) =>
      logger good s"POST ${ entity.testID }"
      db save DbEntity(entity.testID, TestConfiguration.toJSON(entity))
      QueryOk(entity)
    case Put(testID, newEntity) =>
      val get = db.query[DbEntity]
        .whereEqual("testID", testID)
        .fetchOne()
      get match {
        case Some(_) =>
          logger good s"PUT ${ newEntity.testID }"
          db save DbEntity(newEntity.testID, TestConfiguration.toJSON(newEntity))
          QueryOk(newEntity)
        case None =>
          logger bad s"404 ${ newEntity.testID }"
          QueryNotFound
      }
    case Get(testID) =>
      logger good s"GET ${ testID }"
      val get = db.query[DbEntity]
        .whereEqual("testID", testID)
        .fetchOne()
      get match {
        case Some(e) =>
          logger good s"OK ${ testID }"
          QueryOk(TestConfiguration.fromJSON(e.json))
        case None =>
          logger bad s"404 ${ testID }"
          QueryNotFound
      }
  }

  override def receive = {
    case query:DbQuery => sender ! handleQuery(query)
    case other => super.receive(other)
  }
}
