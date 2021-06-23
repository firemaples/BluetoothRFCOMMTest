package com.firemaples.rfcommtest.ui.client

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import com.firemaples.rfcommtest.bluetooth.ConnectThread
import com.firemaples.rfcommtest.ui.BaseConnectedFragment
import com.firemaples.rfcommtest.utility.Constant
import com.firemaples.rfcommtest.utility.Logger

abstract class BaseClientFragment(@LayoutRes layoutId: Int) : BaseConnectedFragment(layoutId) {
    private val logger: Logger = Logger(this::class)

    private val adapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    private lateinit var connectThread: ConnectThread

    protected fun enableBluetooth() {
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

    abstract fun onBluetoothEnabled()

    protected fun connectDevice(device: BluetoothDevice) {
        logger.debug(
            "Pair device, name: ${device.name}, MAC address: ${device.address}, " +
                    "uuids: ${device.uuids?.joinToString(",")}"
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
