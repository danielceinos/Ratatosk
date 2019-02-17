package com.danielceinos.ratatosk

import android.annotation.SuppressLint
import android.os.Handler
import com.danielceinos.ratatosk.models.ConnectionStatus
import com.danielceinos.ratatosk.models.Node
import com.danielceinos.ratatosk.stores.*
import io.reactivex.schedulers.Schedulers
import mini.*
import timber.log.Timber

/**
 * Created by Daniel S on 16/02/2019.
 */
@SuppressLint("CheckResult")
class RatatoskController(val dispatcher: Dispatcher,
                         val nodesStore: NodesStore,
                         val ratatoskStore: RatatoskStore) {

    private val PING_TIME = 5000L

    private var autoDiscoveryHandler: Handler? = null
    private var pingHandler: Handler? = null
//    private val connectionQueue = mutableSetOf<NodeId>()

    init {
        ratatoskStore.flowable()
                .select { it.autoDiscover }
                .subscribe {
                    if (it) enableAutoDiscover()
                    else disableAutoDiscover()
                }

        ratatoskStore.flowable()
                .select { it.ping }
                .subscribe {
                    if (it) enablePing()
                    else disablePing()
                }

//        nodesStore.flowable()
//                .select { it.connectionStatusMap }
//                .subscribe {
//                    if (connectionQueue.isNotEmpty()) {
//                        connectToEndpoint(connectionQueue.first())
//                        connectionQueue.removeAt(0)
//                    }
//                }
    }

    private fun enableAutoDiscover() {
        if (autoDiscoveryHandler == null) {
            Timber.i("Enabled Auto Discover")
            autoDiscoveryHandler = Handler()
            val runnable = object : Runnable {
                override fun run() {
                    Timber.v("Checking for enable discovering...")
                    if (nodesStore.state.connectionStatusMap.values.none { it == ConnectionStatus.CONNECTED }
                            && !ratatoskStore.state.discovering) {
                        dispatcher.dispatch(StartDiscoveringAction())
                    }
                    autoDiscoveryHandler?.postDelayed(this, 10000)
                }
            }
            autoDiscoveryHandler?.postDelayed(runnable, 1000)
        }
    }

    private fun disableAutoDiscover() {
        Timber.i("Disable Auto Discover")
        autoDiscoveryHandler?.removeCallbacksAndMessages(null)
    }

    private fun enablePing() {
        if (pingHandler == null) {
            Timber.i("Enabled Ping")
            pingHandler = Handler()
            val runnable = object : Runnable {
                override fun run() {
                    nodesStore.state.getNodes()
                            .filter { it.connectionStatus == ConnectionStatus.CONNECTED }
                            .forEach {
                                dispatcher.dispatch(SendPingAction(it))
                            }
                    pingHandler?.postDelayed(this, PING_TIME)
                }
            }
            pingHandler?.post(runnable)
        }
    }

    private fun disablePing() {
        pingHandler?.removeCallbacksAndMessages(null)
    }

//    fun connectToEndpoint(nodeId: NodeId) {
//        if (ratatoskStore.state.connecting) {
//            connectionQueue.add(nodeId)
//            return
//        }
//
//        if (nodesStore.state.node(nodeId)?.connectionStatus == ConnectionStatus.DISCONNECTED) {
//            dispatcher.dispatch(RequestConnectionAction(endpointId, taskRunning()))
//        }
//    }
}