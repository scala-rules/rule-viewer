package controllers

import scala.collection.JavaConverters._

import javax.inject._

import play.api.Configuration
import play.api.libs.json._
import play.api.mvc._


//scalastyle:off public.methods.have.type

@Singleton
class ConfigurationController @Inject()(configuration: Configuration) extends Controller {

  val config: Map[String, List[String]] = Map[String, List[String]](
    "endpoints" -> configuration.getStringList("endpoints").map( _.asScala.toList ).getOrElse( List[String]() )
  )

  def all = Action {
    Ok(Json.toJson(config))
  }

}
