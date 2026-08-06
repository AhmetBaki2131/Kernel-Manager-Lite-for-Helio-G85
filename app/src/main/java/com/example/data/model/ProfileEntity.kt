package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val isBuiltIn: Boolean = false,
    val cpuMinFreqKHz: Long = 0L,
    val cpuMaxFreqKHz: Long = 0L,
    val cpuGovernor: String = "schedutil",
    val gpuFreqHz: Long = 0L,
    val gpuGovernor: String = "msm-adreno-tz",
    val zramSizeMb: Long = 2048L,
    val ioScheduler: String = "mq-deadline",
    val applyOnBoot: Boolean = false
)
