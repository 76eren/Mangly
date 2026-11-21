package com.eren76.mangly.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.eren76.mangly.composables.screens.CardData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtensionDetailsViewModel @Inject constructor() : ViewModel() {
    private val allSourceCards = mutableStateListOf<CardData>()

    var selectedCardData by mutableStateOf<CardData?>(null)
        private set

    fun setAllSourceCards(allSourceCards: List<CardData>) {
        this.allSourceCards.clear()
        this.allSourceCards.addAll(allSourceCards)
    }

    fun selectCardBySource(id: String) {
        selectedCardData = allSourceCards.find { it.metadata.extensionId == id }
    }
}