package com.eren76.mangly.workers

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.eren76.mangly.permissions.BatteryOptimizationPermissionHandling

internal object ChapterDownloadWorkerForegroundPolicy {
    fun canStartForegroundExecution(context: Context): Boolean {
        return isAppInForeground(context) ||
                BatteryOptimizationPermissionHandling.isIgnoringBatteryOptimizations(context)
    }

    fun isForegroundServiceStartDenied(error: Throwable): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false

        return error.hasCauseNamed("android.app.ForegroundServiceStartNotAllowedException")
    }

    private fun isAppInForeground(context: Context): Boolean {
        val activityManager =
            context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return false
        val packageName = context.packageName

        return activityManager.runningAppProcesses.orEmpty().any { processInfo ->
            val foregroundImportance =
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND

            processInfo.processName == packageName &&
                    processInfo.importance <= foregroundImportance
        }
    }

    private fun Throwable.hasCauseNamed(className: String): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current.javaClass.name == className) return true
            current = current.cause
        }
        return false
    }
}
