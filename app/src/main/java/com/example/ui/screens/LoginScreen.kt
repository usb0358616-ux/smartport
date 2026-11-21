package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.ui.components.canUseBiometric
import com.example.ui.components.showBiometricPrompt
import com.example.ui.theme.ClearanceEmerald
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SovereignNavy
import com.example.ui.theme.SovereignNavyDeep
import com.example.ui.viewmodel.AuthUiState

@Composable
fun LoginScreen(
    isFirstRunSetup: Boolean,
    authState: AuthUiState,
    biometricUsername: String?,
    onBootstrapAdmin: (username: String, fullName: String, badge: String, port: String, pass: String) -> Unit,
    onLogin: (username: String, password: String) -> Unit,
    onBiometricLogin: (username: String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAvailable = remember(activity, biometricUsername) {
        activity != null && !biometricUsername.isNullOrBlank() && canUseBiometric(activity)
    }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var badgeNumber by remember { mutableStateOf("") }
    var assignedPort by remember { mutableStateOf("المنفذ الرئيسي") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SovereignNavyDeep, SovereignNavy, MaterialTheme.colorScheme.background)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_sovereign_crest),
                contentDescription = "شعار نظام المنافذ والجوازات",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "نظام المنافذ والجوازات الذكي",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = SovereignGold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "النظام الميداني السيادي • يعمل محلياً دون إنترنت 100% ومشفّر بـ SQLCipher",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isFirstRunSetup) {
                        Surface(
                            color = ClearanceEmerald.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = ClearanceEmerald)
                                Column {
                                    Text(
                                        text = "تهيئة حساب القيادة الأول (First-Run Setup)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ClearanceEmerald
                                    )
                                    Text(
                                        text = "أنشئ حساب المدير العام الأول للنظام لتفعيل قاعدة البيانات المشفرة.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("الاسم والرتبة الكاملة") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_fullname_input")
                        )
                        OutlinedTextField(
                            value = badgeNumber,
                            onValueChange = { badgeNumber = it },
                            label = { Text("الرقم العسكري / الوظيفي") },
                            leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_badge_input")
                        )
                        OutlinedTextField(
                            value = assignedPort,
                            onValueChange = { assignedPort = it },
                            label = { Text("اسم المنفذ / المركز") },
                            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_port_input")
                        )
                    } else {
                        Text(
                            text = "تسجيل دخول الضباط والموظفين",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور (6 خانات فأكثر)") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "إظهار أو إخفاء كلمة المرور"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    val displayedError = localError ?: authState.errorMessage
                    if (!displayedError.isNullOrBlank()) {
                        Text(
                            text = displayedError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            localError = null
                            if (isFirstRunSetup) {
                                if (fullName.isBlank() || badgeNumber.isBlank() || username.isBlank() || password.length < 6) {
                                    localError = "أكمل جميع الحقول مع كلمة مرور لا تقل عن 6 خانات"
                                } else {
                                    onBootstrapAdmin(username, fullName, badgeNumber, assignedPort, password)
                                }
                            } else {
                                onLogin(username, password)
                            }
                        },
                        enabled = !authState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_button")
                    ) {
                        if (authState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isFirstRunSetup) "اعتماد القيادة والدخول للنظام" else "دخول آمن",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    if (!isFirstRunSetup && biometricAvailable && activity != null && !biometricUsername.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                showBiometricPrompt(
                                    activity = activity,
                                    title = "المصادقة الحيوية للضابط",
                                    subtitle = "الدخول السريع لحساب: $biometricUsername",
                                    onSuccess = { onBiometricLogin(biometricUsername) },
                                    onError = { msg -> localError = msg }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("biometric_login_button")
                        ) {
                            Icon(Icons.Filled.Fingerprint, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("الدخول بالبصمة ($biometricUsername)")
                        }
                    }
                }
            }
        }
    }
}
