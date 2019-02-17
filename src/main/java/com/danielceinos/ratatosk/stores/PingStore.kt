package com.danielceinos.ratatosk.stores

import com.danielceinos.ratatosk.NodeId
import com.danielceinos.ratatosk.extensions.replace
import com.danielceinos.ratatosk.models.Node
import mini.*

/**
 * Created by Daniel S on 17/02/2019.
 */
data class PingState(
    val pings: Map<NodeId, Long> = emptyMap(),
    val pingsSended: Map<NodeId, Long> = emptyMap(),
    val pingsTasks: Map<NodeId, Task> = emptyMap()
)

class PingStore : Store<PingState>() {

    @Reducer
    fun sendPing(action: SendPingAction): PingState {
        if (state.pingsTasks[action.node.nodeId]?.isRunning() == true) return state
        return state.copy(
            pingsSended = state.pingsSended.replace(action.node.nodeId, System.currentTimeMillis()),
            pingsTasks = state.pingsTasks.replace(action.node.nodeId, taskRunning())
        )
    }

    @Reducer
    fun pingReceived(action: PingReceivedAction): PingState {
        return state.copy(
            pings = state.pings.replace(
                action.node.nodeId,
                System.currentTimeMillis() - (state.pingsSended[action.node.nodeId] ?: System.currentTimeMillis())
            ),
            pingsTasks = state.pingsTasks.replace(action.node.nodeId, taskSuccess())
        )
    }
}

data class SendPingAction(val node: Node) : Action
data class SendPongAction(val node: Node) : Action
data class PingReceivedAction(val node: Node) : Action
data class PongReceivedAction(val node: Node) : Action