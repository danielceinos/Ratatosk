package com.danielceinos.ratatosk.models

import com.danielceinos.ratatosk.NodeId

/**
 * Created by Daniel S on 11/11/2018.
 */
data class Node(
    val endpointId: EndpointId,
    var nodeId: NodeId = "",
    var name: String = "Sr. X",
    var inSight: Boolean = false,
    var connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    var ping: Long = 0
) {

    override fun toString(): String {
        return "Id: $endpointId | nodeId: $nodeId | name: $name | inSight: $inSight " +
                "| connection status: $connectionStatus | ping: $ping ms"
    }

    fun pingInMs() = "$ping ms"
}

enum class ConnectionStatus {
    CONNECTED, DISCONNECTED, CONNECTING
}

typealias EndpointId = String