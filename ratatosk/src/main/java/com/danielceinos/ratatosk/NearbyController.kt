package com.danielceinos.ratatosk

import android.annotation.SuppressLint
import android.content.Context
import com.danielceinos.ratatosk.models.EndpointId
import com.danielceinos.ratatosk.models.Node
import com.danielceinos.ratatosk.models.PayloadReceived
import com.danielceinos.ratatosk.stores.*
import com.danielceinos.rxnearbyconnections.RxNearbyConnections
import com.danielceinos.rxnearbyconnections.RxNearbyConnections.ConnectionInitiated
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Strategy
import io.reactivex.schedulers.Schedulers
import mini.Dispatcher
import mini.taskFailure
import mini.taskRunning
import mini.taskSuccess
import timber.log.Timber
import java.sql.Timestamp
import java.util.*

/**
 * Created by Daniel S on 17/02/2019.
 */
class NearbyController(val context: Context,
                       val rxNearby: RxNearbyConnections,
                       val dispatcher: Dispatcher) {

    private val PING_CHANEL = "PING_CHANEL"
    private val PONG_CHANEL = "PONG_CHANEL"
    private val UUID_CHANEL = "UUID_CHANEL"

    init {
        observeNearbyConnection()
    }

    @SuppressLint("CheckResult")
    fun startDiscovering(serviceId: String, strategy: Strategy) {
        Timber.i("startDiscovering")
        rxNearby.startDiscovery(context,
                serviceId,
                strategy)
                .observeOn(Schedulers.io())
                .subscribe ({
                    dispatcher.dispatchAsync(EnableDiscoveringCompleteAction(true))
                },{
                   Timber.e("startDiscovering error = $it")
                })
    }

    fun stopDiscovering() {
        rxNearby.stopDiscovery(context)
    }

    @SuppressLint("CheckResult")
    fun startAdvertising(name: String, serviceId: String, strategy: Strategy) {
        Timber.i("startAdvertising")
        rxNearby.startAdvertising(context,
                name,
                serviceId,
                strategy)
                .observeOn(Schedulers.io())
                .subscribe ({
                    dispatcher.dispatchAsync(EnableAdvertisingCompleteAction(true))
                },{
                    Timber.e("startAdvertising error = $it")
                })
    }

    fun stopAdvertising() {
        rxNearby.stopAdvertising(context)
    }

    fun disconnect(endpointId: EndpointId) {
        rxNearby.disconnect(context, endpointId)
    }

    @SuppressLint("CheckResult")
    fun sendPayload(endpointId: EndpointId, payload: Payload) {
        rxNearby.sendPayload(context, endpointId, payload)
                .observeOn(Schedulers.computation())
                .subscribe ({
                    dispatcher.dispatchAsync(PayloadSendedAction(payload, endpointId))
                },{
                    Timber.e("sendPayload error = $it")
                })
    }

    @SuppressLint("CheckResult")
    fun senToAllPayload(endpointIds: List<EndpointId>, payload: Payload) {
        rxNearby.sendPayload(context, endpointIds, payload)
                .observeOn(Schedulers.computation())
                .subscribe ({
                    dispatcher.dispatchAsync(
                        PayloadSendedToAllAction(
                            payload,
                            endpointIds
                        )
                    )
                },{
                    Timber.e("senToAllPayload error = $it")
                })
    }

    @SuppressLint("CheckResult")
    fun requestConnection(endpointId: EndpointId, name: String) {
        dispatcher.dispatchAsync(RequestConnectionAction(endpointId, taskRunning()))
        rxNearby.requestConnection(context, endpointId, name)
                .observeOn(Schedulers.io())
                .subscribe({
                    dispatcher.dispatchAsync(
                        RequestConnectionAction(
                            endpointId,
                            taskSuccess()
                        )
                    )
                }, {
                    dispatcher.dispatchAsync(
                        RequestConnectionAction(
                            endpointId,
                            taskFailure(it)
                        )
                    )
                })
    }

    @SuppressLint("CheckResult")
    fun acceptConnection(connectionInitiated: ConnectionInitiated) {
        dispatcher.dispatchAsync(
            AcceptConnectionAction(
                connectionInitiated,
                taskRunning()
            )
        )
        rxNearby.acceptConnection(context, connectionInitiated.endpointId)
                .observeOn(Schedulers.io())
                .subscribe({
                    dispatcher.dispatchAsync(
                        AcceptConnectionAction(
                            connectionInitiated,
                            taskSuccess()
                        )
                    )
                }, { error ->
                    dispatcher.dispatchAsync(
                        AcceptConnectionAction(
                            connectionInitiated,
                            taskFailure(error)
                        )
                    )
                })
    }

    fun sendPing(endpointId: String) {
        sendPayload(endpointId, Payload.fromBytes(PING_CHANEL.toByteArray()))
    }

    fun sendPong(endpointId: String) {
        sendPayload(endpointId, Payload.fromBytes(PONG_CHANEL.toByteArray()))
    }

    fun sendUUID(endpointId: String, uuid: String) {
        sendPayload(endpointId, Payload.fromBytes("$UUID_CHANEL=$uuid".toByteArray()))
    }

    @SuppressLint("CheckResult")
    private fun observeNearbyConnection() {
        rxNearby.onEndpointDiscovered
                .observeOn(Schedulers.io())
                .subscribe {
                    dispatcher.dispatchAsync(EndpointDiscoveredAction(it))
                }

        rxNearby.onEndpointLost
                .observeOn(Schedulers.io())
                .subscribe {
                    dispatcher.dispatchAsync(EndpointLostAction(it))
                }

        rxNearby.onConnectionInitiated
                .observeOn(Schedulers.io())
                .subscribe {
                    Timber.d("Connection initiated with ${it.endpointId}")
                    dispatcher.dispatchAsync(OnConnectionInitializedAction(it))
                }

        rxNearby.onConnectionResult
                .observeOn(Schedulers.io())
                .subscribe {
                    when (it.result.status.statusCode) {
                        ConnectionsStatusCodes.STATUS_OK -> {
                            Timber.d("STATUS_OK with ${it.endpointId}")
                            dispatcher.dispatchAsync(
                                ConnectionResultAction(
                                    it,
                                    taskSuccess()
                                )
                            )
                        }
                        ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                            Timber.d("STATUS_CONNECTION_REJECTED with ${it.endpointId}")
                            dispatcher.dispatchAsync(
                                ConnectionResultAction(
                                    it,
                                    taskFailure()
                                )
                            )
                        }
                        ConnectionsStatusCodes.STATUS_ERROR -> {
                            Timber.d("STATUS_ERROR with ${it.endpointId}")
                            dispatcher.dispatchAsync(
                                ConnectionResultAction(
                                    it,
                                    taskFailure()
                                )
                            )
                        }
                    }
                }

        rxNearby.onConnectionDisconnected
                .observeOn(Schedulers.io())
                .subscribe {
                    dispatcher.dispatchAsync(EndpointDisconnectedAction(it.endpointId))
                }

        rxNearby.onPayloadReceived
                .observeOn(Schedulers.io())
                .subscribe {
                    dispatcher.dispatch(
                        OnPayloadReceivedAction(
                            it.payload,
                            it.endpointId
                        )
                    )
                }
    }

    fun payloadReceived(payload: Payload, node: Node) {
        if (payload.type != Payload.Type.BYTES) return

        val payloadString = String(payload.asBytes()!!)
        Timber.d("Payload received from $node")
        Timber.d("Payload= $payloadString")
        when {
            payloadString == PING_CHANEL -> dispatcher.dispatchAsync(
                PongReceivedAction(
                    node
                )
            )
            payloadString == PONG_CHANEL -> dispatcher.dispatchAsync(
                PingReceivedAction(
                    node
                )
            )
            payloadString.contains(UUID_CHANEL) -> {
                val regex = "$UUID_CHANEL=([a-zA-Z0-9-]+)".toRegex().find(payloadString)
                regex?.groups?.get(1)?.value?.let { uuidReceived ->
                    dispatcher.dispatchAsync(
                        UUIDLoadedAction(
                            node.endpointId,
                            uuidReceived
                        )
                    )
                }
            }
            else -> dispatcher.dispatchAsync(
                DataReceivedAction(
                    PayloadReceived(
                        payloadString,
                        node,
                        Timestamp(Date().time)
                    )
                )
            )
        }
    }
}