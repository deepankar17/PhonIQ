package com.phoniq.app.ui.phone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.phoniq.app.R
import com.phoniq.app.data.model.ContactRow
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock

/**
 * Mock per-contact routing UI only — toggles are held in composition state (not persisted).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPoliciesBottomSheet(
    contact: ContactRow,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ringNormally = remember { mutableStateOf(true) }
    val defaultSim = remember { mutableIntStateOf(0) } // 0 = SIM 1, 1 = SIM 2
    val customTone = remember { mutableStateOf(false) }

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
                text = stringResource(R.string.contact_policy_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyMedium,
                color = PhoniqTextSecondaryMock,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.contact_policy_ring_mode),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.contact_policy_ring_mode_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = PhoniqTextSecondaryMock,
                    )
                }
                Switch(
                    checked = ringNormally.value,
                    onCheckedChange = { ringNormally.value = it },
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.contact_policy_default_sim),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                stringResource(R.string.contact_policy_sim1),
                stringResource(R.string.contact_policy_sim2),
            ).forEachIndexed { index, label ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = defaultSim.intValue == index,
                                onClick = { defaultSim.intValue = index },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = defaultSim.intValue == index,
                        onClick = null,
                    )
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.contact_policy_custom_ringtone),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.contact_policy_custom_ringtone_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = PhoniqTextSecondaryMock,
                    )
                }
                Switch(
                    checked = customTone.value,
                    onCheckedChange = { customTone.value = it },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.contact_policy_mock_note),
                style = MaterialTheme.typography.bodySmall,
                color = PhoniqTextSecondaryMock,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_done))
            }
        }
    }
}
