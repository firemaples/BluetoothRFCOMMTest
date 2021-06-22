package com.firemaples.rfcommtest.bluetooth

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import com.firemaples.rfcommtest.utility.Constant
import com.firemaples.rfcommtest.utility.Logger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

class ConnectionManager(socket: BluetoothSocket, private val handler: Handler) {
    private val logger: Logger = Logger(this::class)

    private val connectedThread: ConnectedThread = ConnectedThread(socket)

    init {
        connectedThread.start()
    }

    fun write(msg: String){
        write(msg.toByteArray(Charset.forName("utf-8")))
    }

    fun write(bytes: ByteArray) {
        connectedThread.write(bytes)
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread("bt-connected-thread") {

        private val inStream: InputStream = socket.inputStream
        private val outStream: OutputStream = socket.outputStream
        private val buffer: ByteArray = ByteArray(1024)

        override fun run() {
            var numBytes: Int

            while (true) {
                numBytes = try {
                    logger.debug("Waiting for the next message")
                    inStream.read(buffer)
                } catch (e: IOException) {
                    logger.error("Read message from the input stream failed", e = e)
                    handler.obtainMessage(Constant.MSG_ERROR).apply {
                        obj = "Read message failed: ${Log.getStackTraceString(e)}"
                    }.sendToTarget()

                    break
                }

                handler.obtainMessage(Constant.MSG_READ, numBytes, -1, buffer).sendToTarget()
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outStream.write(bytes)
            } catch (e: IOException) {
                logger.error("Send message failed", e = e)
                handler.obtainMessage(Constant.MSG_ERROR).apply {
                    obj = "Send message failed: ${Log.getStackTraceString(e)}"
                }.sendToTarget()

                return
            }

            handler.obtainMessage(Constant.MSG_WRITE, -1, -1, bytes).sendToTarget()
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: Exception) {
                logger.error("Could not close the connected socket", e = e)
            }
        }
    }
}
