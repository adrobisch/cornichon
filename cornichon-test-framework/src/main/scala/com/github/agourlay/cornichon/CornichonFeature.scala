package com.github.agourlay.cornichon

import com.github.agourlay.cornichon.dsl.{ BaseFeature, CheckDsl, CoreDsl }
import com.github.agourlay.cornichon.http.HttpDsl
import com.github.agourlay.cornichon.json.JsonDsl

trait CornichonBaseFeature extends BaseFeature with CoreDsl
trait CornichonFeature extends CornichonBaseFeature with HttpDsl with JsonDsl with CheckDsl