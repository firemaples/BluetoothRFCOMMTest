package com.firemaples.rfcommtest.ui.server

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.firemaples.rfcommtest.R
import com.firemaples.rfcommtest.bluetooth.AcceptThread
import com.firemaples.rfcommtest.ui.BaseConnectedFragment
import com.firemaples.rfcommtest.utility.Constant
import com.firemaples.rfcommtest.utility.Logger

class ServerFragment : BaseConnectedFragment(R.layout.fragment_server) {
    private val logger = Logger(this::class)

    private lateinit var acceptThread: AcceptThread

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(view)
    }

    private fun setViews(view: View) {
        view.findViewById<View>(R.id.bt_requestDiscoverability).setOnClickListener {
            enableDiscoverability()
        }

        view.findViewById<View>(R.id.bt_startServer).setOnClickListener {
            startServer()
        }
    }

    private fun enableDiscoverability() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }

        startForResult.launch(intent)
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode != Activity.RESULT_CANCELED) {
            logger.debug("Start discoverability for ${it.resultCode} seconds")
        }
    }

    private fun startServer() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            try {
                acceptThread =
                    AcceptThread(
                        bluetoothAdapter = adapter,
                        displayName = "RFCOMM server",
                        uuid = Constant.uuid,
                        onConnected = onConnected
                    ).apply {
                        start()
                    }
            } catch (e: Exception) {
                logger.error(e = e)
            }
        }
    }

    private val onConnected: (BluetoothSocket) -> Unit = {
        initConnectionManager(it)
    }

    override fun onConnectionError() {
        super.onConnectionError()

        logger.debug("Restart accept thread")

        startServer()
    }
}
