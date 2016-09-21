package controllers

import java.io.{ByteArrayOutputStream, OutputStreamWriter, Writer}
import javax.inject._

import akka.util.ByteString
import org.scalarules.dsl.nl.grammar.Berekening
import play.api.http.HttpEntity
import play.api.mvc._
import play.api.{Configuration, Environment}
import services.{DerivationsService, SourceAnnotator}

import scala.io.Source

//scalastyle:off public.methods.have.type

@Singleton
class SourcesController @Inject() (configuration: Configuration, environment: Environment, derivationsService: DerivationsService) extends Controller {

  private val sourceRoot = configuration.getString("sources.root").getOrElse("/public/sources/")

  def sourcesFor(derivationName: String) = Action({ _ =>
    val sourceToLoad = sourceRoot + fqnToRelativePath(derivationName) + ".scala"
    val inputFile = environment.getExistingFile(sourceToLoad)
    val inputResource = environment.resource(sourceToLoad)
    val derivation = derivationsService.findById(derivationName)

    if (inputFile.isDefined && derivation.isDefined) {
      Ok.sendEntity(
        // Note: Try to detect, or at least try other values for encoding if it fails to read
        HttpEntity.Strict(processRawSource(Source.fromFile(inputFile.get, "UTF-8"), derivation.get), Some("text/html"))
      )
    }
    else if (inputResource.isDefined && derivation.isDefined) {
      Ok.sendEntity(
        HttpEntity.Strict(processRawSource(Source.fromInputStream(inputResource.get.openStream()), derivation.get), Some("text/html"))
      )
    }
    else {
      Ok.sendResource("public/sources/no-source-available.html")
    }
  })

  private def fqnToRelativePath(fqn: String): String = fqn.replaceAll("\\.", "/")

  private def processRawSource(input: Source, derivation: Berekening): ByteString = {
    val byteBuffer: ByteArrayOutputStream = new ByteArrayOutputStream()
    val writer: Writer = new OutputStreamWriter(byteBuffer)

    SourceAnnotator.annotate(input, derivation, writer)

    writer.flush()
    val contentBuffer = byteBuffer.toByteArray
    ByteString(contentBuffer)
  }

}
