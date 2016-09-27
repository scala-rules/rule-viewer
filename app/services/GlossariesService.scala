package services

import javax.inject.{Inject, Singleton}

import org.scalarules.utils.Glossary
import play.api.Configuration

@Singleton
class GlossariesService @Inject()(configuration: Configuration, jarLoaderService: JarLoaderService) {

  val glossaries: Map[String, Glossary] = jarLoaderService.jars.flatMap( jarEntry => {
    jarEntry._2.glossaries.map( g => (g.getClass.getName, g) )
  })

  def findById(id: String): Option[Glossary] = glossaries.get(id)

}
