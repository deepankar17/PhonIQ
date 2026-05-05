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
import androidx.compose.foundation.shape.CircleShape
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
import com.phoniq.app.ui.components.MockupSectionLabel
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
)

@Composable
fun GlobalSearchOverlay(onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val hits =
        remember(query) {
            sampleHits.filter {
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
                                contentDescription = null,
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
                                        "Money" -> SearchMoneyRow(hit)
                                        "Messages" -> SearchMessageRow(hit)
                                        else -> SearchCallRow(hit)
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
private fun SearchCallRow(hit: SearchHit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val g0 = Color(hit.avatarStartArgb.toInt())
        val g1 = Color(hit.avatarEndArgb.toInt())
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(g0, g1))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                hit.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
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
private fun SearchMessageRow(hit: SearchHit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val g0 = Color(hit.avatarStartArgb.toInt())
        val g1 = Color(hit.avatarEndArgb.toInt())
        Box(
            modifier =
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(g0, g1))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                hit.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
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
                    if (hit.showRcsPill) {
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
private fun SearchMoneyRow(hit: SearchHit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { }
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

private val sampleHits =
    listOf(
        SearchHit(
            title = "Priya Sharma",
            subtitle = "+91 98765 43210",
            kind = "Calls",
            avatarStartArgb = 0xFF8C5FE8L,
            avatarEndArgb = 0xFF6C63FFL,
        ),
        SearchHit(
            title = "HDFC Bank",
            subtitle = "1800 267 6161",
            kind = "Calls",
            avatarStartArgb = 0xFF1A6FD4L,
            avatarEndArgb = 0xFF0D4FA8L,
        ),
        SearchHit(
            title = "Rahul Verma",
            subtitle = "Outgoing video · Sun",
            kind = "Calls",
            avatarStartArgb = 0xFFE87D20L,
            avatarEndArgb = 0xFFC45A00L,
        ),
        SearchHit(
            title = "Priya Sharma",
            subtitle = "Are you coming to the office tomorrow?",
            kind = "Messages",
            timeLabel = "9:15",
            showRcsPill = true,
            avatarStartArgb = 0xFF8C5FE8L,
            avatarEndArgb = 0xFF6C63FFL,
        ),
        SearchHit(
            title = "HDFCBK",
            subtitle = "INR 2,450 debited at BLINKIT",
            kind = "Messages",
            avatarStartArgb = 0xFF1A6FD4L,
            avatarEndArgb = 0xFF0D4FA8L,
        ),
        SearchHit(
            title = "VM-VFSOTP",
            subtitle = "OTP 482910 · valid 3 min",
            kind = "Messages",
            avatarStartArgb = 0xFF607D8BL,
            avatarEndArgb = 0xFF455A64L,
        ),
        SearchHit(
            title = "Swiggy",
            subtitle = "Food · HDFC XX4521",
            kind = "Money",
            moneyEmoji = "🍔",
            moneyAmount = "-₹2,450",
            avatarStartArgb = 0L,
            avatarEndArgb = 0L,
        ),
        SearchHit(
            title = "Food & dining",
            subtitle = "₹8,420 this month · sample",
            kind = "Money",
            moneyEmoji = "🍽",
            moneyAmount = "",
            avatarStartArgb = 0L,
            avatarEndArgb = 0L,
        ),
    )
