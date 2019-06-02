package com.danielceinos.ratatosk.models

import java.sql.Timestamp

/**
 * Created by Daniel S on 17/11/2018.
 */
data class PayloadReceived(
    val payload: String,
    val node: Node,
    val timestamp: Timestamp,
    val readed: Boolean = false
)