package com.phoniq.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.ui.theme.PhoniqTextSubtle
import java.util.Locale

@Composable
fun MockupSectionLabel(text: String, topPadding: Dp = 12.dp, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = topPadding, end = 16.dp, bottom = 6.dp),
        color = PhoniqTextSubtle,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
    )
}
