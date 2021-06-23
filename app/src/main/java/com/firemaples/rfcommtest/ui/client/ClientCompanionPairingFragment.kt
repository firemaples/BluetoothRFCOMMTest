package com.firemaples.rfcommtest.ui.client

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.firemaples.rfcommtest.R
import com.firemaples.rfcommtest.utility.Constant
import com.firemaples.rfcommtest.utility.Logger

@RequiresApi(Build.VERSION_CODES.O)
class ClientCompanionPairingFragment : BaseClientFragment(R.layout.fragment_client_companion_pairing) {
    private val logger: Logger = Logger(this::class)

    private val manager: CompanionDeviceManager by lazy {
        requireContext().getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    }

    private val associatedAddresses = mutableListOf<String>()
    private var isPairing: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(view)

        requireContext().registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(receiver)
    }

    private fun setViews(view: View) {
        view.findViewById<View>(R.id.bt_findByCompanionPairing).setOnClickListener {
            if (isPairing) {
                Toast.makeText(requireContext(), "It pairing now", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            enableBluetooth()
        }
        view.findViewById<View>(R.id.bt_disassociateCompanions).setOnClickListener {
            disassociateDevices()
        }
    }

    override fun onBluetoothEnabled() {
        findCompanionDevice()
    }

    private fun findCompanionDevice() {
        logger.debug("Start to find companion devices")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val deviceFilter = BluetoothDeviceFilter.Builder()
//                .setAddress()
//                .setNamePattern()
                .addServiceUuid(Constant.parcelUuid, null)
                .build()

            val pairingRequest = AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(true)
                .build()

            manager.associate(pairingRequest, object : CompanionDeviceManager.Callback() {
                override fun onDeviceFound(chooserLauncher: IntentSender?) {
                    logger.debug("Companion device found: $chooserLauncher")
                    if (chooserLauncher == null) return
                    onPairingResult.launch(IntentSenderRequest.Builder(chooserLauncher).build())
                }

                override fun onFailure(error: CharSequence?) {
                    logger.debug("Failed to request for pairing companion devices: $error")
                }

            }, null)
        }
    }

    val onPairingResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val device = it.data?.getParcelableExtra<BluetoothDevice>(CompanionDeviceManager.EXTRA_DEVICE)
                ?: return@registerForActivityResult

            if (!associatedAddresses.contains(device.address)) {
                associatedAddresses.add(device.address)
            }

            connectDevice(device)
        }
    }

    private fun disassociateDevices() {
        associatedAddresses.forEach {
            logger.debug("Disassociating $it")
            manager.disassociate(it)
        }
        associatedAddresses.clear()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    logger.debug("ACTION_DISCOVERY_STARTED")
                    isPairing = true
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    logger.debug("ACTION_DISCOVERY_FINISHED")
                    isPairing = false
                }
            }
        }
    }
}
