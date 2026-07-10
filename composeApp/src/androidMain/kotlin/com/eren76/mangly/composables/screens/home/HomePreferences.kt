package com.eren76.mangly.composables.screens.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eren76.mangly.Constants

data class HomeDisplayPreferences(
    val sorting: HomeSorting,
    val isPaginationEnabled: Boolean,
    val pageSize: Int
)

@Composable
fun rememberHomeDisplayPreferences(context: Context): HomeDisplayPreferences {
    val sortingPref = remember(context) {
        context.getSharedPreferences(
            Constants.HOME_SETTING_KEY,
            Context.MODE_PRIVATE
        ).getString(
            Constants.HOME_SORTING_SETTING_KEY,
            HomeSorting.DEFAULT_PREF_VALUE
        )
    }

    val paginationPreferences = remember(context) {
        context.getSharedPreferences(
            Constants.PAGINATION_SETTINGS_KEY,
            Context.MODE_PRIVATE
        )
    }

    val sorting = remember(sortingPref) {
        HomeSorting.fromPrefValue(sortingPref)
    }

    val isPaginationEnabled: Boolean =
        paginationPreferences.getBoolean(Constants.PAGINATION_ENABLED_KEY, false)
    val pageSize: Int = paginationPreferences.getInt(
        Constants.MANGLY_PAGINATION_SIZE_KEY.toString(),
        Constants.MANGLY_PAGINATION_SIZE_KEY
    )

    return HomeDisplayPreferences(
        sorting = sorting,
        isPaginationEnabled = isPaginationEnabled,
        pageSize = pageSize
    )
}
