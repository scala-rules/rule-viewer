package controllers

import java.io.{ByteArrayOutputStream, File, OutputStreamWriter, Writer}
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
    val derivation = derivationsService.findById(derivationName)

    if (inputFile.isEmpty || derivation.isEmpty) {
      Ok.sendResource("public/sources/no-source-available.html")
    }
    else {
      Result(
        header = ResponseHeader(OK),
        body = HttpEntity.Strict(processRawSourceFile(inputFile.get, derivation.get), Some("text/html"))
      )
    }
  })

  private def fqnToRelativePath(fqn: String): String = fqn.replaceAll("\\.", "/")

  private def processRawSourceFile(inputFile: File, derivation: Berekening): ByteString = {
    val byteBuffer: ByteArrayOutputStream = new ByteArrayOutputStream()
    val writer: Writer = new OutputStreamWriter(byteBuffer)

    SourceAnnotator.annotate(Source.fromFile(inputFile), derivation, writer)

    writer.flush()
    val contentBuffer = byteBuffer.toByteArray
    ByteString(contentBuffer)
  }

}
