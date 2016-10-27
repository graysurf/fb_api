package com.pubgame.alphakigo.util.logging

import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.spi.ScanException
import ch.qos.logback.core.subst.Token

import scala.annotation.{ switch, tailrec }
import scala.collection.JavaConverters._

@deprecated
class RecursiveTokenizer(private[this] final val pattern: String) {
  object TokenizerState extends Enumeration {
    type TokenizerState = Value
    val LITERAL_STATE, START_STATE, DEFAULT_VAL_STATE = Value
  }
  import TokenizerState._
  private[this] final lazy val patternLength: Int = pattern.length

  @throws(classOf[ScanException])
  def recursiveTokenize = {
    val stringBuilder = StringBuilder.newBuilder

    @tailrec
    def recursiveTokenize0(pointer: Int, state: TokenizerState, tokenList: List[Token]): List[Token] = {
      val nextPointer = pointer + 1
      def addLiteralToken(tokenList: List[Token], stringBuilder: StringBuilder) = {
        if (stringBuilder.isEmpty) {
          tokenList
        } else {
          val token = new Token(Token.Type.LITERAL, stringBuilder.toString())
          stringBuilder.setLength(0)
          tokenList :+ token
        }
      }
      if (pointer < patternLength) {
        val c = pattern(pointer)
        state match {
          case LITERAL_STATE ⇒
            (c: @switch) match {
              case CoreConstants.COLON_CHAR ⇒
                recursiveTokenize0(nextPointer, DEFAULT_VAL_STATE, addLiteralToken(tokenList, stringBuilder))
              case CoreConstants.CURLY_LEFT ⇒
                recursiveTokenize0(nextPointer, state, addLiteralToken(tokenList, stringBuilder) :+ Token.CURLY_LEFT_TOKEN)
              case CoreConstants.CURLY_RIGHT ⇒
                recursiveTokenize0(nextPointer, state, addLiteralToken(tokenList, stringBuilder) :+ Token.CURLY_RIGHT_TOKEN)
              case CoreConstants.DOLLAR ⇒
                if (nextPointer != patternLength) {
                  recursiveTokenize0(nextPointer, START_STATE, addLiteralToken(tokenList, stringBuilder))
                } else {
                  stringBuilder.append(c)
                  recursiveTokenize0(nextPointer, state, tokenList)
                }
              case _ ⇒
                stringBuilder.append(c)
                recursiveTokenize0(nextPointer, state, tokenList)
            }
          case START_STATE ⇒
            (c: @switch) match {
              case CoreConstants.CURLY_LEFT ⇒
                recursiveTokenize0(nextPointer, LITERAL_STATE, tokenList :+ Token.START_TOKEN)
              case _ ⇒
                stringBuilder.append(CoreConstants.DOLLAR).append(c)
                recursiveTokenize0(nextPointer, LITERAL_STATE, tokenList)
            }
          case DEFAULT_VAL_STATE ⇒
            (c: @switch) match {
              case CoreConstants.DASH_CHAR ⇒
                recursiveTokenize0(nextPointer, LITERAL_STATE, tokenList :+ Token.DEFAULT_SEP_TOKEN)
              case CoreConstants.DOLLAR ⇒
                recursiveTokenize0(nextPointer, START_STATE, addLiteralToken(tokenList, stringBuilder.append(CoreConstants.COLON_CHAR)))
              case _ ⇒
                stringBuilder.append(CoreConstants.COLON_CHAR).append(c)
                recursiveTokenize0(nextPointer, LITERAL_STATE, tokenList)
            }
        }
      } else {
        (state: @switch) match {
          case LITERAL_STATE ⇒
            addLiteralToken(tokenList, stringBuilder)
          case START_STATE ⇒
            throw new ScanException("Unexpected end of pattern string")
        }
      }
    }

    recursiveTokenize0(0, LITERAL_STATE, List.empty[Token]).asJava
  }
}