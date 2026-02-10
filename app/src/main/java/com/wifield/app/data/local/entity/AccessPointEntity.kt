package com.wifield.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "access_points",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("snapshotId")]
)
data class AccessPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val snapshotId: Long,
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val channel: Int,
    val frequency: Int,
    val band: String,  // "2.4 GHz", "5 GHz", "6 GHz"
    val channelWidth: Int, // 20, 40, 80, 160 MHz
    val security: String,  // "Open", "WEP", "WPA2", "WPA3", etc.
    val wpsEnabled: Boolean = false,
    val isConnected: Boolean = false
)
