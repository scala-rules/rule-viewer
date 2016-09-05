package controllers

import javax.inject._

import org.scalarules.engine.Fact
import org.scalarules.utils.Glossary
import play.api._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import scala.language.postfixOps

//scalastyle:off public.methods.have.type

@Singleton
class GlossariesController @Inject() (configuration: Configuration) extends Controller {

  private val glossariesToLoad: List[String] = configuration.getStringList("glossaries.load").map( _.asScala.toList ).getOrElse( List[String]() )

  private val triedGlossaries: Map[String, Try[Glossary]] = glossariesToLoad.map( gClass => (gClass, loadGlossary(gClass)) ).toMap
  private val glossaryStatusses = triedGlossaries.map {
    case (glossaryName: String, Success(_)) => (glossaryName, "Loaded")
    case (glossaryName: String, Failure(exception)) => (glossaryName, s"Failed to load: ${exception.getMessage}")
  }
  val glossaries: Map[String, Map[String, Fact[Any]]] = triedGlossaries.collect({ case (moduleName: String, Success(glossary: Glossary)) => (moduleName, glossary.facts) })

  def listConfiguration = Action(
    Ok(Json.toJson(glossaryStatusses))
  )

  def list = Action(
    Ok(Json.toJson(glossaries))
  )

  // TODO : Move to something belonging to Fact, but which is within the implicit scope
  implicit def factWrites[T : ClassTag]: Writes[Fact[T]] = (
    (JsPath \ "name").write[String] and
    (JsPath \ "description").write[String] and
    (JsPath \ "valueClass").write[String]
  )( f => (f.name, f.description, f.valueType) )

  def configurationById(id: String) = Action(
    glossaryStatusses.get(id).map( Json.toJson(_) ).map( Ok(_) ).getOrElse( NotFound("Element not found") )
  )

  def byId(id: String) = Action(
    glossaries.get(id).map( Json.toJson(_) ).map( Ok(_) ).getOrElse( NotFound("Element not found") )
  )

  private def loadGlossary(gClass: String): Try[Glossary] = Try({
    import scala.reflect.runtime.universe._

    val mirror = runtimeMirror(this.getClass.getClassLoader)
    val glossaryModule: ModuleSymbol = mirror.staticModule(gClass).asModule

    val modMirror: ModuleMirror = mirror.reflectModule(glossaryModule)

    modMirror.instance.asInstanceOf[Glossary]
  })

}
