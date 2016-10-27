package com.pubgame.alphakigo.util.ansi

/**
 * @version 0.1
 * @since 0.1
 */
private[ansi] trait AnsiStyle {
  /** Reset ANSI styles */
  final val RESET = "0;"
  /** ANSI bold */
  final val BOLD = "1;"
  /** ANSI underlines */
  final val UNDERLINED = "4;"
  /** ANSI blink */
  final val BLINK = "5;"
  /** ANSI reversed */
  final val REVERSED = "7;"
  /** ANSI invisible */
  final val INVISIBLE = "8;"
}
