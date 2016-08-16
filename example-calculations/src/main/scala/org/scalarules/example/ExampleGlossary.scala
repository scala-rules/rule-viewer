package org.scalarules.example

import org.scalarules.finance.nl._
import org.scalarules.utils.Glossary

object ExampleGlossary extends Glossary {

  val BaseIncome = defineFact[Bedrag]
  val TotalPrepaidTaxes = defineFact[Bedrag]
  val TotalPaidHealthCost = defineFact[Bedrag]

  val DefaultPaidHealthCost = defineFact[Bedrag]
  val DefaultMinimumOwedTaxes = defineFact[Bedrag]


  val FlatTaxRate = defineFact[Percentage]
  val HealthCostReimbursementPercentage = defineFact[Percentage]
  val HealthCostReimbursementCeiling = defineFact[Bedrag]


  val BaseIncomeTax = defineFact[Bedrag]
  val BaseHealthCosts = defineFact[Bedrag]
  val HealthCostEligibleForReimbursement = defineFact[Bedrag]
  val TaxesReducedByReimbursements = defineFact[Bedrag]
  val LegallyOwedTaxes = defineFact[Bedrag]

  val ActualHealthCostReimbursement = defineFact[Bedrag]


  val TaxReturnAmount = defineFact[Bedrag]
  val TaxDueAmount = defineFact[Bedrag]

}
