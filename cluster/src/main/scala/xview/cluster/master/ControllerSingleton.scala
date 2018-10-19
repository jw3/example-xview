package xview.cluster.master

import akka.actor.{ActorSystem, PoisonPill}
import akka.cluster.singleton._
import xview.cluster.Roles

object ControllerSingleton {
  private val singletonName = "controller"
  private val singletonRole = Roles.Master

  def startSingleton(system: ActorSystem) =
    system.actorOf(
      ClusterSingletonManager.props(
        Controller.props(),
        PoisonPill,
        ClusterSingletonManagerSettings(system).withRole(singletonRole)
      ),
      singletonName
    )

  def proxyProps(system: ActorSystem) =
    ClusterSingletonProxy.props(settings = ClusterSingletonProxySettings(system).withRole(singletonRole),
                                singletonManagerPath = s"/user/$singletonName")
}
