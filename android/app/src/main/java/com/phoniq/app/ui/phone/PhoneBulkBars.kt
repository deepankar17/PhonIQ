package com.phoniq.app.ui.phone

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phoniq.app.R

/** Minimum touch target per Material accessibility guidance (48×48 dp). */
private val BulkActionMinTouchDp = 48.dp

internal fun Context.shareNumbersLines(lines: Collection<String>) {
    val text = lines.joinToString("\n")
    if (text.isBlank()) return
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    startActivity(Intent.createChooser(intent, null))
}

@Composable
private fun BulkLabeledAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.widthIn(min = 56.dp, max = 96.dp),
) {
    val color =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            modifier
                .defaultMinSize(minWidth = BulkActionMinTouchDp, minHeight = BulkActionMinTouchDp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    indication = ripple(bounded = true),
                    interactionSource = remember { MutableInteractionSource() },
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = color,
        )
    }
}

@Composable
internal fun PhoneRecentBulkBar(
    count: Int,
    visibleRecentsCount: Int,
    onDismiss: () -> Unit,
    onSelectAllVisible: () -> Unit,
    onTrusted: () -> Unit,
    onDelete: () -> Unit,
    onSpam: () -> Unit,
    onBlock: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_close),
                    icon = Icons.Default.Close,
                    enabled = true,
                    onClick = onDismiss,
                    modifier = Modifier.widthIn(min = BulkActionMinTouchDp, max = 72.dp),
                )
                Text(
                    text = stringResource(R.string.bulk_selected_count, count),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_all_visible),
                    icon = Icons.Default.SelectAll,
                    enabled = visibleRecentsCount > 0,
                    onClick = onSelectAllVisible,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_trusted),
                    icon = Icons.Default.VerifiedUser,
                    enabled = count > 0,
                    onClick = onTrusted,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_delete),
                    icon = Icons.Default.Delete,
                    enabled = count > 0,
                    onClick = onDelete,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_spam),
                    icon = Icons.Default.Warning,
                    enabled = count > 0,
                    onClick = onSpam,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_block),
                    icon = Icons.Default.Block,
                    enabled = true,
                    onClick = onBlock,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_share),
                    icon = Icons.Default.Share,
                    enabled = count > 0,
                    onClick = onShare,
                )
            }
        }
    }
}

@Composable
internal fun PhoneContactsBulkBar(
    count: Int,
    visibleContactsCount: Int,
    onDismiss: () -> Unit,
    onSelectAllVisible: () -> Unit,
    onStar: () -> Unit,
    onTrusted: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
    onSpam: () -> Unit,
    onBlock: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_close),
                    icon = Icons.Default.Close,
                    enabled = true,
                    onClick = onDismiss,
                    modifier = Modifier.widthIn(min = BulkActionMinTouchDp, max = 72.dp),
                )
                Text(
                    text = stringResource(R.string.bulk_selected_count, count),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_all_visible),
                    icon = Icons.Default.SelectAll,
                    enabled = visibleContactsCount > 0,
                    onClick = onSelectAllVisible,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_star),
                    icon = Icons.Default.Star,
                    enabled = count > 0,
                    onClick = onStar,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_trusted),
                    icon = Icons.Default.VerifiedUser,
                    enabled = count > 0,
                    onClick = onTrusted,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_merge),
                    icon = Icons.Default.GroupWork,
                    enabled = count > 0,
                    onClick = onMerge,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_delete_contact),
                    icon = Icons.Default.Delete,
                    enabled = count > 0,
                    onClick = onDelete,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_spam),
                    icon = Icons.Default.Warning,
                    enabled = count > 0,
                    onClick = onSpam,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_block),
                    icon = Icons.Default.Block,
                    enabled = true,
                    onClick = onBlock,
                )
                BulkLabeledAction(
                    label = stringResource(R.string.bulk_label_share),
                    icon = Icons.Default.Share,
                    enabled = count > 0,
                    onClick = onShare,
                )
            }
        }
    }
}
