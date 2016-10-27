package com.pubgame.itd.db.json

import java.sql.{ PreparedStatement, ResultSet, Types }

import com.fasterxml.jackson.databind.JsonNode
import com.pubgame.alphakigo.util.Mapper
import org.postgresql.util.PGobject
import slick.ast.FieldSymbol
import slick.driver.PostgresDriver.DriverJdbcType

class JsonNodeJdbcType extends DriverJdbcType[JsonNode] {
  override def sqlTypeName(sym: Option[FieldSymbol]) = "JSONB"
  override def sqlType: Int = Types.OTHER
  override def getValue(r: ResultSet, idx: Int): JsonNode = {
    fromPgObject(r.getObject(idx).asInstanceOf[PGobject])
  }
  override def setValue(v: JsonNode, p: PreparedStatement, idx: Int): Unit = {
    p.setObject(idx, toPgObject(v), sqlType)
  }
  override def updateValue(v: JsonNode, r: ResultSet, idx: Int): Unit = {
    r.updateObject(idx, toPgObject(v))
  }
  override def valueToSQLLiteral(value: JsonNode): String = s"'$value'"
  override def hasLiteralForm = true

  private[this] def toPgObject(jsonNode: JsonNode): PGobject = {
    val json = new PGobject
    json.setType("jsonb")
    json.setValue(jsonNode.toString)
    json
  }

  private[this] def fromPgObject(pgObject: PGobject): JsonNode = {
    assert(pgObject.getType == "jsonb")
    Mapper.readTree(pgObject.getValue)
  }
}