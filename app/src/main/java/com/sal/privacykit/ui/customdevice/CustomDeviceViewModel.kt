package com.sal.privacykit.ui.customdevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.repository.ProfileRepository
import kotlinx.coroutines.launch

class CustomDeviceViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    fun createCustomProfile(
        profileName: String,
        deviceName: String,
        deviceManufacturer: String,
        onCreated: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            onCreated(
                profileRepository.createCustomProfile(
                    name = profileName,
                    customDeviceName = deviceName,
                    customDeviceManufacturer = deviceManufacturer,
                ),
            )
        }
    }
}
