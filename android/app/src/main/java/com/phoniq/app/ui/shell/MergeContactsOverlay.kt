package com.phoniq.app.ui.shell

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.data.model.DuplicateContactGroup
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqOnSurfaceMuted
import com.phoniq.app.ui.theme.PhoniqSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeContactsOverlay(
    groups: List<DuplicateContactGroup>,
    onDismiss: () -> Unit,
    onUserMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val openPeopleError = stringResource(R.string.error_no_contacts_app)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.merge_title)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(R.string.merge_disclosure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Button(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            } catch (_: ActivityNotFoundException) {
                                onUserMessage(openPeopleError)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PhoniqAccent),
                    ) {
                        Text(stringResource(R.string.merge_open_people), color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.merge_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = PhoniqOnSurfaceMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (groups.isEmpty()) {
                        Text(
                            text = stringResource(R.string.merge_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = PhoniqOnSurfaceMuted,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(groups, key = { it.displayNumber + it.contacts.size }) { g ->
                                MergeGroupCard(group = g)
                            }
                            item { Spacer(Modifier.height(64.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeGroupCard(group: DuplicateContactGroup) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = PhoniqSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                stringResource(R.string.merge_group_title, group.contacts.size, group.displayNumber),
                style = MaterialTheme.typography.titleSmall,
                color = PhoniqAccent,
            )
            Spacer(Modifier.height(8.dp))
            group.contacts.forEachIndexed { i, c ->
                Text(
                    "${c.name} — ${c.number}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (i < group.contacts.lastIndex) {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}
