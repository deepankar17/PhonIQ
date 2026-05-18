package com.phoniq.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.ui.theme.PhoniqTextSubtle
import java.util.Locale

@Composable
fun MockupSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = 12.dp,
    bottomPadding: Dp = 6.dp,
    letterSpacing: TextUnit = 1.2.sp,
    fontSize: TextUnit = 11.sp,
    color: Color? = null,
) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = topPadding, end = 16.dp, bottom = bottomPadding),
        color = color ?: PhoniqTextSubtle,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = letterSpacing,
    )
}
