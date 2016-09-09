package controllers

import javax.inject.{Inject, Singleton}

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import services.{JarLoaderService, JarLoadingResults}

import scala.reflect.ClassTag

// scalastyle:off public.methods.have.type

@Singleton
class JarsController @Inject() (jarLoaderService: JarLoaderService) extends Controller {

  def listConfiguration = Action(
    Ok(Json.toJson(jarLoaderService.jarStatusses))
  )

  def list = Action(
    Ok(Json.toJson(jarLoaderService.jars))
  )

  // TODO : Move to something in models
  implicit def jarLoadingResultsWrites[T : ClassTag]: Writes[JarLoadingResults] = (
    (JsPath \ "location").write[String] and
      (JsPath \ "glossaries").lazyWrite(Writes.seq[String]) and
      (JsPath \ "derivations").lazyWrite(Writes.seq[String])
    )( res => (res.jarName, res.glossaries.map( _.getClass.getName ), res.derivations.map( _.getClass.getName )) )

  def configurationById(id: String) = Action(
    jarLoaderService.jarStatusses.get(id).map( Json.toJson(_) ).map( Ok(_) ).getOrElse( NotFound("Element not found") )
  )

  def byId(id: String) = Action(
    jarLoaderService.jars.get(id).map( Json.toJson(_) ).map( Ok(_) ).getOrElse( NotFound("Element not found") )
  )


}
