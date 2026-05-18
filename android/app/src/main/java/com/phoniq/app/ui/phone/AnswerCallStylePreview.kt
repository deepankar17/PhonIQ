package com.phoniq.app.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.util.PersonalizationStore
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight

/**
 * Compact preview for Settings: answer / decline control family (classic pills, glass slide, Samsung liquid).
 */
@Composable
fun AnswerCallStylePreview(
    answerCallStyle: String,
    modifier: Modifier = Modifier,
) {
    val bg =
        Brush.verticalGradient(
            listOf(Color(0xFF121822), Color(0xFF0A0E14)),
        )
    Box(
        modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (answerCallStyle) {
            PersonalizationStore.ANSWER_STYLE_GLASS -> GlassAnswerPreviewRow()
            PersonalizationStore.ANSWER_STYLE_SAMSUNG_LIQUID -> SamsungLiquidAnswerPreviewRow()
            else -> ClassicAnswerPreviewRow()
        }
    }
}

@Composable
private fun ClassicAnswerPreviewRow() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A3440))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(28.dp))
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF3DDC84), Color(0xFF00A854)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun GlassAnswerPreviewRow() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(0.32f), Color.White.copy(0.1f), Color(0xFF6B8CFF).copy(0.12f)),
                        ),
                    )
                    .border(1.dp, Color.White.copy(0.38f), CircleShape),
            )
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(0.28f), Color.White.copy(0.09f), Color(0xFF6B8CFF).copy(0.1f)),
                        ),
                    )
                    .border(1.dp, Color.White.copy(0.35f), CircleShape),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(0.22f), Color.White.copy(0.12f), Color(0xFF88AAFF).copy(0.08f)),
                    ),
                )
                .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                stringResource(R.string.preview_slide_to_answer_short),
                color = Color.White.copy(0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().padding(start = 44.dp, end = 8.dp),
            )
            Box(
                Modifier
                    .padding(start = 3.dp)
                    .size(30.dp)
                    .offset(x = 18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34C759)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SamsungLiquidAnswerPreviewRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ColumnSquirclePreview()
        ColumnSquirclePreview()
    }
}

@Composable
private fun ColumnSquirclePreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF7A8FB0), Color(0xFF3D4A5E), Color(0xFF2A3444))),
                )
                .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(14.dp)),
        )
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFCCD4E0).copy(0.35f)),
        )
    }
}
