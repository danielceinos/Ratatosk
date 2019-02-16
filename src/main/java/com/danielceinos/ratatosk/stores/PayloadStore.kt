package com.danielceinos.ratatosk.stores

import com.danielceinos.ratatosk.models.Node
import com.danielceinos.ratatosk.models.PayloadReceived
import com.google.android.gms.nearby.connection.Payload
import mini.Action
import mini.Reducer
import mini.Store
import java.sql.Timestamp
import java.util.*

/**
 * Created by Daniel S on 03/02/2019.
 */

data class PayloadReceivedAction(
    val payload: Payload,
    val node: Node
) : Action

data class MarkPayloadReadedAction(val payload: PayloadReceived) : Action

data class PayloadState(val payloads: List<PayloadReceived> = emptyList())

class PayloadStore : Store<PayloadState>() {

    @Reducer
    fun onPayloadReceived(action: PayloadReceivedAction): PayloadState {
        val list = state.payloads.toMutableList()
            .apply {
                add(
                    PayloadReceived(
                        payload = action.payload,
                        timestamp = Timestamp(Date().time),
                        readed = false,
                        node = action.node
                    )
                )
            }

        return state.copy(payloads = list)
    }

    @Reducer
    fun onMarkPayload(action: MarkPayloadReadedAction): PayloadState {
        val list = state.payloads
            .map {
                if (it == action.payload)
                    it.copy(readed = true)
                else
                    it
            }
        return state.copy(payloads = list)
    }
}