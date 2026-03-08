package com.eren76.mangly

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


object PermissionHandling {

    fun canWriteWithoutPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}

class StoragePermissionState(
    val launch: () -> Unit
)

@Composable
fun rememberStoragePermissionHandler(
    onGranted: () -> Unit
): StoragePermissionState {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onGranted()
    }

    return remember(onGranted) {
        StoragePermissionState {
            if (PermissionHandling.canWriteWithoutPermission()) {
                onGranted()
            } else {
                launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
}

