package com.sal.privacykit.ui.navigation

object Routes {
    const val APPS = "apps"
    const val PROFILES = "profiles"
    const val SETTINGS = "settings"
    const val APPEARANCE = "appearance"
    const val ADD_APP = "add_app"
    const val ONBOARDING = "onboarding"
    const val DEVELOPER_SETTINGS = "developer_settings"
    const val CUSTOM_HOOKS = "custom_hooks"

    const val PROFILE_DETAILS = "profile_details/{profileId}"
    const val DEVICE_TEMPLATE = "device_template/{profileId}"
    const val IDENTIFIERS = "identifiers/{profileId}"
    const val IDENTIFIER_RULE = "identifier_rule/{profileId}/{identifierKey}"
    const val CREATE_PROFILE = "create_profile"
    const val CLONE_PROFILE = "clone_profile"
    const val CUSTOM_DEVICE = "custom_device"
    const val LAUNCH_OPTIONS = "launch_options/{profileId}"

    fun profileDetails(profileId: Long) = "profile_details/$profileId"
    fun deviceTemplate(profileId: Long) = "device_template/$profileId"
    fun identifiers(profileId: Long) = "identifiers/$profileId"
    fun identifierRule(profileId: Long, identifierKey: String) = "identifier_rule/$profileId/$identifierKey"
    fun createProfile() = "create_profile"
    fun cloneProfile() = "clone_profile"
    fun customDevice() = "custom_device"
    fun launchOptions(profileId: Long) = "launch_options/$profileId"
}

val TOP_LEVEL_ROUTES = setOf(Routes.APPS, Routes.PROFILES, Routes.SETTINGS)
