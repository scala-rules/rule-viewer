package services

import java.net.{URL, URLClassLoader}
import java.util.jar.{JarEntry, JarFile}
import javax.inject.{Inject, Singleton}

import org.scalarules.dsl.nl.grammar.Berekening
import org.scalarules.utils.Glossary
import play.api.Configuration

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success, Try}

@Singleton
class JarLoaderService @Inject() (configuration: Configuration) {

  private val jarsToLoad: List[String] = configuration.getStringList("jars.load").map( _.asScala.toList ).getOrElse( List[String]() )
  private val triedJars: Map[String, Try[JarLoadingResults]] = jarsToLoad.map( jarLocation => (jarLocation, loadJar(jarLocation)) ).toMap

  val jars: Map[String, JarLoadingResults] = triedJars.collect {
    case (jarName: String, Success(loadedJar)) => (jarName, loadedJar)
  }

  val jarStatusses: Map[String, String] = triedJars.map {
    case (jarName: String, Success(_)) => (jarName, "Loaded")
    case (jarName: String, Failure(exception)) => (jarName, s"Failed to load: ${exception.getMessage.takeWhile( ch => (ch != '{') )})}")
  }

  private def loadJar(location: String): Try[JarLoadingResults] = Try({
    val jarFile: JarFile = new JarFile(location)
    val jarClassEntries: List[JarEntry] = jarFile.entries().asScala
      .filter( je => !je.isDirectory && je.getName.endsWith(".class") ).toList

    val urls: Array[URL] = Array( new URL("jar:file:" + location + "!/") )
    val cl: URLClassLoader = URLClassLoader.newInstance(urls, this.getClass.getClassLoader)

    val mirror = runtimeMirror(cl)

    val triedGlossaries: List[Try[Glossary]] = scanAndLoadClasses(jarClassEntries, new GlossaryClassLoader, cl, mirror)
    val triedDerivations: List[Try[Berekening]] = scanAndLoadClasses(jarClassEntries, new DerivationClassLoader, cl, mirror)

    JarLoadingResults(
      jarName = location,
      glossaries = triedGlossaries.collect { case Success(glossary) => glossary }.toList,
      derivations = triedDerivations.collect { case Success(derivation) => derivation }.toList
    )
  })

  private def scanAndLoadClasses[T](entries: List[JarEntry], loader: SpecializedClassLoader[T], classLoader: URLClassLoader, mirror: Mirror): List[Try[T]] = {
    entries.map( entry => entry.getName.dropRight(JarLoaderService.CLASS_FILE_SUFFIX.length).replaceAll("/", ".") )
      .filter( className => loader.precondition(className) )
      .map( className => loader.preprocessClassName(className, classLoader) )
      .map( finalClassName => loader.load(finalClassName, mirror) )
      .toList
  }

}

object JarLoaderService {
  val CLASS_FILE_SUFFIX = ".class"
}

case class JarLoadingResults(jarName: String, glossaries: List[Glossary], derivations: List[Berekening])

trait SpecializedClassLoader[T] {
  def precondition(className: String): Boolean
  def preprocessClassName(className: String, classLoader: URLClassLoader): String
  def load(className: String, mirror: Mirror): Try[T]
}

class GlossaryClassLoader extends SpecializedClassLoader[Glossary] {
  override def precondition(className: String): Boolean = className.endsWith("$")

  override def preprocessClassName(className: String, classLoader: URLClassLoader): String = {
    val finalClassName = className.dropRight(1)
    classLoader.loadClass(finalClassName)
    finalClassName
  }

  override def load(className: String, mirror: _root_.scala.reflect.runtime.universe.Mirror): Try[Glossary] = Try({
    val glossaryModule: ModuleSymbol = mirror.staticModule(className).asModule

    val modMirror: ModuleMirror = mirror.reflectModule(glossaryModule)

    modMirror.instance.asInstanceOf[Glossary]
  })
}

class DerivationClassLoader extends SpecializedClassLoader[Berekening] {
  override def precondition(className: String): Boolean = !className.endsWith("$")

  override def preprocessClassName(className: String, classLoader: URLClassLoader): String = {
    classLoader.loadClass(className)
    className
  }

  override def load(className: String, mirror: _root_.scala.reflect.runtime.universe.Mirror): Try[Berekening] = Try({
    val derivationClass: ClassSymbol = mirror.staticClass(className)

    val classMirror: ClassMirror = mirror.reflectClass(derivationClass)

    val constructor: MethodSymbol = derivationClass.primaryConstructor.asMethod

    val constructorMirror: MethodMirror = classMirror.reflectConstructor(constructor)

    constructorMirror().asInstanceOf[Berekening]
  })
}

