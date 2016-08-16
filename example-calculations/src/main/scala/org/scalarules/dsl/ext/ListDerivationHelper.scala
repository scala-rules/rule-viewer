package org.scalarules.dsl.ext

import org.scalarules.dsl.nl.grammar.{DslCondition, DslEvaluation}
import org.scalarules.engine.{Context, Evaluation}

import scala.language.implicitConversions

object ListDerivationHelper {

  private[this] def constructDslEvaluation[A](dslEvals: List[DslEvaluation[A]]): DslEvaluation[List[A]] = {
    val condition: DslCondition = dslEvals.map(e => e.condition).foldLeft(DslCondition.emptyTrueCondition)((v, cond) => DslCondition.orCombineConditions(v, cond))
    val evaluation: Evaluation[List[A]] = new Evaluation[List[A]] {
      override def apply(c: Context): Option[List[A]] = {
        val values = dslEvals.map(e => e.evaluation.apply(c)).filter(option => option.isDefined)

        if (values.isEmpty) None else Some(values.map(option => option.get))
      }
    }

    new DslEvaluation[List[A]](condition, evaluation)
  }


  implicit def listOfEvalToEvalOfList[A](dslEvals: List[DslEvaluation[A]]): DslEvaluation[List[A]] = constructDslEvaluation(dslEvals)
  implicit def listOfConvertableToEvalToEvalOfList[A, B](dslEvals: List[B])(implicit ev: B => DslEvaluation[A]): DslEvaluation[List[A]] =
    constructDslEvaluation( dslEvals.map( e => ev(e) ) )

}
