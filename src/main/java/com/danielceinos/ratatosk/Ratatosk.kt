package com.danielceinos.ratatosk

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import com.danielceinos.ratatosk.models.ConnectionStatus.CONNECTED
import com.danielceinos.ratatosk.models.ConnectionStatus.DISCONNECTED
import com.danielceinos.ratatosk.models.EndpointId
import com.danielceinos.ratatosk.models.PayloadReceived
import com.danielceinos.ratatosk.stores.*
import com.danielceinos.rxnearbyconnections.RxNearbyConnections
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Strategy
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import mini.*
import mini.log.LoggerInterceptor
import timber.log.Timber

/**
 * Created by Daniel S on 07/11/2018.
 */

typealias NodeId = String

class Ratatosk constructor(
    private val context: Context,
    var name: String = "Poeta Halley",
    var serviceId: String = "default",
    var autoConnectOnDiscover: Boolean = true,
    var retryConnection: Boolean = true
) {

    private val PING_CHANEL = "PING_CHANEL"
    private val PONG_CHANEL = "PONG_CHANEL"
    private val UUID_CHANEL = "UUID_CHANEL"

    private val PING_TIME = 1000L

    private val connectionQueue = mutableListOf<String>()
    private var connecting = false

    private val gson = Gson()
    private val uuid by lazy {
        RatatoskStorage(
            context.getSharedPreferences("Ratatosk", 0)
        ).getUUID()
    }
    private var autoDiscoveryHandler: Handler? = null
    private var pingHandler: Handler? = null

    private val rxNearby: RxNearbyConnections
    val nodesStore: NodesStore
    val payloadStore: PayloadStore
    val ratatoskStore: RatatoskStore
    val pingStore: PingStore

    private val dispatcher: Dispatcher

    init {
        Timber.i("Init Ratatosk")
        rxNearby = RxNearbyConnections()
        rxNearby.stopAll(context)

        nodesStore = NodesStore()
        payloadStore = PayloadStore()
        ratatoskStore = RatatoskStore()
        pingStore = PingStore()

        val stores = mapOf<Class<*>, Store<*>>(
            NodesStore::class.java to nodesStore,
            PayloadStore::class.java to payloadStore,
            RatatoskStore::class.java to ratatoskStore,
            PingStore::class.java to pingStore
        )
        val actionReducer = MiniActionReducer(stores = stores)
        val loggerInterceptor = LoggerInterceptor(stores.values)

        dispatcher = Dispatcher()
        dispatcher.addActionReducer(actionReducer)
        dispatcher.addInterceptor(loggerInterceptor)

        initStores(stores.values)

        stopDiscovering()
        stopAdvertising()
        observeNearbyConnection()
    }

    @SuppressLint("CheckResult")
    fun startDiscovering() {
        Timber.i("startDiscovering")
        rxNearby.startDiscovery(context, serviceId, Strategy.P2P_CLUSTER)
            .observeOn(Schedulers.io())
            .subscribe {
                dispatcher.dispatch(DiscoveringAction(true))
            }
    }

    fun stopDiscovering() {
        Timber.i("stopDiscovering")
        rxNearby.stopDiscovery(context)
        dispatcher.dispatch(DiscoveringAction(false))
    }

    @SuppressLint("CheckResult")
    fun startAdvertising() {
        Timber.i("startAdvertising")
        rxNearby.startAdvertising(context, name, serviceId, Strategy.P2P_CLUSTER)
            .observeOn(Schedulers.io())
            .subscribe {
                dispatcher.dispatch(AdvertisingAction(true))
            }
    }

    fun stopAdvertising() {
        Timber.i("stopAdvertising")
        rxNearby.stopAdvertising(context)
        dispatcher.dispatch(AdvertisingAction(false))
    }

    fun disconnect(endpointId: String) {
        Timber.i("disconnect from $endpointId")
        rxNearby.disconnect(context, endpointId)
    }

    fun sendData(
        nodeId: NodeId,
        data: Any,
        cb: () -> Unit = {}
    ) {
        nodesStore.state.endpointId(nodeId)?.let {
            sendPayload(it, Payload.fromBytes(gson.toJson(data).toByteArray()), cb)
        }
    }

    fun sendToAllData(
        data: Any,
        cb: () -> Unit = {}
    ) {
        senToAllPayload(Payload.fromBytes(gson.toJson(data).toByteArray()), cb)
    }

    @SuppressLint("CheckResult")
    private fun sendPayload(
        endpointId: EndpointId,
        payload: Payload,
        cb: () -> Unit = {}
    ) {
        rxNearby.sendPayload(context, endpointId, payload)
            .observeOn(Schedulers.computation())
            .subscribe {
                Timber.i("Send payload to $endpointId")
                cb.invoke()
            }
    }

    @SuppressLint("CheckResult")
    fun senToAllPayload(
        payload: Payload,
        cb: () -> Unit = {}
    ) {
        rxNearby.sendPayload(context, nodesStore.state.connectionStatusMap
            .filter { it.value == CONNECTED }
            .map { it.key }, payload
        )
            .observeOn(Schedulers.computation())
            .subscribe {
                Timber.i("Send to all payload")
                cb.invoke()
            }
    }

    fun enableAutoDiscover() {
        if (autoDiscoveryHandler == null) {
            Timber.i("Enabled Auto Discover")
            autoDiscoveryHandler = Handler()
            val runnable = object : Runnable {
                override fun run() {
                    Timber.v("Checking for enable discovering...")
                    if (nodesStore.state.connectionStatusMap.values.none { it == CONNECTED }
                        && !ratatoskStore.state.discovering) {
                        startDiscovering()
                    }
                    autoDiscoveryHandler?.postDelayed(this, 10000)
                }
            }
            autoDiscoveryHandler?.postDelayed(runnable, 1000)
        }
    }

    fun disableAutoDiscover() {
        Timber.i("Disable Auto Discover")
        autoDiscoveryHandler?.removeCallbacksAndMessages(null)
    }

    fun enablePing() {
        if (pingHandler == null) {
            Timber.i("Enabled Ping")
            pingHandler = Handler()
            val runnable = object : Runnable {
                override fun run() {
                    nodesStore.state.connectionStatusMap
                        .filter { it.value == CONNECTED }
                        .forEach {
                            Timber.i("Pinging...")
                            sendPing(it.key)
                        }
                    pingHandler?.postDelayed(this, PING_TIME)
                }
            }
            pingHandler?.post(runnable)
        }
    }

    private fun sendPing(endpointId: String) {
        dispatcher.dispatch(SendPingAction(endpointId))
        sendPayload(endpointId, Payload.fromBytes(PING_CHANEL.toByteArray()))
    }

    private fun sendPong(endpointId: String) {
        sendPayload(endpointId, Payload.fromBytes(PONG_CHANEL.toByteArray()))
    }

    private fun sendUUID(endpointId: String) {
        Timber.i("Sending UUID to $endpointId")
        sendPayload(endpointId, Payload.fromBytes("$UUID_CHANEL=$uuid".toByteArray()))
    }

    @SuppressLint("CheckResult")
    fun connectToEndpoint(endpointId: String) {
        if (connecting) {
            connectionQueue.add(endpointId)
            return
        }

        if (nodesStore.state.connectionStatusMap[endpointId] == DISCONNECTED) {
            dispatcher.dispatch(RequestConnectionAction(endpointId, taskRunning()))
            connecting = true
            rxNearby.requestConnection(context, endpointId, name)
                .observeOn(Schedulers.io())
                .subscribe({
                    dispatcher.dispatch(
                        RequestConnectionAction(
                            endpointId,
                            taskSuccess()
                        )
                    )
                    connecting = false
                    if (connectionQueue.isNotEmpty()) {
                        connectToEndpoint(connectionQueue.first())
                        connectionQueue.removeAt(0)
                    }
                }, {
                    dispatcher.dispatch(
                        RequestConnectionAction(
                            endpointId,
                            taskFailure(it)
                        )
                    )
                    connecting = false
                })
        }
    }

    fun connectToAll() {
        nodesStore.state.connectionStatusMap
            .forEach {
                if (it.value == CONNECTED &&
                    nodesStore.state.inSightMap[it.key] == true
                )
                    connectToEndpoint(it.key)
            }
    }

    fun markPayloadRead(payload: PayloadReceived) {
        dispatcher.dispatch(MarkPayloadReadedAction(payload))
    }

    @SuppressLint("CheckResult")
    private fun observeNearbyConnection() {
        rxNearby.onEndpointDiscovered
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                dispatcher.dispatch(EndpointDiscoveredAction(it))
                if (autoConnectOnDiscover)
                    connectToEndpoint(it.endpointId)
            }

        rxNearby.onEndpointLost
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                dispatcher.dispatch(EndpointLostAction(it))
            }

        rxNearby.onConnectionInitiated
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("Connection initiated with ${it.endpointId}")
                dispatcher.dispatch(AcceptConnectionAction(it, taskRunning()))
                rxNearby.acceptConnection(context, it.endpointId)
                    .observeOn(Schedulers.io())
                    .subscribe({ _ ->
                        dispatcher.dispatch(AcceptConnectionAction(it, taskSuccess()))
                    }, { error ->
                        dispatcher.dispatch(
                            AcceptConnectionAction(
                                it,
                                taskFailure(error)
                            )
                        )
                    })
            }

        rxNearby.onConnectionResult
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                when (it.result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        Timber.d("STATUS_OK with ${it.endpointId}")
                        dispatcher.dispatch(
                            ConnectionResultAction(
                                it.endpointId,
                                taskSuccess()
                            )
                        )
                        stopDiscovering()
                        sendUUID(it.endpointId)
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        Timber.d("STATUS_CONNECTION_REJECTED with ${it.endpointId}")
                        dispatcher.dispatch(
                            ConnectionResultAction(
                                it.endpointId,
                                taskFailure()
                            )
                        )
                    }
                    ConnectionsStatusCodes.STATUS_ERROR -> {
                        Timber.d("STATUS_ERROR with ${it.endpointId}")
                        dispatcher.dispatch(
                            ConnectionResultAction(
                                it.endpointId,
                                taskFailure()
                            )
                        )
                    }
                }
            }

        rxNearby.onConnectionDisconnected
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                dispatcher.dispatch(EndpointDisconnectedAction(it.endpointId))
            }

        rxNearby.onPayloadReceived
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.payload.type != Payload.Type.BYTES) return@subscribe
                val payloadString = String(it.payload.asBytes()!!)
                Timber.d("Payload received from ${it.endpointId}")
                Timber.d("Payload= $payloadString")
                val node = nodesStore.state.nodeByEndpointId(it.endpointId)
                when {
                    payloadString == PING_CHANEL -> sendPong(it.endpointId)
                    payloadString == PONG_CHANEL -> dispatcher.dispatch(PingReceivedAction(it.endpointId))
                    payloadString.contains(UUID_CHANEL) -> {
                        val regex = "$UUID_CHANEL=([a-zA-Z0-9-]+)".toRegex()
                            .find(payloadString)
                        regex?.groups?.get(1)
                            ?.value?.let { uuidReceived ->
                            dispatcher.dispatch(
                                UUIDLoadedAction(
                                    it.endpointId,
                                    uuidReceived
                                )
                            )
                        }
                    }
                    else -> dispatcher.dispatch(
                        PayloadReceivedAction(
                            it.payload,
                            node = node
                        )
                    )
                }
            }
    }

    fun onDestroy() {
        rxNearby.stopAll(context)
    }
}