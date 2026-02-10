package com.wifield.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "active_test_results",
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
data class ActiveTestResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val snapshotId: Long,
    val downloadSpeed: Double = 0.0,   // Mbps
    val uploadSpeed: Double = 0.0,     // Mbps
    val latency: Double = 0.0,         // ms
    val jitter: Double = 0.0,          // ms
    val packetLoss: Double = 0.0,      // percentage
    val linkSpeed: Int = 0,            // Mbps
    val gatewayLatency: Double = 0.0   // ms
)
