package com.phoniq.app.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.data.SampleData
import com.phoniq.app.data.model.QuickCallEntry
import com.phoniq.app.ui.theme.PhoniqTextSubtle

@Composable
fun QuickCallStrip(onQuickCall: (QuickCallEntry) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(SampleData.quickCalls, key = { it.id }) { entry ->
            QuickCallColumn(entry = entry, onClick = { onQuickCall(entry) })
        }
    }
}

private fun darkenForAvatar(c: Color): Color =
    Color(
        red = (c.red * 0.65f).coerceIn(0f, 1f),
        green = (c.green * 0.65f).coerceIn(0f, 1f),
        blue = (c.blue * 0.65f).coerceIn(0f, 1f),
        alpha = c.alpha,
    )

@Composable
private fun QuickCallColumn(
    entry: QuickCallEntry,
    onClick: () -> Unit,
) {
    val start = Color(entry.avatarColorArgb.toInt())
    val end = darkenForAvatar(start)
    Column(
        modifier =
            Modifier
                .width(61.dp)
                .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(start, end))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = entry.initial,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = entry.name,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = entry.meta,
            fontSize = 9.sp,
            lineHeight = 9.9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = PhoniqTextSubtle,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
