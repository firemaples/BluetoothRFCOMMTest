package com.firemaples.rfcommtest.utility

import android.os.ParcelUuid
import java.util.*

/**
 * https://developer.android.com/guide/topics/connectivity/bluetooth
 */
object Constant {
    private const val uuidString: String = "c15acdba-d32c-11eb-b8bc-0242ac130003"
    val uuid: UUID by lazy { UUID.fromString(uuidString) }
    val parcelUuid: ParcelUuid by lazy { ParcelUuid(uuid) }

    const val MSG_READ: Int = 0
    const val MSG_WRITE: Int = 1
    const val MSG_ERROR: Int = 2

    const val USE_CLIENT_SERVICE = true

    const val PAIR_DEVICE_WITH_NAME = false
    const val PAIRING_DEVICE_NAME = "Note4"
}
