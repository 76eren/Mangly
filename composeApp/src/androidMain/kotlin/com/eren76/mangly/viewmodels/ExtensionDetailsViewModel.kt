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
    private val cards = mutableStateListOf<CardData>()

    var selectedCardData by mutableStateOf<CardData?>(null)
        private set

    fun setCards(data: List<CardData>) {
        cards.clear()
        cards.addAll(data)
    }

    fun selectCardBySource(id: String) {
        selectedCardData = cards.find { it.metadata.source.getExtensionId() == id }
    }
}