package com.firemaples.rfcommtest.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.firemaples.rfcommtest.utility.Constant
import com.firemaples.rfcommtest.utility.Logger

class ConnectThread(
    device: BluetoothDevice,
    private val onConnected: (BluetoothSocket) -> Unit,
) :
    Thread("bt-connect-thread") {
    private val logger: Logger = Logger(this::class)

    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(Constant.uuid)
    }

    override fun run() {
        mmSocket?.also { socket ->
            try {
                socket.connect()
            } catch (e: Exception) {
                logger.error("Connect socket failed", e = e)
                cancel()
                return
            }
            manageMyConnectedSocket(socket)
        }
    }

    private fun manageMyConnectedSocket(socket: BluetoothSocket) {
        logger.debug("Socket connected")

        onConnected.invoke(socket)
    }

    fun cancel() {
        try {
            mmSocket?.close()
        } catch (e: Exception) {
            logger.error(e = e)
        }
    }
}