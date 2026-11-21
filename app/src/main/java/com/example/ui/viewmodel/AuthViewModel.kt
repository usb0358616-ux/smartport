package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BorderRepository
import com.example.data.local.DashboardMetrics
import com.example.data.local.PreferencesRepository
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.UserAccountEntity
import com.example.data.local.entities.WatchlistAlertEntity
import com.example.domain.model.ThemePreference
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: UserAccountEntity? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val chainIntegrityValid: Boolean? = null
)

class AuthViewModel(
    private val repository: BorderRepository,
    private val preferences: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val userCount: StateFlow<Int?> = repository.userCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<UserAccountEntity>> = repository.users
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themePreference: StateFlow<ThemePreference> = preferences.themePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreference.DARK)

    val activePortName: StateFlow<String> = preferences.activePortName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "المنفذ الرئيسي")

    val biometricUsername: StateFlow<String?> = preferences.biometricUsername
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dashboardMetrics: StateFlow<DashboardMetrics> = repository.dashboardMetrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    val activeAlerts: StateFlow<List<WatchlistAlertEntity>> = repository.activeAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlerts: StateFlow<List<WatchlistAlertEntity>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun bootstrapFirstAdmin(
        username: String,
        fullName: String,
        badgeNumber: String,
        assignedPort: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.bootstrapFirstAdmin(username, fullName, badgeNumber, assignedPort, password)
                .onSuccess { user ->
                    preferences.setActivePortName(user.assignedPort)
                    _uiState.update { it.copy(currentUser = user, isLoading = false, errorMessage = null) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.authenticate(username, password)
                .onSuccess { user ->
                    preferences.setActivePortName(user.assignedPort)
                    _uiState.update { it.copy(currentUser = user, isLoading = false, errorMessage = null) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    fun loginWithBiometric(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.authenticateByBiometricUsername(username)
                .onSuccess { user ->
                    preferences.setActivePortName(user.assignedPort)
                    _uiState.update { it.copy(currentUser = user, isLoading = false, errorMessage = null) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    fun logout() {
        val actor = _uiState.value.currentUser
        if (actor != null) {
            viewModelScope.launch {
                repository.appendAuditLog(
                    actionType = "LOGOUT",
                    entityType = "UserAccount",
                    entityId = actor.id.toString(),
                    summary = "تسجيل خروج الضابط: ${actor.fullName}",
                    details = "",
                    user = actor
                )
            }
        }
        _uiState.update { it.copy(currentUser = null, errorMessage = null, statusMessage = null) }
    }

    fun toggleBiometricForCurrentUser(enabled: Boolean) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            val updated = repository.setBiometricEnabled(user, enabled)
            preferences.setBiometricUsername(if (enabled) updated.username else null)
            _uiState.update {
                it.copy(
                    currentUser = updated,
                    statusMessage = if (enabled) "تم ربط الدخول بالبصمة الحيوية بنجاح" else "تم إلغاء البصمة الحيوية"
                )
            }
        }
    }

    fun setTheme(pref: ThemePreference) {
        viewModelScope.launch { preferences.setThemePreference(pref) }
    }

    fun setActivePort(portName: String) {
        viewModelScope.launch { preferences.setActivePortName(portName) }
    }

    fun createUser(
        username: String,
        fullName: String,
        badgeNumber: String,
        role: UserRole,
        assignedPort: String,
        password: String
    ) {
        val actor = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.createUserAccount(actor, username, fullName, badgeNumber, role, assignedPort, password)
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "تم إنشاء الحساب بنجاح", errorMessage = null) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
        }
    }

    fun updateUserRoleOrStatus(target: UserAccountEntity, newRole: UserRole, isActive: Boolean) {
        val actor = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.updateUserRoleOrStatus(actor, target, newRole, isActive)
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "تم تحديث الصلاحية بنجاح") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
        }
    }

    fun acknowledgeAlert(alertId: Long) {
        val actor = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.acknowledgeAlert(alertId, actor)
        }
    }

    fun verifyAuditChain() {
        viewModelScope.launch {
            val valid = repository.verifyAuditChainIntegrity()
            _uiState.update {
                it.copy(
                    chainIntegrityValid = valid,
                    statusMessage = if (valid) "سلسلة التدقيق سليمة 100% ولم يتم التلاعب بها" else "تحذير: تم رصد عدم تطابق في سلسلة التجزئة!"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, statusMessage = null) }
    }
}
