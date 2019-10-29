package com.github.agourlay.cornichon.steps.wrapped

import com.github.agourlay.cornichon.core._
import com.github.agourlay.cornichon.steps.StepUtilSpec
import com.github.agourlay.cornichon.steps.regular.assertStep.{ AssertStep, GenericEqualityAssertion }
import org.scalatest.{ Matchers, AsyncWordSpec }

import scala.concurrent.duration._

class RepeatDuringStepSpec extends AsyncWordSpec with Matchers with StepUtilSpec {

  "RepeatDuringStep" must {
    "fail if 'repeatDuring' block contains a failed step" in {
      val nested = AssertStep(
        "always fails",
        _ => GenericEqualityAssertion(true, false)
      ) :: Nil
      val repeatDuring = RepeatDuringStep(nested, 5.millis)
      val s = Scenario("scenario with RepeatDuring", repeatDuring :: Nil)
      ScenarioRunner.runScenario(Session.newEmpty)(s).map(_.isSuccess should be(false))
    }

    "repeat steps inside 'repeatDuring' for at least the duration param" in {
      val nested = AssertStep(
        "always valid",
        _ => {
          Thread.sleep(1)
          GenericEqualityAssertion(true, true)
        }
      ) :: Nil
      val repeatDuringStep = RepeatDuringStep(nested, 50.millis)
      val s = Scenario("scenario with RepeatDuring", repeatDuringStep :: Nil)
      ScenarioRunner.runScenario(Session.newEmpty)(s).timed.map {
        case (executionTime, res) =>
          withClue(executionTime.toMillis + "\n" + LogInstruction.renderLogs(res.logs)) {
            res.isSuccess should be(true)
            executionTime.gt(50.millis) should be(true)
            // empiric values for the upper bound here
            executionTime.lteq(65.millis) should be(true)
          }
      }
    }

    "repeat steps inside 'repeatDuring' at least once if they take more time than the duration param" in {
      val nested = AssertStep(
        "always valid",
        _ => {
          Thread.sleep(500)
          GenericEqualityAssertion(true, true)
        }
      ) :: Nil
      val repeatDuringStep = RepeatDuringStep(nested, 50.millis)
      val s = Scenario("scenario with RepeatDuring", repeatDuringStep :: Nil)
      ScenarioRunner.runScenario(Session.newEmpty)(s).timed.map {
        case (executionTime, res) =>
          withClue(executionTime.toMillis + "\n" + LogInstruction.renderLogs(res.logs)) {
            res.isSuccess should be(true)
            executionTime.gt(50.millis) should be(true)
            // empiric values for the upper bound here
            executionTime.lt(550.millis) should be(true)
          }
      }
    }
  }
}
