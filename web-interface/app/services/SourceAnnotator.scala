package services

import java.io.Writer

import org.scalarules.dsl.nl.grammar.Berekening
import org.scalarules.engine.Derivation
import org.scalarules.utils.{FileSourcePosition, SourcePosition}

import scala.annotation.tailrec
import scala.io.Source

object SourceAnnotator {

  def annotate(input: Source, berekening: Berekening, output: Writer): Unit = {
    val sourcePositionAnnotations: List[SourceAnnotation] = extractSourcePositionAnnotations(berekening.berekeningen)

    val levelOneAnnotations: List[SourceAnnotation] = sourcePositionAnnotations
    val annotations = processDerivationAnnotations(levelOneAnnotations.sortBy( ann => ann.byteOffset ), List[SourceAnnotation]())

    output.write( preamble(berekening) )

    val annotatedStream = new StreamAnnotationCombiner(annotations).applyToStream( input.reset() )
        .flatMap(ch => ch match {
          case '\n' => "<br/>\n"
          case '(' => "<span class='lparen'>(</span>"
          case ')' => "<span class='rparen'>)</span>"
          case ',' => "<span class='comma'>,</span>"
          case x:Char => Array(x)
        })

    new IndentationFixer().applyToStream(annotatedStream)
      .foreach( ch => output.write(ch) )

    output.write( postamble )
  }

  /**
    * @param annotations must be sorted by byteOffset
    * @param acc accumulates the resulting annotations in reversed order
    * @return
    */
  @tailrec
  private def processDerivationAnnotations(annotations: List[SourceAnnotation], acc: List[SourceAnnotation]): List[SourceAnnotation] = annotations match {
    case BerekenOrEnWordEndAnnotation(boStart, fn) :: (sa:SourceAnnotation) :: sas =>
      processDerivationAnnotations(sas, sa :: DerivationEndAnnotation(sa.byteOffset) :: DerivationStartAnnotation(boStart, fn) :: annotations.head :: acc)
    case GegevenWordEndAnnotation(boStart) :: (sa:SourceAnnotation) :: sas =>
      processDerivationAnnotations(sas, sa :: ConditionEndAnnotation(sa.byteOffset) :: ConditionStartAnnotation(boStart) :: annotations.head :: acc)
    case x :: xs => processDerivationAnnotations(xs, x :: acc)
    case Nil => acc.reverse
  }

  class StreamAnnotationCombiner(annotations: List[SourceAnnotation]) {
    var byteCounter: Int = 0
    var remainingAnnotations: List[SourceAnnotation] = annotations.sortBy( a => a.byteOffset )

    def applyToStream(stream: Iterator[Char]): Iterator[Char] = {
      stream.flatMap( ch => (byteCounter, remainingAnnotations) match {
        case (sourcePos, a :: as) if sourcePos < a.byteOffset => {
          byteCounter += 1
          Array(ch)
        }
        case (sourcePos, a :: as) => {
          byteCounter += 1
          val applicableAnnotations: List[SourceAnnotation] = remainingAnnotations.takeWhile( a => sourcePos >= a.byteOffset )
          remainingAnnotations = remainingAnnotations.dropWhile( a => sourcePos >= a.byteOffset )

          val insertBefore: String = applicableAnnotations.flatMap( a => a.before ).fold("")( _ + _ )
          val insertAfter: String = applicableAnnotations.flatMap( a => a.after ).fold("")( _ + _ )

          val insertAround: (String, String) = applicableAnnotations.flatMap( a => a.around ).fold( ("", "") )( (tupleA, tupleB) => (tupleB._1 + tupleA._1, tupleA._2 + tupleB._2) )

          insertBefore + insertAround._1 + ch + insertAround._2 + insertAfter
        }
        case (sourcePos, Nil) => Array(ch)
      })
    }
  }

  class IndentationFixer {
    var whitespaceAfterNewline: Boolean = true

    def applyToStream(stream: Iterator[Char]): Iterator[Char] = {
      stream.flatMap( ch => (whitespaceAfterNewline, ch) match {
        case (true, ' ') => "&nbsp;"
        case (_, '\n') => {
          whitespaceAfterNewline = true
          Array('\n')
        }
        case (_, c) => {
          whitespaceAfterNewline = false
          Array(c)
        }
      })
    }
  }

  private def extractSourcePositionAnnotations(derivations: List[Derivation]): List[SourceAnnotation] = {
    (extractDerivationSourceAnnotations(derivations) ++ extractConditionSourceAnnotations(derivations))
  }

  private def extractConditionSourceAnnotations(derivations: List[Derivation]): List[SourceAnnotation] =
    derivations.map(derivation => derivation.conditionSourcePosition)
      .toSet
      .flatMap( (sp: SourcePosition) => sp match {
        case csp: FileSourcePosition => List(
          GegevenWordStartAnnotation(csp.offset),
          GegevenWordEndAnnotation(csp.offset + csp.length)
        )
        case _ => List()
      })
      .toList

  private def extractDerivationSourceAnnotations(derivations: List[Derivation]): List[SourceAnnotation] =
    derivations.collect ({
      case d: Derivation if d.sourcePosition.isInstanceOf[FileSourcePosition] => {
        val fsp: FileSourcePosition = d.sourcePosition.asInstanceOf[FileSourcePosition]

        List(
          BerekenOrEnWordStartAnnotation(fsp.offset, d.output.name),
          BerekenOrEnWordEndAnnotation(fsp.offset + fsp.length, d.output.name)
        )
      }
    }).flatten

  private def preamble(berekening: Berekening): String =
    s"""<!DOCTYPE html>
        |<html lang="en">
        |  <body>
        |    <div class="berekening" id="berekening-${berekening.getClass.getSimpleName}"><br>
    """.stripMargin

  private def postamble: String =
    """    </div>
      |  </body>
      |</html>
    """.stripMargin

}

sealed abstract class SourceAnnotation(val before: Option[String] = None, val around: Option[(String, String)] = None, val after: Option[String] = None) {
  val byteOffset: Int

  def buildReplacement(ch: Char): String = before.getOrElse("") + around.getOrElse(("", ""))._1 + ch + around.getOrElse(("", ""))._2 + after.getOrElse("")
}

case class GegevenWordStartAnnotation(val byteOffset: Int) extends SourceAnnotation(
  before = Some("<span class='gegeven'>")
)
case class GegevenWordEndAnnotation(val byteOffset: Int) extends SourceAnnotation(
  before = Some("</span>")
)

case class BerekenOrEnWordStartAnnotation(val byteOffset: Int, outputFactName: String) extends SourceAnnotation(
  before = Some("<span class='bereken-or-en'>")
)
case class BerekenOrEnWordEndAnnotation(val byteOffset: Int, outputFactName: String) extends SourceAnnotation(
  before = Some("</span>")
)

case class CommaAnnotation(val byteOffset: Int) extends SourceAnnotation(
  around = Some( ("<span class='comma'>", "</span>"))
)
case class LParenAnnotation(val byteOffset: Int) extends SourceAnnotation(
  around = Some( ("<span class='lparen'>", "</span>"))
)
case class RParenAnnotation(val byteOffset: Int) extends SourceAnnotation(
  around = Some( ("<span class='rparen'>", "</span>"))
)

case class DerivationStartAnnotation(val byteOffset: Int, outputFactName: String) extends SourceAnnotation(
  before = Some(s"""<span class='derivation' ng-class="{'highlight': hoveredNode == '${outputFactName}'}" id="derivation-${outputFactName}">""")
)
case class DerivationEndAnnotation(val byteOffset: Int) extends SourceAnnotation(
  before = Some(s"""</span>""")
)

case class ConditionStartAnnotation(val byteOffset: Int) extends SourceAnnotation(
  before = Some(s"""<span class='condition' id="condition-">""")
)
case class ConditionEndAnnotation(val byteOffset: Int) extends SourceAnnotation(
  before = Some(s"""</span>""")
)
