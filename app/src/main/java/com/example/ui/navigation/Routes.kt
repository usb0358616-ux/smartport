package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

@Serializable
data object ScannerRoute

@Serializable
data object TravelersRoute

@Serializable
data object ImportRoute

@Serializable
data object ModulesHubRoute

@Serializable
data class ModuleDetailRoute(val moduleId: String)

@Serializable
data object AdminSettingsRoute

object ModuleIds {
    const val SEIZURES = "seizures"
    const val MOFA_VISA = "mofa_visa"
    const val RESIDENTS = "residents"
    const val REFUGEES = "refugees"
    const val MIGRANTS = "migrants"
    const val PERMITS_QR = "permits_qr"
    const val SECURITY_REPORTS = "security_reports"
    const val AGENCIES_OFFICERS = "agencies_officers"
}
