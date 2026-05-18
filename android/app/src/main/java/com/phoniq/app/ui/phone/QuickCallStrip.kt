package com.phoniq.app.ui.phone

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.res.stringResource
import com.phoniq.app.R
import com.phoniq.app.data.model.QuickCallEntry
import com.phoniq.app.ui.components.AvatarInitialsText
import com.phoniq.app.ui.components.ContactPhotoAvatar
import com.phoniq.app.ui.components.contactAvatarClip
import com.phoniq.app.ui.components.contactAvatarShapeForSize
/** Peek ~½ of the 5th frequent-contact tile: `4½` tiles + four `spacedBy` gaps fit in horizontal inset. */
private const val QUICK_STRIP_VISIBLE_TILE_SLOTS = 4.5f
private val QuickStripSpacing = 8.dp
private val QuickStripAvatarSize = 56.dp

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
            contentPadding = PaddingValues(start = padH, top = 6.dp, end = padH, bottom = 4.dp),
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
            modifier = Modifier.size(QuickStripAvatarSize),
            contentAlignment = Alignment.Center,
        ) {
            val avDp = QuickStripAvatarSize
            val avShape = contactAvatarShapeForSize(avDp)
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .shadow(
                            elevation = 2.dp,
                            shape = avShape,
                            spotColor = Color.Black.copy(alpha = 0.14f),
                            ambientColor = Color.Black.copy(alpha = 0.06f),
                        )
                        .contactAvatarClip(avDp),
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
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            when {
                entry.quickTimes > 0 ->
                    Surface(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 3.dp, y = 3.dp)
                                .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        border =
                            BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                            ),
                        shadowElevation = 1.dp,
                    ) {
                        Text(
                            text = "${entry.quickTimes}",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                entry.meta.isNotBlank() ->
                    Surface(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 5.dp)
                                .widthIn(max = QuickStripAvatarSize + 12.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        border =
                            BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                            ),
                        shadowElevation = 1.dp,
                    ) {
                        Text(
                            text = entry.meta,
                            modifier =
                                Modifier
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                    .fillMaxWidth(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
            }
        }
        Text(
            text = entry.name,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        if (entry.subtitle.isNotBlank()) {
            Text(
                text = entry.subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
