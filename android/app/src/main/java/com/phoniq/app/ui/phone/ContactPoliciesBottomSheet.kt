package com.phoniq.app.ui.phone

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.phoniq.app.R
import com.phoniq.app.data.model.ContactRow
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import com.phoniq.app.util.ContactPoliciesStore

/**
 * Per-contact routing preferences (ring mode, preferred SIM, custom ringtone).
 * Incoming notifications respect [ContactPoliciesStore] via [com.phoniq.app.telecom.IncomingCallNotification].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPoliciesBottomSheet(
    contact: ContactRow,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initial = remember(contact.id) { ContactPoliciesStore.load(context, contact) }
    val ringNormally = remember(contact.id) { mutableStateOf(initial.ringNormally) }
    val defaultSim = remember(contact.id) { mutableIntStateOf(initial.defaultSimIndex) }
    val customTone = remember(contact.id) { mutableStateOf(initial.customRingtoneEnabled) }
    val ringtoneUriStr = remember(contact.id) { mutableStateOf(initial.customRingtoneUri) }

    fun currentState(): ContactPoliciesStore.State =
        ContactPoliciesStore.State(
            ringNormally = ringNormally.value,
            defaultSimIndex = defaultSim.intValue,
            customRingtoneEnabled = customTone.value,
            customRingtoneUri = ringtoneUriStr.value,
        )

    val ringtoneLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
            val data = result.data ?: return@rememberLauncherForActivityResult
            val uri: Uri? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
            ringtoneUriStr.value = uri?.toString().orEmpty()
        }

    fun launchRingtonePicker() {
        val existing =
            ringtoneUriStr.value.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        val intent =
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
            }
        ringtoneLauncher.launch(intent)
    }

    fun persistAndDismiss() {
        ContactPoliciesStore.save(context, contact, currentState())
        onDismissRequest()
    }

    ModalBottomSheet(
        onDismissRequest = {
            ContactPoliciesStore.save(context, contact, currentState())
            onDismissRequest()
        },
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
                    onCheckedChange = {
                        customTone.value = it
                        if (!it) ringtoneUriStr.value = ""
                    },
                )
            }
            if (customTone.value) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { launchRingtonePicker() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.contact_policy_choose_ringtone))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.contact_policy_mock_note),
                style = MaterialTheme.typography.bodySmall,
                color = PhoniqTextSecondaryMock,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { persistAndDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_done))
            }
        }
    }
}
