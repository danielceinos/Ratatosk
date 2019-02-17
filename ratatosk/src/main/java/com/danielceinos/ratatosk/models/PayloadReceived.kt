package com.danielceinos.ratatosk.models

import com.google.android.gms.nearby.connection.Payload
import java.sql.Timestamp

/**
 * Created by Daniel S on 17/11/2018.
 */
data class PayloadReceived(
        val payload: Payload,
        val node: Node,
        val timestamp: Timestamp,
        val readed: Boolean = false
)