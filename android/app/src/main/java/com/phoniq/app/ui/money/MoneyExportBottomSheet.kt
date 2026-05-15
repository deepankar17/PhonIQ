package com.phoniq.app.ui.money

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phoniq.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyExportBottomSheet(
    onDismissRequest: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.money_export_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.money_export_sheet_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            TextButton(
                onClick = {
                    onExportCsv()
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.money_export_csv))
            }
            TextButton(
                onClick = {
                    onExportPdf()
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.money_export_pdf))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}
