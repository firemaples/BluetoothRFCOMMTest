package com.firemaples.rfcommtest.ui.client

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.ParcelUuid
import android.view.View
import android.widget.CheckBox
import com.firemaples.rfcommtest.R
import com.firemaples.rfcommtest.utility.Constant
import com.firemaples.rfcommtest.utility.Logger

class ClientBtDiscoveryFragment : BaseClientFragment(R.layout.fragment_client_bt_discovery) {
    private val logger: Logger = Logger(this::class)

    private val adapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    private var fetchUUIDsAfterScanning: Boolean = false

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
        val cbFetchUUIDs = view.findViewById<CheckBox>(R.id.cb_fetchUUIDs)
        view.findViewById<View>(R.id.bt_findByDiscovery).setOnClickListener {
            fetchUUIDsAfterScanning = cbFetchUUIDs.isChecked

            enableBluetooth()
        }
    }

    override fun onBluetoothEnabled() {
        discoveryServer()
    }

    private fun discoveryServer() {
        val adapter = adapter ?: return

        val matchedDevice =
            adapter.bondedDevices.firstOrNull { it.uuids.contains(Constant.parcelUuid) }

        if (matchedDevice != null) {
            logger.debug("Find a bounded device that matched the filter: $matchedDevice")
            connectDevice(matchedDevice)
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

                    if (fetchUUIDsAfterScanning) {
                        fetchNextUUID()
                    }
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return

                    logger.debug(
                        "Found device, name: ${device.name}, address: ${device.address}, " +
                                "uuids: ${device.uuids?.joinToString(",")}"
                    )

                    foundDevices.add(device)

                    if (Constant.PAIR_DEVICE_WITH_NAME
                        && device.name?.contains(Constant.PAIRING_DEVICE_NAME, ignoreCase = true) == true
                    ) {
                        logger.debug("Match device with name [${Constant.PAIRING_DEVICE_NAME}]")

                        adapter?.cancelDiscovery()
                        connectDevice(device)
                    }
                    if (device.uuids?.contains(Constant.parcelUuid) == true) {
                        adapter?.cancelDiscovery()
                        connectDevice(device)
                    }
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
                        connectDevice(device)
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
}
