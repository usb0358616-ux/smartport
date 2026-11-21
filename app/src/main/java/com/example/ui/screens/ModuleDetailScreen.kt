package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.ForeignerResidentEntity
import com.example.data.local.entities.InternalMovementPermitEntity
import com.example.data.local.entities.MofaVisaRequestEntity
import com.example.data.local.entities.PassportSeizureEntity
import com.example.data.local.entities.PortOfficerEntity
import com.example.data.local.entities.RefugeeRecordEntity
import com.example.data.local.entities.SecurityReportEntity
import com.example.data.local.entities.TravelAgencyEntity
import com.example.data.local.entities.UndocumentedMigrantEntity
import com.example.data.local.entities.UserAccountEntity
import com.example.domain.model.PermitStatus
import com.example.domain.model.SecurityReportType
import com.example.domain.model.SecuritySeverity
import com.example.domain.model.SeizureStatus
import com.example.domain.security.QrCodeHelper
import com.example.ui.components.QrCodePreviewDialog
import com.example.ui.components.SignaturePadDialog
import com.example.ui.navigation.ModuleIds
import com.example.ui.theme.ClearanceEmerald
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ModulesViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(
    moduleId: String,
    currentUser: UserAccountEntity,
    viewModel: ModulesViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val seizures by viewModel.seizures.collectAsStateWithLifecycle()
    val mofa by viewModel.mofaRequests.collectAsStateWithLifecycle()
    val residents by viewModel.foreignerResidents.collectAsStateWithLifecycle()
    val refugees by viewModel.refugeeRecords.collectAsStateWithLifecycle()
    val migrants by viewModel.undocumentedMigrants.collectAsStateWithLifecycle()
    val permits by viewModel.movementPermits.collectAsStateWithLifecycle()
    val reports by viewModel.securityReports.collectAsStateWithLifecycle()
    val agencies by viewModel.travelAgencies.collectAsStateWithLifecycle()
    val officers by viewModel.portOfficers.collectAsStateWithLifecycle()
    val verifiedPermit by viewModel.verifiedPermit.collectAsStateWithLifecycle()
    val msg by viewModel.moduleMessage.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var signatureTarget by remember { mutableStateOf<PassportSeizureEntity?>(null) }
    var qrPreviewTarget by remember { mutableStateOf<InternalMovementPermitEntity?>(null) }
    var showQrScanner by remember { mutableStateOf(false) }
    var directoryTab by remember { mutableIntStateOf(0) }

    val title = when (moduleId) {
        ModuleIds.SEIZURES -> "محاضر الضبط والجوازات المحتجزة"
        ModuleIds.MOFA_VISA -> "مذكرات الخارجية والمنظمات الدولية"
        ModuleIds.RESIDENTS -> "الوافدون الأجانب والإقامات"
        ModuleIds.REFUGEES -> "شؤون اللاجئين (UNHCR)"
        ModuleIds.MIGRANTS -> "المهاجرون غير النظاميين"
        ModuleIds.PERMITS_QR -> "تصاريح التنقل الداخلي (QR)"
        ModuleIds.SECURITY_REPORTS -> "بلاغات الضبط والتعميمات"
        ModuleIds.AGENCIES_OFFICERS -> "دليل الوكالات وضباط المنافذ"
        else -> "الوحدة التخصصية"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (moduleId == ModuleIds.PERMITS_QR) {
                        IconButton(onClick = { showQrScanner = !showQrScanner }) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "مسح رمز QR للتصريح", tint = SovereignGold)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة سجل")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!msg.isNullOrBlank()) {
                Surface(
                    color = SovereignGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg.orEmpty(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::clearMessage) { Text("إخلاق") }
                    }
                }
            }

            if (moduleId == ModuleIds.AGENCIES_OFFICERS) {
                TabRow(selectedTabIndex = directoryTab) {
                    Tab(selected = directoryTab == 0, onClick = { directoryTab = 0 }, text = { Text("الوكالات (${agencies.size})") })
                    Tab(selected = directoryTab == 1, onClick = { directoryTab = 1 }, text = { Text("ضباط المنافذ (${officers.size})") })
                }
            }

            if (moduleId == ModuleIds.PERMITS_QR && showQrScanner) {
                PermitQrScannerPanel(
                    onQrScanned = { code ->
                        showQrScanner = false
                        viewModel.verifyPermitQr(code, currentUser)
                    },
                    onManualCodeSubmit = { code ->
                        viewModel.verifyPermitQr(code, currentUser)
                    }
                )
            }

            verifiedPermit?.let { permit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = ClearanceEmerald.copy(alpha = 0.14f)),
                    border = BorderStroke(1.5.dp, ClearanceEmerald)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("✓ تصريح موثق: ${permit.permitCode} (${permit.status.arabicLabel})", color = ClearanceEmerald, fontWeight = FontWeight.Bold)
                        Text("الوفد: ${permit.delegationName} | الجهة: ${permit.organization}")
                        Text("القائد: ${permit.leaderName} | المسار: ${permit.originGovernorate} -> ${permit.destinationGovernorate}")
                        TextButton(onClick = viewModel::clearVerifiedPermit) { Text("إخفاء") }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                when (moduleId) {
                    ModuleIds.SEIZURES -> {
                        items(seizures, key = { it.id }) { s ->
                            SeizureCard(
                                seizure = s,
                                onAdvanceStatus = { nextStatus ->
                                    viewModel.saveSeizure(s.copy(status = nextStatus), currentUser)
                                },
                                onOpenSignature = { signatureTarget = s }
                            )
                        }
                    }
                    ModuleIds.MOFA_VISA -> {
                        items(mofa, key = { it.id }) { item ->
                            SimpleRecordCard(
                                title = "${item.applicantName} (${item.passportNumber})",
                                badge = item.organizationName,
                                subtitle = "مذكرة: ${item.memoNumber} | رحلة/مسار: ${item.flightNumber} ${item.airRoute} | المنفذ: ${item.entryPort}",
                                badgeColor = SovereignGold
                            )
                        }
                    }
                    ModuleIds.RESIDENTS -> {
                        items(residents, key = { it.id }) { item ->
                            SimpleRecordCard(
                                title = "${item.fullName} — ${item.nationality}",
                                badge = item.status,
                                subtitle = "إقامة: ${item.residencyNumber} | جواز: ${item.passportNumber} | انتهاء: ${item.expiryDate}",
                                badgeColor = ClearanceEmerald
                            )
                        }
                    }
                    ModuleIds.REFUGEES -> {
                        items(refugees, key = { it.id }) { item ->
                            SimpleRecordCard(
                                title = "${item.fullName} (${item.nationality})",
                                badge = item.unhcrNumber,
                                subtitle = "الأسرة: ${item.familySize} | المحافظة/المخيم: ${item.campOrGovernorate} | المنفذ: ${item.arrivalPort}",
                                badgeColor = SovereignGold
                            )
                        }
                    }
                    ModuleIds.MIGRANTS -> {
                        items(migrants, key = { it.id }) { item ->
                            SimpleRecordCard(
                                title = "${item.fullName} (${item.claimedNationality})",
                                badge = item.deportationStatus,
                                subtitle = "ملف: ${item.caseNumber} | الضبط: ${item.apprehensionLocation} | المركز: ${item.detentionFacility}",
                                badgeColor = WarningAmber
                            )
                        }
                    }
                    ModuleIds.PERMITS_QR -> {
                        items(permits, key = { it.id }) { permit ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(permit.delegationName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            permit.permitCode,
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = SovereignGold
                                        )
                                    }
                                    Text("${permit.organization} • القائد: ${permit.leaderName} (${permit.membersCount} أفراد)", style = MaterialTheme.typography.bodySmall)
                                    Text("المسار: ${permit.originGovernorate} -> ${permit.destinationGovernorate} | من ${permit.validFrom} إلى ${permit.validUntil}", style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(onClick = { qrPreviewTarget = permit }) {
                                            Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.size(6.dp))
                                            Text("عرض رمز QR للتحقق")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ModuleIds.SECURITY_REPORTS -> {
                        items(reports, key = { it.id }) { rep ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, if (rep.isActive) CriticalCrimson else MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${rep.subjectName} (${rep.passportOrIdNumber})", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = if (rep.isActive) "نشط" else "موقوف",
                                            color = if (rep.isActive) CriticalCrimson else ClearanceEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text("تعميم: ${rep.circularNumber} | النوع: ${rep.reportType.arabicLabel} | ${rep.severity.arabicLabel}", style = MaterialTheme.typography.bodySmall)
                                    Text("السبب: ${rep.reason}", style = MaterialTheme.typography.bodySmall)
                                    if (currentUser.role.canApproveTravelBan) {
                                        TextButton(onClick = { viewModel.toggleReportActive(rep, currentUser) }) {
                                            Text(if (rep.isActive) "إيقاف التعميم" else "إعادة تفعيل التعميم")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ModuleIds.AGENCIES_OFFICERS -> {
                        if (directoryTab == 0) {
                            items(agencies, key = { it.id }) { ag ->
                                SimpleRecordCard(
                                    title = ag.agencyName,
                                    badge = ag.licenseNumber,
                                    subtitle = "المدير: ${ag.managerName} | المنفذ: ${ag.portName} | الهاتف: ${ag.phone}",
                                    badgeColor = SovereignGold
                                )
                            }
                        } else {
                            items(officers, key = { it.id }) { off ->
                                SimpleRecordCard(
                                    title = "${off.rankTitle} / ${off.fullName}",
                                    badge = off.badgeNumber,
                                    subtitle = "المنفذ: ${off.assignedPort} | الوردية: ${off.shiftName} | الهاتف: ${off.phone}",
                                    badgeColor = ClearanceEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    signatureTarget?.let { seizure ->
        SignaturePadDialog(
            title = "توقيع تسليم الجواز (${seizure.passportNumber})",
            initialStrokes = seizure.signatureStrokes,
            onDismiss = { signatureTarget = null },
            onConfirm = { strokes ->
                viewModel.saveSeizure(
                    seizure.copy(signatureStrokes = strokes, status = SeizureStatus.DELIVERED),
                    currentUser
                )
                signatureTarget = null
            }
        )
    }

    qrPreviewTarget?.let { permit ->
        val payload = QrCodeHelper.buildPermitQrPayload(permit.permitCode, permit.leaderPassport)
        val bmp = remember(payload) { QrCodeHelper.generateQrBitmap(payload) }
        QrCodePreviewDialog(
            title = "تصريح تنقل: ${permit.delegationName}",
            subtitle = payload,
            bitmap = bmp,
            onDismiss = { qrPreviewTarget = null }
        )
    }

    if (showCreateDialog) {
        ModuleRecordCreateDialog(
            moduleId = moduleId,
            directoryTab = directoryTab,
            currentUser = currentUser,
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun SeizureCard(
    seizure: PassportSeizureEntity,
    onAdvanceStatus: (SeizureStatus) -> Unit,
    onOpenSignature: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CriticalCrimson.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${seizure.holderName} (${seizure.passportNumber})", style = MaterialTheme.typography.titleMedium)
                Text(seizure.seizureNumber, style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily), color = SovereignGold)
            }
            Text("السبب: ${seizure.seizureReason} | الضابط: ${seizure.officerName}", style = MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SeizureStatus.entries) { st ->
                    FilterChip(
                        selected = seizure.status == st,
                        onClick = { onAdvanceStatus(st) },
                        label = { Text(st.arabicLabel, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onOpenSignature) {
                    Icon(Icons.Filled.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (seizure.signatureStrokes.isEmpty()) "توقيع التسليم" else "تم التوقيع (تعديل)")
                }
            }
        }
    }
}

@Composable
private fun SimpleRecordCard(
    title: String,
    badge: String,
    subtitle: String,
    badgeColor: androidx.compose.ui.graphics.Color
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Surface(color = badgeColor.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                    Text(badge, style = MaterialTheme.typography.labelSmall, color = badgeColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun PermitQrScannerPanel(
    onQrScanned: (String) -> Unit,
    onManualCodeSubmit: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    var manualCode by remember { mutableStateOf("") }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val future = ProcessCameraProvider.getInstance(ctx)
                            future.addListener({
                                val provider = future.get()
                                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                analysis.setAnalyzer(executor) { proxy ->
                                    val img = proxy.image
                                    if (img != null) {
                                        val input = InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees)
                                        scanner.process(input)
                                            .addOnSuccessListener { barcodes ->
                                                barcodes.firstOrNull()?.rawValue?.let { onQrScanned(it) }
                                            }
                                            .addOnCompleteListener { proxy.close() }
                                    } else {
                                        proxy.close()
                                    }
                                }
                                runCatching {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        }
                    )
                }
            } else {
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("تفعيل الكاميرا لمسح رمز QR")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("أو أدخل كود التصريح يدوياً") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { if (manualCode.isNotBlank()) onManualCodeSubmit(manualCode) }) {
                    Text("تحقق")
                }
            }
        }
    }
}
