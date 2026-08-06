package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.KernelRepository
import com.example.root.RootShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed broadcast received: $action")

            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val dataStore = SettingsDataStore(context)
                    val applyOnBoot = dataStore.applyOnBoot.first()

                    if (!applyOnBoot) {
                        Log.d("BootReceiver", "Apply on Boot is disabled. Skipping.")
                        return@launch
                    }

                    if (!RootShell.isRootAvailable()) {
                        Log.e("BootReceiver", "Root not granted on boot. Skipping profile re-application.")
                        return@launch
                    }

                    val database = AppDatabase.getDatabase(context, scope)
                    val selectedProfileId = dataStore.selectedProfileId.first()
                    val profile = database.profileDao().getProfileById(selectedProfileId)

                    if (profile != null) {
                        Log.d("BootReceiver", "Re-applying profile on boot: ${profile.name}")
                        val repository = KernelRepository(context, database.logDao(), database.profileDao())
                        repository.applyProfile(profile)
                    } else {
                        Log.w("BootReceiver", "Selected profile ID $selectedProfileId not found.")
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error in BootReceiver", e)
                }
            }
        }
    }
}
