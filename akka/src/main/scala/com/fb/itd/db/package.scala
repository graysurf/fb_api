package com.pubgame.itd

import com.pubgame.itd.db.json.JsonNodeJdbcType

package object db {
  implicit val jsonNodeJdbcType = new JsonNodeJdbcType
}
