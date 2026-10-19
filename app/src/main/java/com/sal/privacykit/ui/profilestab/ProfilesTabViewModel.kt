
package com.sal.privacykit.ui.profilestab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfilesTabViewModel(private val profileRepository: ProfileRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val activeProfileId: StateFlow<Long?> = combine(
        profileRepository.observeAll(),
        profileRepository.activeProfileIdGlobal(),
    ) { all, selectedId ->
        all.firstOrNull { it.id == selectedId && !it.archived }?.id
            ?: all.firstOrNull { !it.archived }?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profiles: StateFlow<List<Profile>> = combine(profileRepository.observeAll(), _query) { all, query ->
        all.filterNot { it.archived }
            .filter { it.name.contains(query, ignoreCase = true) || it.deviceTemplateId.contains(query, ignoreCase = true) }
            .sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActive(profile: Profile) {
        viewModelScope.launch { profileRepository.setActiveProfile(profile) }
    }

    fun onQueryChange(value: String) { _query.value = value }
}
