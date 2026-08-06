package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AiHistoryEntity
import com.example.data.model.BackupEntity
import com.example.data.model.LogEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.TerminalCommandEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProfileEntity::class,
        LogEntity::class,
        TerminalCommandEntity::class,
        AiHistoryEntity::class,
        BackupEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun logDao(): LogDao
    abstract fun terminalDao(): TerminalDao
    abstract fun aiHistoryDao(): AiHistoryDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kernel_manager_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialProfiles(database.profileDao())
                    }
                }
            }

            suspend fun populateInitialProfiles(profileDao: ProfileDao) {
                if (profileDao.getProfileCount() == 0) {
                    val gameMode = ProfileEntity(
                        name = "Game Mode",
                        description = "Maximum CPU & GPU performance governor for demanding gaming.",
                        isBuiltIn = true,
                        cpuGovernor = "performance",
                        gpuGovernor = "performance",
                        zramSizeMb = 3072L,
                        ioScheduler = "mq-deadline"
                    )

                    val balancedMode = ProfileEntity(
                        name = "Balanced Mode",
                        description = "Optimal balance between battery life and smooth daily responsiveness.",
                        isBuiltIn = true,
                        cpuGovernor = "schedutil",
                        gpuGovernor = "msm-adreno-tz",
                        zramSizeMb = 2048L,
                        ioScheduler = "mq-deadline"
                    )

                    val batterySaver = ProfileEntity(
                        name = "Battery Saver",
                        description = "Reduces CPU/GPU clocks and uses powersave governor to maximize uptime.",
                        isBuiltIn = true,
                        cpuGovernor = "powersave",
                        gpuGovernor = "powersave",
                        zramSizeMb = 1024L,
                        ioScheduler = "none"
                    )

                    profileDao.insertProfile(gameMode)
                    profileDao.insertProfile(balancedMode)
                    profileDao.insertProfile(batterySaver)
                }
            }
        }
    }
}
