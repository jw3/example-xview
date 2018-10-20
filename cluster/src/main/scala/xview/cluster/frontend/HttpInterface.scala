package xview.cluster.frontend

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import xview.cluster.api
import xview.cluster.api._

trait HttpInterface {
  def system: ActorSystem
  def mat: Materializer

  def routes(ctrl: ActorRef)(implicit to: Timeout): Route =
    pathPrefix("api") {
      extractExecutionContext { implicit ec ⇒
        path("job") {
          post {
            entity(as[JobDescriptor]) { desc ⇒
              onSuccess(ctrl ? api.SubmitJob(desc)) {
                case JobAccepted(id) ⇒
                  complete(StatusCodes.Accepted → id)
                case JobRejected ⇒
                  complete(StatusCodes.NotAcceptable)
                case _ ⇒
                  complete(StatusCodes.InternalServerError)
              }
            }
          }
        } ~
          path("health") {
            complete(StatusCodes.OK)
          }
      }
    }
}
