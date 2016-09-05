package org.scalarules.example

import org.scalarules.dsl.nl.grammar._
import org.scalarules.finance.nl._

import org.scalarules.example.ExampleGlossary._
import org.scalarules.dsl.ext.ListDerivationHelper._

class ExampleDerivation extends Berekening (

  Gegeven (altijd)
  Bereken
    DefaultPaidHealthCost is 0.euro en
    DefaultMinimumOwedTaxes is 0.euro
  ,
  Gegeven (altijd)
  Bereken
    BaseIncomeTax is BaseIncome * FlatTaxRate en
    BaseHealthCosts is eerste(TotalPaidHealthCost, DefaultPaidHealthCost) en
    HealthCostEligibleForReimbursement is BaseHealthCosts * HealthCostReimbursementPercentage en
    ActualHealthCostReimbursement is laagste.van(List(HealthCostEligibleForReimbursement, HealthCostReimbursementCeiling)) en
    TaxesReducedByReimbursements is BaseIncomeTax - ActualHealthCostReimbursement en
    LegallyOwedTaxes is (hoogste van(List(TaxesReducedByReimbursements, DefaultMinimumOwedTaxes)))
  ,
  Gegeven (LegallyOwedTaxes < TotalPrepaidTaxes)
  Bereken
    TaxReturnAmount is TotalPrepaidTaxes - LegallyOwedTaxes
  ,
  Gegeven (LegallyOwedTaxes >= TotalPrepaidTaxes)
  Bereken
    TaxDueAmount is LegallyOwedTaxes - TotalPrepaidTaxes


)
