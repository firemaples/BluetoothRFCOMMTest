package com.firemaples.rfcommtest.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import com.firemaples.rfcommtest.utility.Logger
import java.io.IOException
import java.util.*

class AcceptThread(
    bluetoothAdapter: BluetoothAdapter,
    val displayName: String,
    uuid: UUID,
    val onConnected: (BluetoothSocket) -> Unit,
) : Thread("bt-accept-thread") {
    private val logger: Logger = Logger(this::class)

    init {
        bluetoothAdapter.name = displayName
    }

    private val mmServerSocket: BluetoothServerSocket? by lazy {
        // The name here is not working
        bluetoothAdapter.listenUsingRfcommWithServiceRecord(displayName, uuid)
//        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(displayName, uuid)
    }

    override fun run() {
        logger.debug("Create server socket with name: $displayName, socket: $mmServerSocket")

        var shouldLoop = true
        while (shouldLoop) {
            val socket: BluetoothSocket? = try {
                mmServerSocket?.accept()
            } catch (e: IOException) {
                logger.error("Accept socket failed", e = e)
                shouldLoop = false
                null
            }
            socket?.also {
                manageMyConnectedSocket(it)
                mmServerSocket?.close()
                shouldLoop = false
            }
        }
    }

    private fun manageMyConnectedSocket(it: BluetoothSocket) {
        logger.debug("Socket connected: $it")

        onConnected.invoke(it)
    }

    fun cancel() {
        try {
            mmServerSocket?.close()
        } catch (e: IOException) {
            logger.error(e = e)
        }
    }
}