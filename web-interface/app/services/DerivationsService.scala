package services

import javax.inject.{Inject, Singleton}

import models.graph.Graph
import org.scalarules.dsl.nl.grammar.Berekening
import play.api.Configuration

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

@Singleton
class DerivationsService @Inject() (configuration: Configuration) {

  private val derivationsToLoad: List[String] = configuration.getStringList("derivations.load").map( _.asScala.toList ).getOrElse( List[String]() )
  private val triedDerivations: Map[String, Try[Berekening]] = derivationsToLoad.map( dClass => (dClass, loadDerivation(dClass)) ).toMap

  val derivations: Map[String, Berekening] = triedDerivations.collect {
    case (derivationName: String, Success(loadedDerivation)) => (derivationName, loadedDerivation)
  }

  val derivationStatusses: Map[String, String] = triedDerivations.map {
    case (derivationName: String, Success(_)) => (derivationName, "Loaded")
    case (derivationName: String, Failure(exception)) => (derivationName, s"Failed to load: ${exception.getMessage.takeWhile( ch => (ch != '{') )})}")
  }

  val derivationGraphs: Map[String, Graph] = triedDerivations.collect {
    case (derivationName: String, Success(loadedDerivation)) => (derivationName, DerivationsToGraphModel.convert(loadedDerivation))
  }

  def findById(id: String): Option[Berekening] = derivations.get(id)
  def findGraphById(id: String): Option[Graph] = derivationGraphs.get(id)
  def findStatusById(id: String): Option[String] = derivationStatusses.get(id)

  private def loadDerivation(dClass: String): Try[Berekening] = Try({
    import scala.reflect.runtime.universe._ // scalastyle:ignore

    val mirror = runtimeMirror(this.getClass.getClassLoader)
    val derivationClass: ClassSymbol = mirror.staticClass(dClass)

    val classMirror: ClassMirror = mirror.reflectClass(derivationClass)

    val constructor: MethodSymbol = derivationClass.primaryConstructor.asMethod

    val constructorMirror: MethodMirror = classMirror.reflectConstructor(constructor)

    constructorMirror().asInstanceOf[Berekening]
  })

}
