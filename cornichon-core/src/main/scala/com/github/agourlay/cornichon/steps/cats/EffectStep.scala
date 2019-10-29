package com.github.agourlay.cornichon.steps.cats

import cats.data.{ EitherT, NonEmptyList }
import cats.effect.Effect
import cats.syntax.either._
import com.github.agourlay.cornichon.core.ScenarioRunner.{ errorsToFailureStep, successLog }
import com.github.agourlay.cornichon.core._
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.duration.Duration

case class EffectStep[F[_]: Effect](title: String, effect: ScenarioContext => F[Either[CornichonError, Session]], show: Boolean = true) extends SessionValueStep {

  def setTitle(newTitle: String): Step = copy(title = newTitle)

  override def runSessionValueStep(runState: RunState): Task[Either[NonEmptyList[CornichonError], Session]] =
    Task.fromEffect(effect(runState.scenarioContext)).map(_.leftMap(NonEmptyList.one))

  override def onError(errors: NonEmptyList[CornichonError], runState: RunState, executionTime: Duration): (LogInstruction, FailedStep) =
    errorsToFailureStep(this, runState.depth, errors, Some(executionTime))

  override def logOnSuccess(result: Session, runState: RunState, executionTime: Duration): LogInstruction =
    successLog(title, runState.depth, show, executionTime)

}

object EffectStep {

  def fromEitherT[F[_]: Effect](title: String, effect: ScenarioContext => EitherT[F, CornichonError, Session], show: Boolean = true): Step = {
    val effectT: ScenarioContext => F[Either[CornichonError, Session]] = s => effect(s).value
    EffectStep(title, effectT, show)
  }

  def fromSync(title: String, effect: ScenarioContext => Session, show: Boolean = true): Step = {
    import Scheduler.Implicits.global
    val effectF: ScenarioContext => Task[Either[CornichonError, Session]] = s => Task.now(effect(s).asRight)
    EffectStep(title, effectF, show)
  }

  def fromSyncE(title: String, effect: ScenarioContext => Either[CornichonError, Session], show: Boolean = true): Step = {
    import Scheduler.Implicits.global
    val effectF: ScenarioContext => Task[Either[CornichonError, Session]] = s => Task.now(effect(s))
    EffectStep(title, effectF, show)
  }

  def fromAsync[F[_]: Effect](title: String, effect: ScenarioContext => F[Session], show: Boolean = true): Step = {
    import Scheduler.Implicits.global
    val effectF: ScenarioContext => Task[Either[CornichonError, Session]] = s => Task.fromEffect(effect(s)).map(Right.apply)
    EffectStep(title, effectF, show)
  }

}
