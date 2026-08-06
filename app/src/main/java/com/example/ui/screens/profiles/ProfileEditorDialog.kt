package com.example.ui.screens.profiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorDialog(
    initialProfile: ProfileEntity,
    onSave: (ProfileEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProfile.name) }
    var description by remember { mutableStateOf(initialProfile.description) }
    var cpuGovernor by remember { mutableStateOf(initialProfile.cpuGovernor) }
    var gpuGovernor by remember { mutableStateOf(initialProfile.gpuGovernor) }
    var zramSizeMbStr by remember { mutableStateOf(initialProfile.zramSizeMb.toString()) }
    var ioScheduler by remember { mutableStateOf(initialProfile.ioScheduler) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProfile.id == 0L) "Create Profile" else "Edit Profile") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_desc_input")
                )

                OutlinedTextField(
                    value = cpuGovernor,
                    onValueChange = { cpuGovernor = it },
                    label = { Text("CPU Governor (e.g. schedutil, performance, powersave)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_cpugov_input")
                )

                OutlinedTextField(
                    value = gpuGovernor,
                    onValueChange = { gpuGovernor = it },
                    label = { Text("GPU Governor (e.g. msm-adreno-tz, performance)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_gpugov_input")
                )

                OutlinedTextField(
                    value = zramSizeMbStr,
                    onValueChange = { zramSizeMbStr = it },
                    label = { Text("ZRAM Size (MB)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_zram_input")
                )

                OutlinedTextField(
                    value = ioScheduler,
                    onValueChange = { ioScheduler = it },
                    label = { Text("I/O Scheduler (e.g. mq-deadline, bfq, kyber)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_iosched_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val zram = zramSizeMbStr.toLongOrNull() ?: 2048L
                    val profileToSave = initialProfile.copy(
                        name = name.ifBlank { "Custom Profile" },
                        description = description,
                        cpuGovernor = cpuGovernor,
                        gpuGovernor = gpuGovernor,
                        zramSizeMb = zram,
                        ioScheduler = ioScheduler
                    )
                    onSave(profileToSave)
                },
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_profile_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
