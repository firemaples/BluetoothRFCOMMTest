package com.firemaples.rfcommtest.ui.client

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import com.firemaples.rfcommtest.bluetooth.ConnectThread
import com.firemaples.rfcommtest.bluetooth.ConnectionManager
import com.firemaples.rfcommtest.utility.Constant
import com.firemaples.rfcommtest.utility.Logger
import java.nio.charset.Charset

class ClientService : Service() {
    companion object {
        private const val ACTION_PAIR_DEVICE = "action_pair_device"
        private const val EXTRA_DEVICE = "extra_device"

        private const val ACTION_MESSAGE = "action_message"
        private const val EXTRA_MESSAGE = "extra_message"

        fun pair(context: Context, bluetoothDevice: BluetoothDevice) {
            context.startService(Intent(context, ClientService::class.java).apply {
                action = ACTION_PAIR_DEVICE
                putExtra(EXTRA_DEVICE, bluetoothDevice)
            })
        }

        fun sendMessage(context: Context, message: String) {
            context.startService(Intent(context, ClientService::class.java).apply {
                action = ACTION_MESSAGE
                putExtra(EXTRA_MESSAGE, message)
            })
        }
    }

    private val logger: Logger = Logger(this::class)

    private lateinit var connectThread: ConnectThread
    private lateinit var connectionManager: ConnectionManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        logger.debug("Received action: ${intent?.action}")

        when (intent?.action) {
            ACTION_PAIR_DEVICE -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_DEVICE)

                if (device != null) {
                    pair(device)
                }
            }

            ACTION_MESSAGE -> {
                val message = intent.getStringExtra(EXTRA_MESSAGE)

                if (message != null) {
                    connectionManager.write(message)
                }
            }
        }

        return START_STICKY
    }

    private fun pair(device: BluetoothDevice) {
        connectThread = ConnectThread(
            device = device,
            onConnected = onConnected
        ).apply {
            start()
        }
    }

    private val onConnected: (BluetoothSocket) -> Unit = {
        initConnectionManager(it)
    }

    fun initConnectionManager(socket: BluetoothSocket) {
        logger.debug("Init ConnectionManager")
        connectionManager = ConnectionManager(socket, handler)
    }

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constant.MSG_READ -> {
                    val bytes = msg.obj as ByteArray
                    val numBytes = msg.arg1
                    val message = bytes.decodeToString(0, numBytes)

                    logger.debug("Received message: $message")

                    connectionManager.write("I received your message: $message")
                }
                Constant.MSG_WRITE -> {
                    val bytes = msg.obj as ByteArray
                    val message = bytes.toString(Charset.forName("utf-8"))

                    logger.debug("Message sent: $message")
                }
                Constant.MSG_ERROR -> {
                    val error = msg.obj as String
                    logger.error(error)

                    onConnectionError()
                }
            }
        }
    }

    private fun onConnectionError() {
        logger.debug("Stop service")
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
