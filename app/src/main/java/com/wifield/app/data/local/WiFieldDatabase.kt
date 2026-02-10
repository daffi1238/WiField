package com.wifield.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wifield.app.data.local.dao.AccessPointDao
import com.wifield.app.data.local.dao.ActiveTestResultDao
import com.wifield.app.data.local.dao.ProjectDao
import com.wifield.app.data.local.dao.SnapshotDao
import com.wifield.app.data.local.entity.AccessPointEntity
import com.wifield.app.data.local.entity.ActiveTestResultEntity
import com.wifield.app.data.local.entity.ProjectEntity
import com.wifield.app.data.local.entity.SnapshotEntity

@Database(
    entities = [
        ProjectEntity::class,
        SnapshotEntity::class,
        AccessPointEntity::class,
        ActiveTestResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WiFieldDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun accessPointDao(): AccessPointDao
    abstract fun activeTestResultDao(): ActiveTestResultDao

    companion object {
        @Volatile
        private var INSTANCE: WiFieldDatabase? = null

        fun getInstance(context: Context): WiFieldDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WiFieldDatabase::class.java,
                    "wifield_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
