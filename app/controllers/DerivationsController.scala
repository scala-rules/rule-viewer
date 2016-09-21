package controllers

import javax.inject._

import models.graph.GraphModelJsonSerializers._
import play.api.libs.json._
import play.api.mvc._
import services.DerivationsService

//scalastyle:off public.methods.have.type

@Singleton
class DerivationsController @Inject() (derivationsService: DerivationsService) extends Controller {

  def list = Action {
    Ok(Json.toJson(derivationsService.derivationGraphs))
  }

  def byId(id: String) = Action(
    derivationsService.findGraphById(id).map( Json.toJson(_) ).map( Ok(_) ).getOrElse( NotFound("Element not found") )
  )

}
