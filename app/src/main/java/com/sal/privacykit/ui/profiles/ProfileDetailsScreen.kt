package com.sal.privacykit.ui.profiles

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.data.model.DeviceTemplateCatalog
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProfileDetailsScreen(
    profileId: Long,
    onBack: () -> Unit,
    onDeviceTemplateClick: (Long) -> Unit,
    onIdentifiersClick: (Long) -> Unit,
) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: ProfileDetailsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ProfileDetailsViewModel(profileId, application.container.profileRepository) }
        },
    )
    val profile by viewModel.profile.collectAsState()
    val current = profile ?: return
    val template = DeviceTemplateCatalog.byId(current.deviceTemplateId)
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember(current.id, showRenameDialog) { mutableStateOf(current.name) }

    OverlayDialog(
        show = showRenameDialog,
        onDismissRequest = { showRenameDialog = false },
        title = "Rename profile",
    ) {
        TextField(value = newName, onValueChange = { newName = it }, singleLine = true)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            TextButton(text = "Cancel", onClick = { showRenameDialog = false }, modifier = Modifier.weight(1f))
            androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 8.dp))
            TextButton(
                text = "Save",
                onClick = {
                    if (newName.isNotBlank()) viewModel.rename(current, newName.trim())
                    showRenameDialog = false
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename profile")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MiuixTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = current.name.firstOrNull()?.uppercase() ?: "?",
                            style = MiuixTheme.textStyles.title2,
                            color = MiuixTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(text = current.name, style = MiuixTheme.textStyles.title3, modifier = Modifier.padding(top = 12.dp))
            }
            LazyColumn {
                item {
                    DetailRow(
                        icon = Icons.Filled.Smartphone,
                        title = "Device Template",
                        value = current.deviceLabel(template.name, template.manufacturer),
                        onClick = { onDeviceTemplateClick(profileId) },
                    )
                }
                item {
                    DetailRow(
                        icon = Icons.Filled.Fingerprint,
                        title = "Identifiers",
                        value = "${current.configuredIdentifierCount} Configured",
                        onClick = { onIdentifiersClick(profileId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    ArrowPreference(
        title = title,
        summary = value,
        startAction = { Icon(icon, contentDescription = null, tint = MiuixTheme.colorScheme.onSurfaceVariantActions) },
        onClick = onClick,
    )
}
