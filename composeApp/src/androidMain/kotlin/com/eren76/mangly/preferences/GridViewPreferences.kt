package com.eren76.mangly.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.shared.collection.GridViewMode

@Composable
fun rememberGridViewMode(context: Context): GridViewMode {
    val preferences = remember(context) {
        context.getSharedPreferences(
            Constants.PAGINATION_SETTINGS_KEY,
            Context.MODE_PRIVATE
        )
    }
    val isPaginationEnabled = preferences.getBoolean(
        Constants.PAGINATION_ENABLED_KEY,
        false
    )
    val pageSize = preferences.getInt(
        Constants.MANGLY_PAGINATION_SIZE_KEY.toString(),
        Constants.MANGLY_PAGINATION_SIZE_KEY
    ).coerceAtLeast(1)

    return remember(isPaginationEnabled, pageSize) {
        if (isPaginationEnabled) {
            GridViewMode.Paginated(pageSize = pageSize)
        } else {
            GridViewMode.Scrolling
        }
    }
}
