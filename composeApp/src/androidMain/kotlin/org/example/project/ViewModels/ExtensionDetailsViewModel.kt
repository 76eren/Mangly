package org.example.project.ViewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.example.project.Composables.Standard.CardData

class ExtensionDetailsViewModel : ViewModel() {
    private val cards = mutableStateListOf<CardData>()

    var selectedCardData by mutableStateOf<CardData?>(null)
        private set

    fun setCards(data: List<CardData>) {
        cards.clear()
        cards.addAll(data)
    }

    fun selectCardByName(id: String) {
        selectedCardData = cards.find { it.metadata.name == id }
    }
}