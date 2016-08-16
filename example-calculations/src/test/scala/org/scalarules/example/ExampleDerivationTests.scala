package org.scalarules.example

import org.scalarules.testutils.nl.BerekeningenTester
import org.scalarules.example.ExampleGlossary._
import org.scalarules.finance.nl._

class ExampleDerivationTests extends BerekeningenTester(new ExampleDerivation) {

  val constantValues = waardes(
    FlatTaxRate is 45.procent,
    HealthCostReimbursementPercentage is 75.procent,
    HealthCostReimbursementCeiling is 2500.euro,
    TotalPrepaidTaxes is 21000.euro
  )

  test("Base income without health costs leads to tax due") gegeven (
    constantValues,
    BaseIncome is 50000.euro
  ) verwacht (
    TaxDueAmount is 1500.euro
  )

  test("Base income without health costs, but too high prepaid taxes leads to tax return") gegeven (
    constantValues,
    BaseIncome is 50000.euro,
    TotalPrepaidTaxes is 24000.euro
  ) debug (
    TaxReturnAmount is 1500.euro
  )

  test("Base income with minor health costs leads to reduced tax due") gegeven (
    constantValues,
    BaseIncome is 50000.euro,
    TotalPaidHealthCost is 400.euro
  ) verwacht (
    TaxDueAmount is 1200.euro
  )

  test("Base income with major health costs leads to tax return") gegeven (
    constantValues,
    BaseIncome is 50000.euro,
    TotalPaidHealthCost is 2800.euro
  ) verwacht (
    TaxReturnAmount is 600.euro
  )

  test("Base income with enormous health costs leads to capped tax return") gegeven (
    constantValues,
    BaseIncome is 50000.euro,
    TotalPaidHealthCost is 280000.euro
  ) verwacht (
    TaxReturnAmount is 1000.euro
  )

}
