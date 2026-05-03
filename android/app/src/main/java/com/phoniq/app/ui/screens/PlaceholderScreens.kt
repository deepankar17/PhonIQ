package com.phoniq.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phoniq.app.R

@Composable
fun PhonePlaceholderScreen() {
    PlaceholderBody(text = stringResource(R.string.placeholder_phone))
}

@Composable
fun MessagesPlaceholderScreen() {
    PlaceholderBody(text = stringResource(R.string.placeholder_messages))
}

@Composable
fun MoneyPlaceholderScreen() {
    PlaceholderBody(text = stringResource(R.string.placeholder_money))
}

@Composable
private fun PlaceholderBody(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}
