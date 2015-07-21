package edu.nus.hipci.daemon

import akka.actor.Props
import sorm._

import edu.nus.hipci.core._

/**
 * Actor for accessing database
 *
 * @author Evan Sebastian <evanlhoini@gmail.com>
 */

case class DbEntity(testID: String, json: String)

object DbAccess {
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
