package com.eren76.mangly.composables.shared.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun BatteryOptimizationPromptDialog(
    visible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Improve background downloads")
        },
        text = {
            Text(
                "Android may pause long downloads while Mangly is in the background. " +
                        "Open settings to disable battery optimization for more reliable downloads. " +
                        "You can skip this and downloads will keep using the current fallback."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        }
    )
}
