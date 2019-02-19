package com.danielceinos.sample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.danielceinos.ratatosk.Ratatosk
import com.danielceinos.ratatosk.extensions.select
import com.danielceinos.sample.databinding.ActivityMainBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import mini.mapNotNull
import mini.select
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var nodesAdapter: NodesAdapter
    private val ratatosk by lazy {
        Ratatosk(this)
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(
            this,
            R.layout.activity_main
        )

        nodesAdapter = NodesAdapter {
            ratatosk.connectTo(it.endpointId)
        }
        binding.nodessRv.adapter = nodesAdapter

        ratatosk.getNodes()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                nodesAdapter.setNodes(it)
            }

        ratatosk.getRatatoskState()
            .mapNotNull { it.advertising }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { advertising ->
                binding.advertisingB.text = when (advertising) {
                    true -> "Stop Advertising"
                    false -> "Advert"
                }
                when (advertising) {
                    true -> binding.advertisingB.setOnClickListener { ratatosk.stopAdvertising() }
                    false -> binding.advertisingB.setOnClickListener { ratatosk.startAdvertising() }
                }
            }

        ratatosk.getRatatoskState()
            .mapNotNull { it.discovering }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { discovering ->
                binding.discoveringB.text = when (discovering) {
                    true -> "Stop Discovering"
                    false -> "Discover"
                }
                when (discovering) {
                    true -> binding.discoveringB.setOnClickListener { ratatosk.stopDiscovering() }
                    false -> binding.discoveringB.setOnClickListener { ratatosk.startDiscovering() }
                }
            }

        ratatosk.getPings()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                nodesAdapter.setPings(it)
            }

        ratatosk.startAdvertising()
        ratatosk.enablePing()

//        nearbyConnect.flowable()
//            .mapNotNull { it.payloadsReceived }
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe {
//                var text = ""
//                for (payloadReceived in it)
//                    text += "${payloadReceived.timestamp} - ${String(payloadReceived.payload.asBytes()!!)}\n"
//                binding.receivedTv.text = text
//            }
//
//        nearbyConnect.startAdvertising()
//        nearbyConnect.startDiscovering()
//        binding.sendB.setOnClickListener {
//            val text = binding.textEt.text.toString()
//            nearbyConnect.sendPayload(
//                lastEndpoint, Payload.fromBytes(text.toByteArray())
//            )
//        }
    }
}
