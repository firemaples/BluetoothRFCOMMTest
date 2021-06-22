package com.firemaples.rfcommtest.ui.client

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.firemaples.rfcommtest.R
import com.firemaples.rfcommtest.bluetooth.ConnectThread
import com.firemaples.rfcommtest.ui.BaseConnectedFragment
import com.firemaples.rfcommtest.utility.Constant
import com.firemaples.rfcommtest.utility.Logger

class ClientFragment : BaseConnectedFragment(R.layout.fragment_client) {
    private val logger: Logger = Logger(this::class)

    private val adapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    private lateinit var connectThread: ConnectThread

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(view)

        requireContext().registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_UUID)
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
        view.findViewById<View>(R.id.bt_findServer).setOnClickListener {
            enableBluetooth()
        }
    }

    private fun enableBluetooth() {
        if (adapter?.isEnabled == true) {
            onBluetoothEnabled()
        } else {
            startForResult.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            onBluetoothEnabled()
        }
    }

    private fun onBluetoothEnabled() {
        if (Constant.useCompanionManager) {
            findCompanionDevice()
        } else {
            discoveryServer()
        }
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

            val manager = requireContext().getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
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

            pairDevice(device)
        }
    }

    private fun discoveryServer() {
        val adapter = adapter ?: return

        val matchedDevice =
            adapter.bondedDevices.firstOrNull { it.uuids.contains(Constant.parcelUuid) }

        if (matchedDevice != null) {
            pairDevice(matchedDevice)
        } else {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }

            logger.debug("Start discovering devices")
            adapter.startDiscovery()
        }
    }

    private val foundDevices = mutableListOf<BluetoothDevice>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    logger.debug("ACTION_DISCOVERY_STARTED")

                    foundDevices.clear()
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    logger.debug("ACTION_DISCOVERY_FINISHED")

                    if (!Constant.useCompanionManager) fetchNextUUID()
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return

                    logger.debug(
                        "Found device, name: ${device.name}, address: ${device.address}, " +
                                "uuids: ${device.uuids?.joinToString(",")}"
                    )

                    foundDevices.add(device)

//                    if (device.uuids?.contains(ParcelUuid.fromString(Constant.uuid)) == true) {
//                        adapter?.cancelDiscovery()
//                        pairDevice(device)
//                    }
                }

                BluetoothDevice.ACTION_UUID -> {
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return
                    val uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)

                    logger.debug(
                        "Device uuid fetched, name: ${device.name}, address: ${device.address}, " +
                                "uuids: ${uuids?.joinToString(",")}, " +
                                "deviceUUIDs: ${device.uuids?.joinToString(",")}"
                    )
                    val matchedDevice = uuids?.firstOrNull { (it as ParcelUuid) == Constant.parcelUuid }

                    if (matchedDevice != null) {
                        adapter?.cancelDiscovery()
                        pairDevice(device)
                    } else {
                        fetchNextUUID()
                    }
                }
            }
        }
    }

    private fun fetchNextUUID() {
        if (foundDevices.isNotEmpty()) {
            val device = foundDevices.removeFirst()
            device.fetchUuidsWithSdp()
        } else {
            logger.debug("All found device's UUID is fetched")
        }
    }

    private fun pairDevice(device: BluetoothDevice) {
        logger.debug(
            "Pair device, name: ${device.name}, MAC address: ${device.address}, " +
                    "uuids: ${device.uuids.joinToString(",")}"
        )

        if (Constant.USE_CLIENT_SERVICE) {
            ClientService.pair(requireContext(), device)
        } else {
            connectThread = ConnectThread(
                device = device,
                onConnected = onConnected
            ).apply {
                start()
            }
        }
    }

    private val onConnected: (BluetoothSocket) -> Unit = {
        initConnectionManager(it)
    }
}
