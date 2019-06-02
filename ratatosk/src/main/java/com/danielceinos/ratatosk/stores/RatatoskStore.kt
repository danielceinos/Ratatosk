package com.danielceinos.ratatosk.stores

import com.danielceinos.ratatosk.RatatoskStorage
import com.danielceinos.ratatosk.models.EndpointId
import com.danielceinos.rxnearbyconnections.RxNearbyConnections
import com.google.android.gms.nearby.connection.Strategy
import mini.Action
import mini.Reducer
import mini.Store

/**
 * Created by Daniel S on 16/02/2019.
 */
data class RatatoskState(
        val advertising: Boolean = false,
        val discovering: Boolean = false,
        val name: String = "Poeta Halley",
        val uuid: String = "",
        val autoDiscover: Boolean = true,
        val autoConnectOnDiscover: Boolean = true,
        val autoAcceptConnection: Boolean = true,
        val connecting: Boolean = false,
        val ping: Boolean = false,
        val serviceId: String = "serviceId",
        val strategy: Strategy = Strategy.P2P_CLUSTER)

class RatatoskStore(private val controller: NearbyController,
                    ratatoskStorage: RatatoskStorage) : Store<RatatoskState>() {

    init {
        state = state.copy(uuid = ratatoskStorage.getUUID())
    }

    @Reducer
    fun enableAdvertising(action: StartAdvertisingAction): RatatoskState {
        if (!state.advertising)
            controller.startAdvertising(state.name, state.serviceId, state.strategy)
        return state
    }

    @Reducer
    fun stopAdvertising(action: StopAdvertisingAction): RatatoskState {
        controller.stopAdvertising()
        return state.copy(advertising = false)
    }

    @Reducer
    fun onAdvertisingComplete(action: EnableAdvertisingCompleteAction): RatatoskState {
        return state.copy(advertising = action.advertising)
    }

    @Reducer
    fun enableDiscovering(action: StartDiscoveringAction): RatatoskState {
        if (!state.discovering)
            controller.startDiscovering(state.serviceId, state.strategy)
        return state
    }

    @Reducer
    fun stopDiscovering(action: StopDiscoveringAction): RatatoskState {
        controller.stopDiscovering()
        return state.copy(discovering = false)
    }

    @Reducer
    fun onDiscoveringComplete(action: EnableDiscoveringCompleteAction): RatatoskState {
        return state.copy(discovering = action.discovering)
    }

    @Reducer
    fun changeName(action: ChangeNameAction): RatatoskState {
        return state.copy(name = action.name)
    }

    @Reducer
    fun enableAutoDiscover(action: EnableAutoDiscoverAction): RatatoskState {
        return state.copy(autoDiscover = action.autoDiscover)
    }

    @Reducer
    fun endpointDiscovered(action: EndpointDiscoveredAction): RatatoskState {
        if (state.autoConnectOnDiscover)
            controller.requestConnection(action.endpoint.endpointId, state.name)
        return state
    }

    @Reducer
    fun connectToEndpoint(action: ConnectToEndpointAction): RatatoskState {
        controller.requestConnection(action.endpointId, state.name)
        return state.copy(connecting = true)
    }

    @Reducer
    fun connectionInitialized(action: OnConnectionInitializedAction): RatatoskState {
        if (state.autoAcceptConnection)
            controller.acceptConnection(action.connectionInitiated)
        return state
    }

    @Reducer
    fun connectionResult(action: ConnectionResultAction): RatatoskState {
        if (action.task.isSuccessful()) {
            controller.stopDiscovering()
            controller.sendUUID(action.connectionResult.endpointId, state.uuid)
        }
        return state
    }

    @Reducer
    fun onRequestConnection(action: RequestConnectionAction): RatatoskState {
        return state.copy(connecting = action.task.isRunning())
    }

    @Reducer
    fun enablePing(action: EnablePingAction): RatatoskState {
        return state.copy(ping = true)
    }

    @Reducer
    fun disablePing(action: DisablePingAction): RatatoskState {
        return state.copy(ping = false)
    }
}

class StartAdvertisingAction : Action
class StopAdvertisingAction : Action
data class EnableAdvertisingCompleteAction(val advertising: Boolean) : Action

data class ConnectToEndpointAction(val endpointId: EndpointId) : Action
class StartDiscoveringAction : Action
class StopDiscoveringAction : Action
data class EnableDiscoveringCompleteAction(val discovering: Boolean) : Action

class EnablePingAction : Action
class DisablePingAction : Action

data class ChangeNameAction(val name: String) : Action
data class EnableAutoDiscoverAction(val autoDiscover: Boolean) : Action

data class OnConnectionInitializedAction(val connectionInitiated: RxNearbyConnections.ConnectionInitiated) : Action
