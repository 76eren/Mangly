package org.example.project.Composables.Standard.ExtensionDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import org.example.project.Composables.Shared.Topbar.TopBarDeleteExtensionFromExtensionDetails
import org.example.project.Composables.Standard.CardData

@Composable
fun ExtensionDetails(cardData: CardData) {
    Scaffold (topBar = { TopBarDeleteExtensionFromExtensionDetails() }
    ) { paddingValues ->

        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            Text(text = "Source is: ${cardData.title}")

        }


    }

}