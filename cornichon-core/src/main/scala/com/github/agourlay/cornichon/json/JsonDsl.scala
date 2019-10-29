package com.github.agourlay.cornichon.json

import cats.syntax.show._
import com.github.agourlay.cornichon.core.Step
import com.github.agourlay.cornichon.dsl.{ BaseFeature, CoreDsl }
import com.github.agourlay.cornichon.json.JsonSteps._
import com.github.agourlay.cornichon.json.CornichonJson._

trait JsonDsl {
  this: BaseFeature with CoreDsl =>

  def show_key_as_json(key: String, index: Option[Int] = None): Step =
    show_session(key, index, v => parseString(v).map(_.show))

  def session_json_values(k1: String, k2: String) = JsonValuesStepBuilder(k1, k2)

}
