package com.danielceinos.ratatosk.stores

import com.danielceinos.ratatosk.NodeId
import com.danielceinos.ratatosk.extensions.replace
import mini.*

/**
 * Created by Daniel S on 17/02/2019.
 */
data class SendPingAction(val nodeId: NodeId) : Action

data class PingReceivedAction(val nodeId: NodeId) : Action

data class PingState(
    val pings: Map<NodeId, Long>,
    val pingsTasks: Map<NodeId, Task>
)

class PingStore : Store<PingState>() {

    @Reducer
    fun sendPing(action: SendPingAction): PingState {
        if (state.pingsTasks[action.nodeId]?.isRunning() == true) return state
        return state.copy(pingsTasks = state.pingsTasks.replace(action.nodeId, taskRunning()))
    }

    @Reducer
    fun pingReceived(action: PingReceivedAction): PingState {
        return state.copy(
            pings =
            state.pings.replace(
                action.nodeId,
                System.currentTimeMillis() - (state.pings[action.nodeId] ?: System.currentTimeMillis())
            ),
            pingsTasks = state.pingsTasks.replace(action.nodeId, taskSuccess())
        )
    }
}