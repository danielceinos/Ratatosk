package com.danielceinos.ratatosk.stores

import com.danielceinos.ratatosk.NearbyController
import com.danielceinos.ratatosk.models.EndpointId
import com.danielceinos.ratatosk.models.Node
import com.danielceinos.ratatosk.models.PayloadReceived
import com.google.android.gms.nearby.connection.Payload
import mini.Action
import mini.Reducer
import mini.Store

/**
 * Created by Daniel S on 03/02/2019.
 */
data class PayloadState(val payloads: List<PayloadReceived> = emptyList())

class PayloadStore(val controller: NearbyController) : Store<PayloadState>() {

    @Reducer
    fun onPayloadReceived(action: DataReceivedAction): PayloadState {
        val list = state.payloads.toMutableList().apply { add(action.payload) }
        return state.copy(payloads = list)
    }

    @Reducer
    fun onMarkPayload(action: MarkPayloadReadedAction): PayloadState {
        val list = state.payloads.map {
            if (it == action.payload)
                it.copy(readed = true)
            else
                it
        }
        return state.copy(payloads = list)
    }

    @Reducer
    fun onSendPayload(action: SendPayloadAction): PayloadState {
        controller.sendPayload(action.node.endpointId, action.payload)
        return state
    }

    @Reducer
    fun onCompletedSendPayload(action: PayloadSendedAction): PayloadState {
        return state
    }

    @Reducer
    fun onSendPayloadToAll(action: SendPayloadToAllAction): PayloadState {
        controller.senToAllPayload(action.nodes.map { it.endpointId }, action.payload)
        return state
    }

    @Reducer
    fun onCompleteSendPayloadToAll(action: PayloadSendedToAllAction): PayloadState {
        return state
    }
}

data class SendPayloadAction(val payload: Payload, val node: Node) : Action
data class PayloadSendedAction(val payload: Payload, val endpointId: EndpointId) : Action

data class SendPayloadToAllAction(val payload: Payload, val nodes: List<Node>) : Action
data class PayloadSendedToAllAction(val payload: Payload, val endpointIds: List<EndpointId>) : Action

data class DataReceivedAction(val payload: PayloadReceived): Action
data class MarkPayloadReadedAction(val payload: PayloadReceived) : Action
