package com.danielceinos.ratatosk.stores

import com.danielceinos.ratatosk.NearbyController
import com.danielceinos.ratatosk.NodeId
import com.danielceinos.ratatosk.extensions.replace
import com.danielceinos.ratatosk.models.ConnectionStatus
import com.danielceinos.ratatosk.models.EndpointId
import com.danielceinos.ratatosk.models.Node
import com.danielceinos.rxnearbyconnections.RxNearbyConnections
import com.danielceinos.rxnearbyconnections.RxNearbyConnections.ConnectionInitiated
import com.danielceinos.rxnearbyconnections.RxNearbyConnections.ConnectionResult
import com.google.android.gms.nearby.connection.Payload
import mini.*

data class NodesState(
        val nameMap: Map<EndpointId, String> = mapOf(),
        val nodeIdMap: Map<EndpointId, NodeId> = mapOf(),
        val inSightMap: Map<EndpointId, Boolean> = mapOf(),
        val connectionStatusMap: Map<EndpointId, ConnectionStatus> = mapOf(),
        val pingMap: Map<EndpointId, Long> = mapOf()
) {

    fun node(nodeId: NodeId): Node? {
        val endpointId = endpointId(nodeId) ?: return null
        return nodeByEndpointId(endpointId)
    }

    fun nodeByEndpointId(endpointId: EndpointId): Node {
        return Node(
                endpointId = endpointId,
                name = nameMap[endpointId] ?: "Unknown",
                nodeId = nodeIdMap[endpointId] ?: "",
                inSight = inSightMap[endpointId] ?: false,
                connectionStatus = connectionStatusMap[endpointId]
                        ?: ConnectionStatus.DISCONNECTED
        )
    }

    fun name(nodeId: NodeId): String? {
        return nameMap[endpointId(nodeId)]
    }

    fun endpointId(nodeId: NodeId): String? {
        nodeIdMap.forEach {
            if (it.value == nodeId)
                return it.key
        }
        return null
    }

    fun getNodes(): List<Node> {
        val list = ArrayList<Node>()
        connectionStatusMap.forEach {
            list.add(nodeByEndpointId(it.key))
        }
        return list
    }

    fun contains(endpointId: EndpointId) =
            inSightMap.keys.contains(endpointId) &&
                    connectionStatusMap.keys.contains(endpointId)

    fun notContains(endpointId: EndpointId) = !contains(endpointId)
}

class NodesStore(val controller: NearbyController) : Store<NodesState>() {

    @Reducer
    fun endpointDiscovered(action: EndpointDiscoveredAction): NodesState {
        with(action.endpoint) {
            return state.copy(
                    nameMap = state.nameMap.replace(endpointId, discoveredEndpointInfo?.endpointName),
                    inSightMap = state.inSightMap.replace(endpointId, true),
                    connectionStatusMap = state.connectionStatusMap.replace(
                            endpointId,
                            ConnectionStatus.DISCONNECTED
                    )
            )
        }
    }

    @Reducer
    fun endpointLost(action: EndpointLostAction): NodesState {
        with(action.endpoint) {
            if (state.notContains(endpointId)) return state
            return state.copy(
                    connectionStatusMap =
                    state.connectionStatusMap.replace(endpointId, ConnectionStatus.DISCONNECTED),
                    inSightMap =
                    state.inSightMap.replace(endpointId, false)
            )
        }
    }

    @Reducer
    fun acceptConnection(action: AcceptConnectionAction): NodesState {
        with(action.connectionInitiated) {
            return state.copy(
                    nameMap = state.nameMap.replace(endpointId, connectionInfo.endpointName),
                    connectionStatusMap = state.connectionStatusMap.replace(
                            endpointId,
                            when (action.task.status) {
                                TaskStatus.SUCCESS -> ConnectionStatus.CONNECTED
                                TaskStatus.ERROR -> ConnectionStatus.DISCONNECTED
                                TaskStatus.IDLE, TaskStatus.RUNNING -> ConnectionStatus.CONNECTING
                            }
                    ),
                    inSightMap = state.inSightMap.replace(endpointId, true)
            )
        }
    }

    @Reducer
    fun connectionResult(action: ConnectionResultAction): NodesState {
        if (state.notContains(action.connectionResult.endpointId)) return state
        return state.copy(
                connectionStatusMap = state.connectionStatusMap.replace(
                        action.connectionResult.endpointId,
                        when (action.task.status) {
                            TaskStatus.IDLE, TaskStatus.RUNNING -> ConnectionStatus.CONNECTING
                            TaskStatus.SUCCESS -> ConnectionStatus.CONNECTED
                            TaskStatus.ERROR -> ConnectionStatus.DISCONNECTED
                        }
                )
        )
    }

    @Reducer
    fun disconect(action: DisconnectAction): NodesState {
        controller.disconnect(action.node.endpointId)
        return state.copy(
                connectionStatusMap = state.connectionStatusMap.replace(action.node.endpointId, ConnectionStatus.DISCONNECTING)
        )
    }

    @Reducer
    fun endpointDisconnected(action: EndpointDisconnectedAction): NodesState {
        with(action) {
            if (state.notContains(endpointId)) return state
            return state.copy(
                    connectionStatusMap = state.connectionStatusMap.replace(
                            endpointId,
                            ConnectionStatus.DISCONNECTED
                    )
            )
        }
    }

    @Reducer
    fun requestConnection(action: RequestConnectionAction): NodesState {
        if (state.notContains(action.endpointId)) return state
        return state.copy(
                connectionStatusMap = state.connectionStatusMap.replace(
                        action.endpointId,
                        when (action.task.status) {
                            TaskStatus.ERROR -> ConnectionStatus.DISCONNECTED
                            TaskStatus.SUCCESS,
                            TaskStatus.RUNNING,
                            TaskStatus.IDLE -> ConnectionStatus.CONNECTING
                        }
                )
        )
    }

    @Reducer
    fun uuidLoaded(action: UUIDLoadedAction): NodesState {
        if (state.notContains(action.endpointId)) return state
        return state.copy(
                nodeIdMap = state.nodeIdMap.replace(action.endpointId, action.uuid),
                connectionStatusMap = state.connectionStatusMap.replace(
                        action.endpointId,
                        ConnectionStatus.CONNECTED
                )
        )
    }

    @Reducer
    fun payloadReceived(action: OnPayloadReceivedAction): NodesState {
        controller.payloadReceived(action.payload, state.nodeByEndpointId(action.endpointId))
        return state
    }
}

data class EndpointDiscoveredAction(val endpoint: RxNearbyConnections.Endpoint) : Action
data class EndpointLostAction(val endpoint: RxNearbyConnections.Endpoint) : Action
data class EndpointDisconnectedAction(val endpointId: String) : Action

data class AcceptConnectionAction(val connectionInitiated: ConnectionInitiated, val task: Task) : Action
data class ConnectionResultAction(val connectionResult: ConnectionResult, val task: Task) : Action
data class RequestConnectionAction(val endpointId: String, val task: Task) : Action
data class UUIDLoadedAction(val endpointId: String, val uuid: String) : Action

data class OnPayloadReceivedAction(val payload: Payload, val endpointId: EndpointId) : Action

data class DisconnectAction(val node: Node) : Action
