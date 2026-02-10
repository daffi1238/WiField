package com.wifield.app

import android.app.Application
import com.wifield.app.data.local.WiFieldDatabase

class WiFieldApplication : Application() {
    val database: WiFieldDatabase by lazy {
        WiFieldDatabase.getInstance(this)
    }
}
