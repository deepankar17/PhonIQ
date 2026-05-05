package com.phoniq.app.ui.money

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBorder
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

data class MonthlySpend(val month: Month, val year: Int, val totalSpent: Double)

/**
 * Bar chart showing monthly spending for the past 6 months, powered by Vico.
 */
@Composable
fun SpendingAnalyticsSection(
    monthlySpends: List<MonthlySpend>,
    modifier: Modifier = Modifier,
) {
    if (monthlySpends.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(monthlySpends) {
        modelProducer.runTransaction {
            columnSeries {
                series(y = monthlySpends.map { it.totalSpent.coerceAtLeast(0.0) })
            }
        }
    }

    val accentFill = fill(PhoniqAccent)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PhoniqSurface,
        border = BorderStroke(1.dp, PhoniqBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Monthly spending",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val total = monthlySpends.lastOrNull()?.totalSpent ?: 0.0
                Text(
                    "This month: ₹${total.toInt()}",
                    fontSize = 11.sp,
                    color = PhoniqTextSecondaryMock,
                )
            }

            Spacer(Modifier.height(16.dp))

            CartesianChartHost(
                chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(
                                    fill = accentFill,
                                    thickness = 16.dp,
                                    shape = CorneredShape.rounded(topLeftPercent = 25, topRightPercent = 25),
                                )
                            )
                        ),
                    startAxis = VerticalAxis.rememberStart(
                        label = null,
                        tick = null,
                        guideline = null,
                    ),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = { _, value, _ ->
                            val idx = value.toInt().coerceIn(monthlySpends.indices)
                            monthlySpends[idx].month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        },
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            )

            // Legend
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val maxMonth = monthlySpends.maxByOrNull { it.totalSpent }
                val minMonth = monthlySpends.filter { it.totalSpent > 0 }.minByOrNull { it.totalSpent }
                if (maxMonth != null) {
                    LegendChip(
                        label = "Peak: ${maxMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}",
                        value = "₹${maxMonth.totalSpent.toInt()}",
                        color = Color(0xFFFF6B6B),
                    )
                }
                if (minMonth != null) {
                    LegendChip(
                        label = "Low: ${minMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}",
                        value = "₹${minMonth.totalSpent.toInt()}",
                        color = PhoniqAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendChip(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = PhoniqTextSecondaryMock)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
