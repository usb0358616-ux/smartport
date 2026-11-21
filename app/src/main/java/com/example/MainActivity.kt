package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.local.entities.UserAccountEntity
import com.example.domain.model.ThemePreference
import com.example.ui.navigation.AdminSettingsRoute
import com.example.ui.navigation.DashboardRoute
import com.example.ui.navigation.ImportRoute
import com.example.ui.navigation.ModuleDetailRoute
import com.example.ui.navigation.ModuleIds
import com.example.ui.navigation.ModulesHubRoute
import com.example.ui.navigation.ScannerRoute
import com.example.ui.navigation.TravelersRoute
import com.example.ui.screens.AdminSettingsScreen
import com.example.ui.screens.CentralImportScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ModuleDetailScreen
import com.example.ui.screens.PassportScannerScreen
import com.example.ui.screens.SpecializedModulesScreen
import com.example.ui.screens.TravelersScreen
import com.example.ui.theme.BorderPortsTheme
import com.example.ui.theme.SovereignGold
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ImportViewModel
import com.example.ui.viewmodel.ModulesViewModel
import com.example.ui.viewmodel.ScannerViewModel
import com.example.ui.viewmodel.TravelersViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : FragmentActivity() {

    private val authViewModel: AuthViewModel by viewModel()
    private val travelersViewModel: TravelersViewModel by viewModel()
    private val scannerViewModel: ScannerViewModel by viewModel()
    private val importViewModel: ImportViewModel by viewModel()
    private val modulesViewModel: ModulesViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePref by authViewModel.themePreference.collectAsStateWithLifecycle()
            val darkTheme = when (themePref) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }

            BorderPortsTheme(darkTheme = darkTheme) {
                val userCount by authViewModel.userCount.collectAsStateWithLifecycle()
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val biometricUsername by authViewModel.biometricUsername.collectAsStateWithLifecycle()

                when {
                    userCount == null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    authState.currentUser == null -> {
                        LoginScreen(
                            isFirstRunSetup = (userCount == 0),
                            authState = authState,
                            biometricUsername = biometricUsername,
                            onBootstrapAdmin = authViewModel::bootstrapFirstAdmin,
                            onLogin = authViewModel::login,
                            onBiometricLogin = authViewModel::loginWithBiometric
                        )
                    }
                    else -> {
                        MainOperationsShell(
                            currentUser = authState.currentUser!!,
                            themePreference = themePref,
                            authViewModel = authViewModel,
                            travelersViewModel = travelersViewModel,
                            scannerViewModel = scannerViewModel,
                            importViewModel = importViewModel,
                            modulesViewModel = modulesViewModel
                        )
                    }
                }
            }
        }
    }
}

private data class NavTabItem(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainOperationsShell(
    currentUser: UserAccountEntity,
    themePreference: ThemePreference,
    authViewModel: AuthViewModel,
    travelersViewModel: TravelersViewModel,
    scannerViewModel: ScannerViewModel,
    importViewModel: ImportViewModel,
    modulesViewModel: ModulesViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val activePort by authViewModel.activePortName.collectAsStateWithLifecycle()
    val metrics by authViewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val activeAlerts by authViewModel.activeAlerts.collectAsStateWithLifecycle()

    val tabs = listOfNotNull(
        NavTabItem(DashboardRoute, "الرئيسية", Icons.Filled.Dashboard, "nav_dashboard"),
        if (currentUser.role.canScanAndInvestigate) {
            NavTabItem(ScannerRoute, "الماسح", Icons.Filled.DocumentScanner, "nav_scanner")
        } else null,
        NavTabItem(TravelersRoute, "المسافرون", Icons.Filled.Groups, "nav_travelers"),
        NavTabItem(ImportRoute, "الاستيراد", Icons.Filled.FileUpload, "nav_import"),
        NavTabItem(ModulesHubRoute, "الوحدات", Icons.Filled.Apps, "nav_modules")
    )

    val navigateTab: (Any) -> Unit = { target ->
        navController.navigate(target) {
            popUpTo<DashboardRoute> { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isModuleDetail = currentDest?.hasRoute<ModuleDetailRoute>() == true

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (!isModuleDetail) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Shield, contentDescription = null, tint = SovereignGold)
                                Text(
                                    text = "  نظام المنافذ والجوازات",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { navigateTab(AdminSettingsRoute) },
                                modifier = Modifier.testTag("open_settings_button")
                            ) {
                                Icon(Icons.Filled.Settings, contentDescription = "الإعدادات وسجلات التدقيق")
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!useRail && !isModuleDetail) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !isModuleDetail) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                        NavigationRailItem(
                            selected = currentDest?.hasRoute<AdminSettingsRoute>() == true,
                            onClick = { navigateTab(AdminSettingsRoute) },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = "الإعدادات") },
                            label = { Text("الإعدادات") }
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = DashboardRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<DashboardRoute> {
                        DashboardScreen(
                            currentUser = currentUser,
                            activePort = activePort,
                            metrics = metrics,
                            activeAlerts = activeAlerts,
                            onNavigateScanner = {
                                if (currentUser.role.canScanAndInvestigate) navigateTab(ScannerRoute)
                            },
                            onNavigateImport = { navigateTab(ImportRoute) },
                            onNavigateTravelers = { navigateTab(TravelersRoute) },
                            onNavigatePermitQr = {
                                navController.navigate(ModuleDetailRoute(ModuleIds.PERMITS_QR))
                            },
                            onNavigateSecurityReports = {
                                navController.navigate(ModuleDetailRoute(ModuleIds.SECURITY_REPORTS))
                            },
                            onNavigateSeizures = {
                                navController.navigate(ModuleDetailRoute(ModuleIds.SEIZURES))
                            },
                            onAcknowledgeAlert = authViewModel::acknowledgeAlert
                        )
                    }

                    composable<ScannerRoute> {
                        PassportScannerScreen(
                            currentUser = currentUser,
                            viewModel = scannerViewModel
                        )
                    }

                    composable<TravelersRoute> {
                        TravelersScreen(
                            currentUser = currentUser,
                            viewModel = travelersViewModel
                        )
                    }

                    composable<ImportRoute> {
                        CentralImportScreen(
                            currentUser = currentUser,
                            viewModel = importViewModel
                        )
                    }

                    composable<ModulesHubRoute> {
                        SpecializedModulesScreen(
                            viewModel = modulesViewModel,
                            onOpenModule = { moduleId ->
                                navController.navigate(ModuleDetailRoute(moduleId))
                            }
                        )
                    }

                    composable<ModuleDetailRoute> { entry ->
                        val route = entry.toRoute<ModuleDetailRoute>()
                        ModuleDetailScreen(
                            moduleId = route.moduleId,
                            currentUser = currentUser,
                            viewModel = modulesViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AdminSettingsRoute> {
                        AdminSettingsScreen(
                            currentUser = currentUser,
                            themePreference = themePreference,
                            activePort = activePort,
                            authViewModel = authViewModel,
                            importViewModel = importViewModel
                        )
                    }
                }
            }
        }
    }
}
