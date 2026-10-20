package com.sal.privacykit.ui.cloneprofile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sal.privacykit.PrivacyKitApplication
import com.sal.privacykit.data.model.DeviceTemplateCatalog
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar

@Composable
fun CloneProfileScreen(
    onBack: () -> Unit,
    onCloned: (Long) -> Unit,
) {
    val application = LocalContext.current.applicationContext as PrivacyKitApplication
    val viewModel: CloneProfileViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CloneProfileViewModel(application.container.profileRepository) }
        },
    )
    val profiles by viewModel.profiles.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Clone Existing Profile",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            items(profiles, key = { it.id }) { profile ->
                val template = DeviceTemplateCatalog.byId(profile.deviceTemplateId)
                BasicComponent(
                    title = profile.name,
                    summary = profile.deviceLabel(template.name, template.manufacturer),
                    
                    onClick = { viewModel.clone(profile, onCloned) },
                )
            }
        }
    }
}
