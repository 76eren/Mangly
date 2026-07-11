package com.eren76.mangly.composables.screens.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eren76.mangly.Constants

@Composable
fun rememberHomeSorting(context: Context): HomeSorting {
    val sortingPref = remember(context) {
        context.getSharedPreferences(
            Constants.HOME_SETTING_KEY,
            Context.MODE_PRIVATE
        ).getString(
            Constants.HOME_SORTING_SETTING_KEY,
            HomeSorting.DEFAULT_PREF_VALUE
        )
    }

    return remember(sortingPref) {
        HomeSorting.fromPrefValue(sortingPref)
    }
}
