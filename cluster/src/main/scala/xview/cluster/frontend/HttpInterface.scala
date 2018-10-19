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
import xview.cluster.api.{Job, JobAccepted, JobDescriptor, JobRejected}

trait HttpInterface {
  def system: ActorSystem
  def mat: Materializer

  def routes(ctrl: ActorRef)(implicit to: Timeout): Route =
    pathPrefix("api") {
      extractExecutionContext { implicit ec ⇒
        path("job") {
          post {
            entity(as[JobDescriptor]) { desc ⇒
              complete(
                ctrl ? api.SubmitJob(desc.job, desc.workers.getOrElse(Job.DefaultWorkerCount)) map {
                  case JobAccepted ⇒
                    StatusCodes.Accepted
                  case JobRejected ⇒
                    StatusCodes.NotAcceptable
                  case _ ⇒
                    StatusCodes.InternalServerError
                }
              )
            }
          }
        } ~
          path("health") {
            complete(StatusCodes.OK)
          }
      }
    }
}
