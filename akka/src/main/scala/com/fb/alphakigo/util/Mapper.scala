package com.pubgame.alphakigo.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object Mapper extends ObjectMapper {
  registerModule(DefaultScalaModule)
}
