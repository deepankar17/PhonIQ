package com.phoniq.app.ui.phone

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.phoniq.app.R
import com.phoniq.app.data.model.QuickCallEntry
import com.phoniq.app.ui.components.AvatarInitialsText
import com.phoniq.app.ui.components.ContactPhotoAvatar
import com.phoniq.app.ui.theme.PhoniqTextSubtle

/** Peek ~½ of the 5th frequent-contact tile: `4½` tiles + four `spacedBy` gaps fit in horizontal inset. */
private const val QUICK_STRIP_VISIBLE_TILE_SLOTS = 4.5f
private val QuickStripSpacing = 8.dp
private val QuickStripAvatarSize = 56.dp
private val QuickStripCornerRadius = 18.dp

@Composable
fun QuickCallStrip(
    entries: List<QuickCallEntry>,
    onQuickCall: (QuickCallEntry) -> Unit,
) {
    val stripCd = stringResource(R.string.cd_frequent_quick_call_strip)
    val density = LocalDensity.current
    val padH = 16.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val innerWidthDp = maxWidth - padH * 2
        val tileWidth =
            remember(innerWidthDp, density) {
                val innerPx = with(density) { innerWidthDp.toPx() }
                val gapPx = with(density) { QuickStripSpacing.toPx() }
                val numerator = innerPx - 4 * gapPx
                val denom = QUICK_STRIP_VISIBLE_TILE_SLOTS
                val wPx =
                    ((if (numerator > 0f) numerator else innerPx.coerceAtLeast(1f)) / denom)
                        .coerceAtLeast(with(density) { 48.dp.toPx() })
                with(density) { wPx.toDp() }
            }

        LazyRow(
            modifier = Modifier.semantics { contentDescription = stripCd },
            contentPadding = PaddingValues(start = padH, top = 8.dp, end = padH, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(QuickStripSpacing),
        ) {
            items(entries, key = { it.id }) { entry ->
                QuickCallColumn(
                    entry = entry,
                    tileWidth = tileWidth,
                    onClick = { onQuickCall(entry) },
                )
            }
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
    tileWidth: Dp,
    onClick: () -> Unit,
) {
    val start = Color(entry.avatarColorArgb.toInt())
    val end =
        if (entry.avatarEndArgb != 0L) {
            Color(entry.avatarEndArgb.toInt())
        } else {
            darkenForAvatar(start)
        }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "quickCallPress")

    Column(
        modifier =
            Modifier
                .width(tileWidth)
                .scale(scale)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(QuickStripAvatarSize)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(QuickStripCornerRadius),
                        spotColor = Color.Black.copy(alpha = 0.24f),
                        ambientColor = Color.Black.copy(alpha = 0.10f),
                    )
                    .clip(RoundedCornerShape(QuickStripCornerRadius)),
            contentAlignment = Alignment.Center,
        ) {
            if (entry.deviceContactId > 0L) {
                ContactPhotoAvatar(
                    deviceContactId = entry.deviceContactId,
                    initials = entry.initial,
                    gradientStart = start,
                    gradientEnd = end,
                    size = QuickStripAvatarSize,
                    fontSize = 18.sp,
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(start, end))),
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarInitialsText(
                        text = entry.initial,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
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
            text =
                if (entry.quickTimes > 0) {
                    val lanes = stringArrayResource(R.array.quick_call_lanes)
                    val lane = lanes[entry.quickLaneIndex % lanes.size]
                    stringResource(R.string.quick_call_meta_format, lane, "${entry.quickTimes}x")
                } else {
                    entry.meta
                },
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
