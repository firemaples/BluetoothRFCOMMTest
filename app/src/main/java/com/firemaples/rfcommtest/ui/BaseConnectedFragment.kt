package com.firemaples.rfcommtest.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.firemaples.rfcommtest.R
import com.firemaples.rfcommtest.bluetooth.ConnectionManager
import com.firemaples.rfcommtest.ui.client.ClientService
import com.firemaples.rfcommtest.utility.Constant
import com.firemaples.rfcommtest.utility.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

open class BaseConnectedFragment(@LayoutRes layoutId: Int) : Fragment(layoutId) {
    private val logger: Logger = Logger(BaseConnectedFragment::class)

    private lateinit var tvLog: TextView

    private lateinit var connectionManager: ConnectionManager

    private val dateFormat = SimpleDateFormat("HH-mm-ss.SSS", Locale.US)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireContext().registerReceiver(
            broadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        )

        setViews(view)
    }

    override fun onDestroy() {
        super.onDestroy()

        requireContext().unregisterReceiver(broadcastReceiver)
    }

    private fun setViews(view: View) {
        val etMessage: EditText = view.findViewById(R.id.et_message)
        tvLog = view.findViewById(R.id.tv_log)

        view.findViewById<View>(R.id.bt_send).setOnClickListener {
            val msg = etMessage.text.toString()
            if (msg.isEmpty()) return@setOnClickListener
            etMessage.text = null

            if (!::connectionManager.isInitialized) {
                if (Constant.USE_CLIENT_SERVICE) {
                    ClientService.sendMessage(requireContext(), msg)
                } else {
                    printLog("Connection is not established")
                }
                return@setOnClickListener
            }

            connectionManager.write(msg)
            printLog("Message [$msg] sent")
        }

        Logger.callback = { log ->
            CoroutineScope(Dispatchers.Main).launch {
                printLog(log)
            }
        }
    }

    private fun printLog(log: String) {
        val text = "${dateFormat.format(System.currentTimeMillis())}\n$log\n\n${tvLog.text}"
        tvLog.text = text
    }

    fun initConnectionManager(socket: BluetoothSocket) {
        logger.debug("Init ConnectionManager")
        connectionManager = ConnectionManager(socket, handler)
    }

    open fun onConnectionError() {

    }

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constant.MSG_READ -> {
                    val bytes = msg.obj as ByteArray
                    val numBytes = msg.arg1
                    val message = bytes.decodeToString(0, numBytes)

                    logger.debug("Received message: $message")
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

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val scanMode = intent?.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)
            val previousScanMode = intent?.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1)

            when (scanMode) {
                BluetoothAdapter.SCAN_MODE_CONNECTABLE -> {
                    logger.debug("On scan mode changed: connectable")
                }
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> {
                    logger.debug("On scan mode changed: connectable and discoverable")
                }
                BluetoothAdapter.SCAN_MODE_NONE -> {
                    logger.debug("On scan mode changed: none")
                }
            }
        }
    }
}