package com.github.agourlay.cornichon.steps.wrapped

import cats.data.Xor._
import com.github.agourlay.cornichon.core.Done._
import com.github.agourlay.cornichon.core._
import com.github.agourlay.cornichon.dsl.BlockScopedResource

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._

case class WithBlockScopedResource(nested: Vector[Step], resource: BlockScopedResource) extends WrapperStep {

  val title = resource.openingTitle

  override def run(engine: Engine)(initialRunState: RunState)(implicit ec: ExecutionContext) = {

    // FIXME chain futures once https://github.com/agourlay/cornichon/issues/80
    val resourceHandle = Await.result(resource.startResource(), 10.seconds)

    val resourcedRunState = initialRunState.withSteps(nested).resetLogs.goDeeper.mergeSessions(resourceHandle.initialisedSession)
    val (resourcedState, resourcedRes) = engine.runSteps(resourcedRunState)

    val nestedLogs = resourcedState.logs
    val initialDepth = initialRunState.depth

    val (fullLogs, xor) = resourcedRes.fold(
      failedStep ⇒ {
        val failureLogs = failedTitleLog(initialDepth) +: nestedLogs :+ FailureLogInstruction(resource.closingTitle, initialDepth)
        (failureLogs, left(failedStep))
      },
      done ⇒ {
        val successLogs = successTitleLog(initialDepth) +: nestedLogs :+ SuccessLogInstruction(resource.closingTitle, initialDepth, None)
        (successLogs, rightDone)
      }
    )

    val resourceResultsF = for {
      results ← resourceHandle.resourceResults()
      _ ← resourceHandle.stopResource()
    } yield results

    // FIXME chain futures once https://github.com/agourlay/cornichon/issues/80
    val resourceResults = Await.result(resourceResultsF, 10.seconds)

    val completeSession = resourcedState.session.merge(resourceResults)
    (initialRunState.withSession(completeSession).appendLogs(fullLogs), xor)
  }
}