package com.github.agourlay.cornichon.steps.wrapped

import com.github.agourlay.cornichon.core._
import com.github.agourlay.cornichon.steps.StepUtilSpec
import com.github.agourlay.cornichon.steps.regular.assertStep.{ AssertStep, GenericEqualityAssertion }
import org.scalatest.{ AsyncWordSpec, Matchers }

class WithDataInputStepSpec extends AsyncWordSpec with Matchers with StepUtilSpec {

  "WithDataInputStep" must {

    "fail if table is malformed" in {
      val nested = AssertStep(
        "always ok",
        _ => GenericEqualityAssertion(true, true)
      ) :: Nil
      val inputs =
        """
          | a | b | c |
          | 1 | 3  3 |
          | 7 | 4 | 4 |
          | 0  0 | 0 |
        """

      val withDataInputStep = WithDataInputStep(nested, inputs)
      val s = Scenario("scenario with WithDataInput", withDataInputStep :: Nil)
      val res = ScenarioRunner.runScenario(Session.newEmpty)(s)
      res.map(_.isSuccess should be(false))
    }

    "fail at first failed input" in {
      val nested = AssertStep(
        "always fails",
        _ => GenericEqualityAssertion(true, false)
      ) :: Nil
      val inputs =
        """
          | a | b | c |
          | 1 | 3 | 3 |
          | 7 | 4 | 4 |
          | 0 | 0 | 0 |
        """

      val withDataInputStep = WithDataInputStep(nested, inputs)
      val s = Scenario("scenario with WithDataInput", withDataInputStep :: Nil)
      ScenarioRunner.runScenario(Session.newEmpty)(s).map(_.isSuccess should be(false))
    }

    "execute all steps if successful" in {
      var uglyCounter = 0
      val nested = AssertStep(
        "always ok",
        _ => {
          uglyCounter = uglyCounter + 1
          GenericEqualityAssertion(true, true)
        }
      ) :: Nil
      val inputs =
        """
        | a | b | c |
        | 1 | 3 | 3 |
        | 7 | 4 | 4 |
        | 0 | 0 | 0 |
      """

      val withDataInputStep = WithDataInputStep(nested, inputs)
      val s = Scenario("scenario with WithDataInput", withDataInputStep :: Nil)
      val res = ScenarioRunner.runScenario(Session.newEmpty)(s)
      res.map { res =>
        res.isSuccess should be(true)
        uglyCounter should be(3)
      }
    }

    "inject values in session" in {
      val nested = AssertStep(
        "sum of 'a' + 'b' = 'c'",
        sc => {
          val s = sc.session
          val sum = s.getUnsafe("a").toInt + s.getUnsafe("b").toInt
          GenericEqualityAssertion(sum, s.getUnsafe("c").toInt)
        }
      ) :: Nil
      val inputs =
        """
          | a | b  | c  |
          | 1 | 3  | 4  |
          | 7 | 4  | 11 |
          | 1 | -1 | 0  |
        """

      val withDataInputStep = WithDataInputStep(nested, inputs)
      val s = Scenario("scenario with WithDataInput", withDataInputStep :: Nil)
      val res = ScenarioRunner.runScenario(Session.newEmpty)(s)
      res.map(_.isSuccess should be(true))
    }

    "resolve placeholder" in {
      val nested = AssertStep(
        "building URL",
        sc => {
          val s = sc.session
          val url = s.getUnsafe("endpoint") + "/" + s.getUnsafe("resource")
          GenericEqualityAssertion(url, s.getUnsafe("url"))
        }
      ) :: Nil
      val inputs =
        """
          | endpoint | resource   | url             |
          | "api"    | "products" | "api/products"  |
          | "api"    | "<other>"  | "api/customers" |
        """

      val withDataInputStep = WithDataInputStep(nested, inputs)
      val s = Scenario("scenario with WithDataInput", withDataInputStep :: Nil)
      val res = ScenarioRunner.runScenario(Session.newEmpty.addValueUnsafe("other", "customers"))(s)
      res.map(_.isSuccess should be(true))
    }
  }

}
