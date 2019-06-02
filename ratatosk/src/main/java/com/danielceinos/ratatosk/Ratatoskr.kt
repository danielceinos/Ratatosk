package com.danielceinos.ratatosk

import android.annotation.SuppressLint
import android.content.Context
import com.danielceinos.ratatosk.models.ConnectionStatus.CONNECTED
import com.danielceinos.ratatosk.models.ConnectionStatus.DISCONNECTED
import com.danielceinos.ratatosk.models.EndpointId
import com.danielceinos.ratatosk.models.Node
import com.danielceinos.ratatosk.models.PayloadReceived
import com.danielceinos.ratatosk.stores.*
import com.danielceinos.rxnearbyconnections.RxNearbyConnections
import com.google.android.gms.nearby.connection.Payload
import com.google.gson.Gson
import mini.*
import mini.log.DebugTree
import mini.log.Grove
import mini.log.LoggerInterceptor
import timber.log.Timber

/**
 * Created by Daniel S on 07/11/2018.
 */

typealias NodeId = String

class Ratatoskr constructor(private val context: Context) {

    private val gson = Gson()

    private val rxNearby: RxNearbyConnections
    private val nodesStore: NodesStore
    private val payloadStore: PayloadStore
    private val ratatoskStore: RatatoskStore
    private val pingStore: PingStore

    private val dispatcher: Dispatcher

    init {
        Grove.plant(DebugTree())
        Timber.plant(Timber.DebugTree())
        Timber.i("Init Ratatoskr")
        rxNearby = RxNearbyConnections()
        rxNearby.stopAll(context)
        dispatcher = Dispatcher()

        val nearbyController = NearbyController(context, rxNearby, dispatcher)
        nodesStore = NodesStore(nearbyController)
        payloadStore = PayloadStore(nearbyController)
        ratatoskStore = RatatoskStore(nearbyController, RatatoskStorage(context.getSharedPreferences("Ratatoskr", 0)))
        pingStore = PingStore(nearbyController)

        RatatoskController(dispatcher, nodesStore, ratatoskStore)

        val stores = mapOf<Class<*>, Store<*>>(
            NodesStore::class.java to nodesStore,
            PayloadStore::class.java to payloadStore,
            RatatoskStore::class.java to ratatoskStore,
            PingStore::class.java to pingStore
        )
        val actionReducer = MiniActionReducer(stores = stores)
        val loggerInterceptor = LoggerInterceptor(stores.values)

        dispatcher.addActionReducer(actionReducer)
        dispatcher.addInterceptor(loggerInterceptor)

        initStores(stores.values)

        stopDiscovering()
        stopAdvertising()
    }

    @SuppressLint("CheckResult")
    fun setName(name: String) {
        dispatcher.dispatch(ChangeNameAction(name))
    }

    @SuppressLint("CheckResult")
    fun startDiscovering() {
        dispatcher.dispatch(StartDiscoveringAction())
    }

    fun stopDiscovering() {
        dispatcher.dispatch(StopDiscoveringAction())
    }

    @SuppressLint("CheckResult")
    fun startAdvertising() {
        dispatcher.dispatch(StartAdvertisingAction())
    }

    fun stopAdvertising() {
        dispatcher.dispatch(StopAdvertisingAction())
    }

    fun connectTo(endpointId: EndpointId) {
        if (nodesStore.state.connectionStatusMap[endpointId] == DISCONNECTED)
            dispatcher.dispatch(ConnectToEndpointAction(endpointId))
    }

    fun disconnect(node: Node) {
        dispatcher.dispatch(DisconnectAction(node))
    }

    fun sendData(node: Node, data: Any) {
        dispatcher.dispatch(SendPayloadAction(Payload.fromBytes(gson.toJson(data).toByteArray()), node))
    }

    fun sendToAllData(data: Any) {
        val nodes = nodesStore.state.getNodes().filter { it.connectionStatus == CONNECTED }
        dispatcher.dispatch(SendPayloadToAllAction(Payload.fromBytes(gson.toJson(data).toByteArray()), nodes))
    }

    fun enableAutoDiscover() {
        dispatcher.dispatch(EnableAutoDiscoverAction(true))
    }

    fun disableAutoDiscover() {
        dispatcher.dispatch(EnableAutoDiscoverAction(false))
    }

    fun enableAutoConnect() {
        dispatcher.dispatch(EnableAutoDiscoverAction(true))
    }

    fun disableAutoConnect() {
        dispatcher.dispatch(EnableAutoDiscoverAction(false))
    }

    fun enablePing() {
        dispatcher.dispatch(EnablePingAction())
    }

    fun disablePing() {
        dispatcher.dispatch(DisablePingAction())
    }

    fun markPayloadRead(payload: PayloadReceived) {
        dispatcher.dispatch(MarkPayloadReadedAction(payload))
    }

    fun onDestroy() {
        rxNearby.stopAll(context)
    }

    fun getNodesFlowable() = nodesStore.flowable().select { it.getNodes() }

    fun getRatatoskStateFlowable() = ratatoskStore.flowable()

    fun getPayloadsFlowable() = payloadStore.flowable().select { it.payloads }

    fun getPingsFlowable() = pingStore.flowable().select { it.pings }

    fun getNodes() = nodesStore.state.getNodes()

    fun getRatatoskState() = ratatoskStore.state

    fun getPayloads() = payloadStore.state.payloads

    fun getPings() = pingStore.state.pings
}