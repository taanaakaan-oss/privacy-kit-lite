package com.sal.privacykit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileSwitchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SWITCH_PROFILE) return

        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1L)
        if (profileId <= 0L) {
            Log.e(TAG, "Missing or invalid profile_id: $profileId")
            return
        }

        // Root shell operation can take time; keep the broadcast alive.
        val pendingResult = goAsync()
        val app = context.applicationContext as PrivacyKitApplication

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val profile = app.container.profileRepository
                    .observeById(profileId)
                    .first()

                if (profile == null) {
                    Log.e(TAG, "Profile not found: $profileId")
                    return@launch
                }

                if (profile.archived) {
                    Log.e(TAG, "Cannot activate archived profile: $profileId")
                    return@launch
                }

                // Uses the same switching path as the app's profile UI.
                app.container.profileRepository.setActiveProfile(profile)

                Log.i(TAG, "Activated profile id=$profileId name=${profile.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Profile switch failed: id=$profileId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SWITCH_PROFILE =
            "com.sal.privacykit.action.SWITCH_PROFILE"

        const val EXTRA_PROFILE_ID = "profile_id"

        private const val TAG = "ProfileSwitchReceiver"
    }
}