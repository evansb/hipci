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
    case Post(entity) => QueryOk(db.save(entity))
    case Put(testID, newEntity) =>
        val get = db.query[ConfigSchema]
          .whereEqual("testID", testID)
          .fetchOne()
        get match {
          case Some(_) => QueryOk(db.save(newEntity))
          case None => QueryNotFound
        }
    case Get(testID) =>
      val get = db.query[ConfigSchema]
        .whereEqual("testID", testID)
        .fetchOne()
      get match {
        case Some(e) => QueryOk(e)
        case None => QueryNotFound
      }
  }

  override def receive = {
    case query:DbQuery => sender ! handleQuery(query)
    case other => super.receive(other)
  }
}
