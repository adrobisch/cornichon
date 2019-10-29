package com.github.agourlay.cornichon.steps.wrapped

import java.util.concurrent.atomic.AtomicInteger

import com.github.agourlay.cornichon.core.{ Scenario, ScenarioRunner, ScenarioTitleLogInstruction, Session }
import com.github.agourlay.cornichon.steps.StepUtilSpec
import com.github.agourlay.cornichon.steps.cats.EffectStep
import com.github.agourlay.cornichon.steps.regular.assertStep.{ AssertStep, Assertion }
import org.scalatest.{ AsyncWordSpec, Matchers, OptionValues }

class AttachStepSpec extends AsyncWordSpec with Matchers with OptionValues with StepUtilSpec {

  "AttachStep" must {
    "merge nested steps in the parent flow when first" in {
      val nested = List.fill(5)(AssertStep("always true", _ => Assertion.alwaysValid))
      val steps = AttachStep(_ => nested) :: Nil
      val s = Scenario("scenario with Attach", steps)
      ScenarioRunner.runScenario(Session.newEmpty)(s).map { r =>
        r.isSuccess should be(true)
        r.logs.headOption.value should be(ScenarioTitleLogInstruction("Scenario : scenario with Attach", 1))
        r.logs.size should be(7)
      }
    }

    "merge nested steps in the parent flow when nested" in {
      val nested = List.fill(5)(AssertStep("always true", _ => Assertion.alwaysValid))
      val steps = AttachStep(_ => nested) :: Nil
      val s = Scenario("scenario with Attach", RepeatStep(steps, 1, None) :: Nil)
      ScenarioRunner.runScenario(Session.newEmpty)(s).map { r =>
        r.isSuccess should be(true)
        r.logs.headOption.value should be(ScenarioTitleLogInstruction("Scenario : scenario with Attach", 1))
        r.logs.size should be(9)
      }
    }

    "run all nested valid effects" in {
      val uglyCounter = new AtomicInteger(0)
      val effectNumber = 5
      val effect = EffectStep.fromSync(
        "increment captured counter",
        sc => {
          uglyCounter.incrementAndGet()
          sc.session
        }
      )

      val nestedSteps = List.fill(effectNumber)(effect)
      val attached = AttachStep(_ => nestedSteps)

      val s = Scenario("scenario with effects", attached :: effect :: Nil)
      ScenarioRunner.runScenario(Session.newEmpty)(s).map { res =>
        res.isSuccess should be(true)
        uglyCounter.get() should be(effectNumber + 1)
      }
    }
  }

}
