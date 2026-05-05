package com.phoniq.app.ui.money

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBorder
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock

data class AccountBalance(
    val accountId: Long,
    val bankName: String,
    val last4: String,
    val accountType: String,
    val netBalance: Double,
)

/**
 * Horizontal scroll strip of account balance cards derived from parsed SMS transactions.
 * Shows net balance (credits - debits) per account.
 */
@Composable
fun AccountBalanceSection(
    accounts: List<AccountBalance>,
    modifier: Modifier = Modifier,
) {
    if (accounts.isEmpty()) return

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(accounts, key = { it.accountId }) { acct ->
            AccountBalanceCard(acct)
        }
    }
}

@Composable
private fun AccountBalanceCard(acct: AccountBalance) {
    val isCredit = acct.netBalance >= 0
    val balanceColor = if (isCredit) PhoniqSecondary else Color(0xFFFF6B6B)
    val initials = acct.bankName.take(2).uppercase()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PhoniqSurface,
        border = BorderStroke(1.dp, PhoniqBorder),
        modifier = Modifier.width(160.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(PhoniqAccent, Color(0xFF4A43CC)))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        initials,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        acct.bankName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        "····${acct.last4}  ·  ${acct.accountType}",
                        fontSize = 9.sp,
                        color = PhoniqTextSecondaryMock,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "${if (isCredit) "+" else ""}₹${"%,.0f".format(acct.netBalance)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = balanceColor,
            )
            Text(
                "Net balance · SMS derived",
                fontSize = 9.sp,
                color = PhoniqTextSecondaryMock,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
