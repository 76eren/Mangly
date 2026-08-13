package com.eren76.mangly.composables.shared.read

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ReadTopControls(
    currentPage: Int,
    totalPages: Int,
    chapterTitle: String,
    onPreviousChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = chapterTitle,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Page $currentPage / $totalPages",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onPreviousChapter,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Chapter"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Previous Chapter")
        }
    }
}

@Composable
fun ReadBottomControls(
    onNextChapter: () -> Unit,
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    onPagePreview: (Int) -> Unit = {},
    isPageSliderEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (totalPages > 1) {
            ReaderPageSlider(
                currentPage = currentPage,
                totalPages = totalPages,
                enabled = isPageSliderEnabled,
                onPageSelected = onPageSelected,
                onPagePreview = onPagePreview
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onNextChapter,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next Chapter")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next Chapter"
            )
        }
    }
}

@Composable
private fun ReaderPageSlider(
    currentPage: Int,
    totalPages: Int,
    enabled: Boolean,
    onPageSelected: (Int) -> Unit,
    onPagePreview: (Int) -> Unit
) {
    var sliderPosition by remember(currentPage) {
        mutableFloatStateOf(currentPage.toFloat())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PageSliderLabel(text = "1")

        Slider(
            value = sliderPosition,
            enabled = enabled,
            onValueChange = { newPosition ->
                val previousPage = sliderPosition.roundToInt()
                val previewPage = newPosition.roundToInt()

                sliderPosition = newPosition
                if (previewPage != previousPage) {
                    onPagePreview(previewPage)
                }
            },
            onValueChangeFinished = {
                onPageSelected(sliderPosition.roundToInt())
            },
            valueRange = 0f..(totalPages - 1).toFloat(),
            steps = (totalPages - 2).coerceAtLeast(0),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        )

        PageSliderLabel(text = totalPages.toString())
    }
}

@Composable
private fun PageSliderLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodySmall
    )
}
