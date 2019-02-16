package com.danielceinos.ratatosk.models

import com.google.android.gms.nearby.connection.Payload

/**
 * Created by Daniel S on 17/11/2018.
 */
data class PayloadReceived(
  val payload: Payload,
  val node: Node,
  val timestamp: java.sql.Timestamp,
  val readed: Boolean = false
)