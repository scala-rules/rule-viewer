package controllers

import javax.inject._

import models.graph.GraphModelJsonSerializers._
import play.api.libs.json._
import play.api.mvc._
import services.DerivationsService

//scalastyle:off public.methods.have.type


@Singleton
class DerivationsController @Inject() (derivationsService: DerivationsService) extends Controller {

  def listConfiguration = Action(
    Ok(Json.toJson(derivationsService.derivationStatusses))
  )

  def list = Action {
    Ok(Json.toJson(derivationsService.derivationGraphs))
  }

  def configurationById(id: String) = Action(
    derivationsService.findStatusById(id).map( Json.toJson(_) ).map( Ok(_) ).getOrElse( NotFound("Element not found") )
  )

  def byId(id: String) = Action(
    derivationsService.findGraphById(id).map( Json.toJson(_) ).map( Ok(_) ).getOrElse( NotFound("Element not found") )
  )

}
