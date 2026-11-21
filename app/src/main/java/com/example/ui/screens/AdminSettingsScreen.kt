package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.UserAccountEntity
import com.example.domain.model.ThemePreference
import com.example.domain.model.UserRole
import com.example.ui.components.canUseBiometric
import com.example.ui.theme.ClearanceEmerald
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SovereignGold
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ImportViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminSettingsScreen(
    currentUser: UserAccountEntity,
    themePreference: ThemePreference,
    activePort: String,
    authViewModel: AuthViewModel,
    importViewModel: ImportViewModel
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricSupported = remember(activity) { activity != null && canUseBiometric(activity) }

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val allUsers by authViewModel.allUsers.collectAsStateWithLifecycle()
    val auditLogs by authViewModel.auditLogs.collectAsStateWithLifecycle()
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    var portInput by remember(activePort) { mutableStateOf(activePort) }
    var showCreateUserDialog by remember { mutableStateOf(false) }

    val exportAuditLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            importViewModel.exportAuditLogs(uri)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("الإعدادات والأمان وسجلات التدقيق", style = MaterialTheme.typography.titleLarge)
        }

        if (!authState.statusMessage.isNullOrBlank() || !authState.errorMessage.isNullOrBlank()) {
            item {
                val isError = !authState.errorMessage.isNullOrBlank()
                Surface(
                    color = (if (isError) CriticalCrimson else ClearanceEmerald).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = authState.errorMessage ?: authState.statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) CriticalCrimson else ClearanceEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = authViewModel::clearMessages) { Text("إغلاق") }
                    }
                }
            }
        }

        // Current Officer Profile & Biometric + Theme Settings
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("ملف الضابط النشط", style = MaterialTheme.typography.titleMedium)
                    Text("${currentUser.fullName} — ${currentUser.role.arabicLabel}", style = MaterialTheme.typography.bodyMedium)
                    Text("الرقم الوظيفي: ${currentUser.badgeNumber} | المستخدم: ${currentUser.username}", style = MaterialTheme.typography.bodySmall)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("المنفذ النشط") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { authViewModel.setActivePort(portInput) }) {
                            Text("تحديث")
                        }
                    }

                    if (biometricSupported) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = SovereignGold)
                                Text("الدخول السريع بالبصمة الحيوية")
                            }
                            Switch(
                                checked = currentUser.biometricEnabled,
                                onCheckedChange = { authViewModel.toggleBiometricForCurrentUser(it) }
                            )
                        }
                    }

                    Text("نمط العرض الميداني:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = themePreference == pref,
                                onClick = { authViewModel.setTheme(pref) },
                                label = { Text(pref.arabicLabel, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = authViewModel::logout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CriticalCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("تسجيل الخروج الآمن")
                    }
                }
            }
        }

        // RBAC User Management (Leadership Only)
        if (currentUser.role.canManageUsers) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إدارة المستخدمين والصلاحيات (${allUsers.size})", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { showCreateUserDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("إضافة ضابط/موظف")
                    }
                }
            }

            items(allUsers, key = { it.id }) { u ->
                var roleMenuExpanded by remember { mutableStateOf(false) }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(u.fullName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${u.username} • ${u.assignedPort} • ${if (u.isActive) "نشط" else "معطّل"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box {
                                OutlinedButton(onClick = { roleMenuExpanded = true }) {
                                    Text(u.role.arabicLabel, style = MaterialTheme.typography.labelSmall)
                                }
                                DropdownMenu(expanded = roleMenuExpanded, onDismissRequest = { roleMenuExpanded = false }) {
                                    UserRole.entries.forEach { r ->
                                        DropdownMenuItem(
                                            text = { Text("${r.arabicLabel} (${r.tierLabel})") },
                                            onClick = {
                                                authViewModel.updateUserRoleOrStatus(u, r, u.isActive)
                                                roleMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (u.id != currentUser.id) {
                                Switch(
                                    checked = u.isActive,
                                    onCheckedChange = { authViewModel.updateUserRoleOrStatus(u, u.role, it) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Immutable Audit Logs (Leadership Only)
        if (currentUser.role.canViewAuditLogs) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("سجل التدقيق غير القابل للتعديل (${auditLogs.size})", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = authViewModel::verifyAuditChain) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("فحص النزاهة")
                        }
                        OutlinedButton(
                            onClick = { exportAuditLauncher.launch("audit_logs.xlsx") },
                            enabled = auditLogs.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            items(auditLogs.take(60), key = { it.id }) { log ->
                val sevColor = when (log.severity) {
                    "CRITICAL" -> CriticalCrimson
                    "WARNING" -> SovereignGold
                    else -> ClearanceEmerald
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, sevColor.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(log.summary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(dateFmt.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily))
                        }
                        Text("${log.username} (${log.userRole}) • ${log.portName}", style = MaterialTheme.typography.bodySmall, color = sevColor)
                        if (log.details.isNotBlank()) {
                            Text(log.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "SHA-256: ${log.integrityHash.take(20)}...",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showCreateUserDialog) {
        CreateUserDialog(
            defaultPort = activePort,
            onDismiss = { showCreateUserDialog = false },
            onCreate = { username, fullName, badge, role, port, pass ->
                authViewModel.createUser(username, fullName, badge, role, port, pass)
                showCreateUserDialog = false
            }
        )
    }
}

@Composable
private fun CreateUserDialog(
    defaultPort: String,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, UserRole, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var badge by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(defaultPort) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.PASSPORT_OFFICER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء حساب ضابط / موظف جديد") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(fullName, { fullName = it }, label = { Text("الاسم الكامل") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(badge, { badge = it }, label = { Text("الرقم الوظيفي") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(port, { port = it }, label = { Text("المنفذ المعين") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(username, { username = it }, label = { Text("اسم المستخدم") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("كلمة المرور (6+ خانات)") }, modifier = Modifier.fillMaxWidth())
                Text("الصلاحية:", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    UserRole.entries.forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text("${r.arabicLabel} (${r.tierLabel})") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(username, fullName, badge, role, port, password) },
                enabled = fullName.isNotBlank() && username.length >= 3 && password.length >= 6
            ) { Text("إنشاء") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
