package com.pubgame.play

import com.google.inject.AbstractModule

class ApplicationModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ItdInfoSystem]).asEagerSingleton()
  }
}
