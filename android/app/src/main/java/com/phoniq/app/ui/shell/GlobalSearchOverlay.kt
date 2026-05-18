package com.phoniq.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.ui.components.AvatarInitialsText
import com.phoniq.app.ui.components.MockupSectionLabel
import com.phoniq.app.ui.components.contactAvatarClip
import com.phoniq.app.ui.theme.LocalRcsBadgesEnabled
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBackground
import com.phoniq.app.ui.theme.PhoniqBorderSoft
import com.phoniq.app.ui.theme.PhoniqDebit
import com.phoniq.app.ui.theme.PhoniqLegendMuted
import com.phoniq.app.ui.theme.PhoniqOnBackground
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock

private data class SearchHit(
    val title: String,
    val subtitle: String,
    val kind: String,
    val timeLabel: String? = null,
    val showRcsPill: Boolean = false,
    val moneyEmoji: String? = null,
    val moneyAmount: String? = null,
    val avatarStartArgb: Long,
    val avatarEndArgb: Long,
    /** [MessageThread.id] when [kind] is Messages. */
    val messageThreadId: String? = null,
    /** PSTN digits / formatted number when [kind] is Calls. */
    val dialNumber: String? = null,
)

@Composable
fun GlobalSearchOverlay(
    onDismiss: () -> Unit,
    recentCalls: List<com.phoniq.app.data.model.RecentCall> = emptyList(),
    messageThreads: List<com.phoniq.app.data.model.MessageThread> = emptyList(),
    moneyTransactions: List<com.phoniq.app.data.model.RecentTransaction> = emptyList(),
    onDialNumber: (String) -> Unit = {},
    onOpenMessageThread: (String) -> Unit = {},
    onGoToMoney: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val baseHits =
        remember(recentCalls, messageThreads, moneyTransactions) {
            buildSearchHits(recentCalls, messageThreads, moneyTransactions)
        }
    val hits =
        remember(query, baseHits) {
            baseHits.filter {
                query.isBlank() ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.subtitle.contains(query, ignoreCase = true)
            }
        }
    val sectionOrder = listOf("Calls", "Messages", "Money")
    val grouped =
        remember(hits) {
            sectionOrder.mapNotNull { key ->
                val inSection = hits.filter { it.kind == key }
                if (inSection.isEmpty()) null else key to inSection
            }
        }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = PhoniqBackground) {
            Column(Modifier.fillMaxSize()) {
                Row(
                modifier =
                    Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val stroke = 1.dp.toPx()
                                drawLine(
                                    PhoniqBorderSoft,
                                    Offset(0f, size.height - stroke / 2),
                                    Offset(size.width, size.height - stroke / 2),
                                    strokeWidth = stroke,
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.padding(0.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = PhoniqTextSecondaryMock,
                        )
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                stringResource(R.string.search_overlay_input_hint),
                                color = PhoniqTextSecondaryMock,
                                fontSize = 14.sp,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.cd_search),
                                tint = PhoniqTextSecondaryMock,
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = PhoniqSurface,
                                unfocusedContainerColor = PhoniqSurface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = PhoniqOnBackground,
                                unfocusedTextColor = PhoniqOnBackground,
                                cursorColor = PhoniqAccent,
                            ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    )
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (hits.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.search_overlay_empty),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = PhoniqTextSecondaryMock,
                            )
                        }
                    } else {
                        grouped.forEach { (sectionKey, rows) ->
                            item(key = "hdr_$sectionKey") {
                                MockupSectionLabel(
                                    text = sectionTitle(sectionKey),
                                    topPadding = 4.dp,
                                )
                            }
                            items(
                                items = rows,
                                key = { "${sectionKey}_${it.title}_${it.subtitle}" },
                            ) { hit ->
                                Column(Modifier.fillMaxWidth()) {
                                    when (hit.kind) {
                                        "Money" -> SearchMoneyRow(hit, onClick = onGoToMoney)
                                        "Messages" ->
                                            SearchMessageRow(
                                                hit,
                                                onClick = {
                                                    hit.messageThreadId?.let(onOpenMessageThread)
                                                },
                                            )
                                        else ->
                                            SearchCallRow(
                                                hit,
                                                onClick = {
                                                    hit.dialNumber?.let(onDialNumber)
                                                },
                                            )
                                    }
                                    HorizontalDivider(color = PhoniqBorderSoft.copy(alpha = 0.85f), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun sectionTitle(kind: String): String {
    return when (kind) {
        "Calls" -> stringResource(R.string.search_section_calls)
        "Messages" -> stringResource(R.string.search_section_messages)
        "Money" -> stringResource(R.string.search_section_money)
        else -> kind
    }
}

@Composable
private fun SearchCallRow(
    hit: SearchHit,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val g0 = Color(hit.avatarStartArgb.toInt())
        val g1 = Color(hit.avatarEndArgb.toInt())
        val initial = hit.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .contactAvatarClip(56.dp)
                    .background(Brush.linearGradient(listOf(g0, g1))),
            contentAlignment = Alignment.Center,
        ) {
            AvatarInitialsText(
                text = initial,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(hit.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = PhoniqOnBackground)
            Text(
                hit.subtitle,
                fontSize = 12.sp,
                color = PhoniqTextSecondaryMock,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SearchMessageRow(
    hit: SearchHit,
    onClick: () -> Unit,
) {
    val rcsBadgesOn = LocalRcsBadgesEnabled.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val g0 = Color(hit.avatarStartArgb.toInt())
        val g1 = Color(hit.avatarEndArgb.toInt())
        val initial = hit.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .contactAvatarClip(56.dp)
                    .background(Brush.linearGradient(listOf(g0, g1))),
            contentAlignment = Alignment.Center,
        ) {
            AvatarInitialsText(
                text = initial,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(hit.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PhoniqOnBackground)
                    if (hit.showRcsPill && rcsBadgesOn) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PhoniqSecondary.copy(alpha = 0.2f),
                        ) {
                            Text(
                                "RCS",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PhoniqSecondary,
                            )
                        }
                    }
                }
                if (hit.timeLabel != null) {
                    Text(hit.timeLabel, fontSize = 11.sp, color = PhoniqLegendMuted)
                }
            }
            Text(
                hit.subtitle,
                fontSize = 12.sp,
                color = PhoniqTextSecondaryMock,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SearchMoneyRow(
    hit: SearchHit,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PhoniqAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(hit.moneyEmoji ?: "₹", fontSize = 18.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(hit.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PhoniqOnBackground)
            Text(
                hit.subtitle,
                fontSize = 12.sp,
                color = PhoniqTextSecondaryMock,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            hit.moneyAmount ?: "",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = PhoniqDebit,
        )
    }
}

private fun buildSearchHits(
    recentCalls: List<com.phoniq.app.data.model.RecentCall>,
    messageThreads: List<com.phoniq.app.data.model.MessageThread>,
    moneyTransactions: List<com.phoniq.app.data.model.RecentTransaction>,
): List<SearchHit> {
    val calls =
        recentCalls.take(40).map { c ->
            SearchHit(
                title = c.contactName,
                subtitle = c.numberOrLabel,
                kind = "Calls",
                timeLabel = c.timeLabel,
                avatarStartArgb = c.avatarStartArgb,
                avatarEndArgb = c.avatarEndArgb,
                dialNumber = c.numberOrLabel,
            )
        }
    val msgs =
        messageThreads.take(40).map { t ->
            SearchHit(
                title = t.title,
                subtitle = t.snippet,
                kind = "Messages",
                timeLabel = t.timeLabel,
                showRcsPill = t.showRcsBadge,
                avatarStartArgb = t.avatarStartArgb,
                avatarEndArgb = t.avatarEndArgb,
                messageThreadId = t.id,
            )
        }
    val money =
        moneyTransactions.take(40).map { x ->
            SearchHit(
                title = x.merchant,
                subtitle = x.dateLine,
                kind = "Money",
                moneyEmoji = x.emoji,
                moneyAmount = x.amountLabel,
                avatarStartArgb = 0xFF6C63FFL,
                avatarEndArgb = 0xFF4A43CCL,
            )
        }
    return calls + msgs + money
}
