package com.firemaples.rfcommtest.utility

import android.os.Build
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

    /**
     * https://developer.android.com/guide/topics/connectivity/companion-device-pairing
     */
    private const val ENABLE_COMPANION_MANAGER: Boolean = true
    val useCompanionManager: Boolean
        get() {
            return ENABLE_COMPANION_MANAGER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        }

    const val USE_CLIENT_SERVICE = true
}
