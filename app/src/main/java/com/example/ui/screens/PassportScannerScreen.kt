package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.UserAccountEntity
import com.example.ui.components.WatchlistResultCard
import com.example.ui.theme.ClearanceEmerald
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ScannerViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@Composable
fun PassportScannerScreen(
    currentUser: UserAccountEntity,
    viewModel: ScannerViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    viewModel.analyzeBitmap(bmp, currentUser)
                }
            }
        }
    }

    // Editable fields synced with scanResult
    var arabicName by remember { mutableStateOf("") }
    var passportName by remember { mutableStateOf("") }
    var passportNumber by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var placeOfIssue by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var agency by remember { mutableStateOf("") }
    var travelAllowed by remember { mutableStateOf("نعم") }
    var returnReason by remember { mutableStateOf("") }

    LaunchedEffect(state.scanResult, state.watchlistResult) {
        val scan = state.scanResult ?: return@LaunchedEffect
        passportName = scan.fullLatinName
        if (arabicName.isBlank()) arabicName = scan.fullLatinName
        passportNumber = scan.passportNumber
        country = scan.nationalityArabic.ifBlank { scan.issuingCountryArabic }
        gender = scan.gender
        birthDate = scan.birthDate
        issueDate = scan.issueDate
        expiryDate = scan.expiryDate
        placeOfIssue = scan.placeOfIssue
        if (state.watchlistResult?.isMatch == true) {
            travelAllowed = "لا"
            returnReason = state.watchlistResult?.matches?.firstOrNull()?.matchReason.orEmpty()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "الماسح الذكي للجوازات والوثائق (MRZ + OCR)",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "وجّه الكاميرا نحو سطر القراءة الآلية (MRZ) أسفل الجواز أو اختر صورة من المعرض.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Camera Preview or Permission Request Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        CameraMrzPreview(
                            liveEnabled = state.liveAnalysisEnabled,
                            onTextDetected = { raw -> viewModel.onLiveOcrTextDetected(raw, currentUser) }
                        )
                        // MRZ Target Frame Overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val boxTop = size.height * 0.62f
                            val boxBottom = size.height * 0.88f
                            val margin = size.width * 0.06f
                            drawRect(
                                color = if (state.liveAnalysisEnabled) SovereignGold else ClearanceEmerald,
                                topLeft = Offset(margin, boxTop),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width - margin * 2,
                                    boxBottom - boxTop
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                            )
                        }
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (state.liveAnalysisEnabled) "جاري المسح المباشر لسطر MRZ..." else "تم التقاط البيانات — جاهز للمراجعة",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = SovereignGold,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "يتطلب المسح المباشر إذن الكاميرا",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.testTag("grant_camera_button")
                        ) {
                            Text("تفعيل الكاميرا")
                        }
                    }
                }
            }
        }

        // Action Controls Row (Gallery Photo Picker + Resume/Reset)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_passport_photo_button")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("صورة من المعرض")
                }

                OutlinedButton(
                    onClick = {
                        arabicName = ""
                        passportName = ""
                        passportNumber = ""
                        country = ""
                        gender = ""
                        birthDate = ""
                        issueDate = ""
                        expiryDate = ""
                        placeOfIssue = ""
                        returnReason = ""
                        travelAllowed = "نعم"
                        viewModel.resetScan()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("مسح جديد")
                }
            }
        }

        if (state.isProcessing) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.size(10.dp))
                    Text("جاري تحليل الجواز ومطابقة قوائم المطلوبين...")
                }
            }
        }

        // Watchlist Guard Output
        state.watchlistResult?.let { guard ->
            item {
                WatchlistResultCard(result = guard)
            }
        }

        // MRZ Check Digits Verification Banner
        state.scanResult?.let { scan ->
            if (scan.mrzDetected) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (scan.checkDigitsValid) ClearanceEmerald else WarningAmber
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (scan.checkDigitsValid) ClearanceEmerald else WarningAmber
                                )
                                Text(
                                    text = if (scan.checkDigitsValid) {
                                        "فحص أرقام التحقق ICAO MRZ TD3: سليم ومطابق"
                                    } else {
                                        "تنبيه: عدم تطابق في أحد أرقام الفحص الآلية (Check Digits)"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (scan.checkDigitsValid) ClearanceEmerald else WarningAmber
                                )
                            }
                            Text(
                                text = "${scan.mrzLine1}\n${scan.mrzLine2}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (scan.age != null || scan.isExpired) {
                                Text(
                                    text = buildString {
                                        scan.age?.let { append("العمر المحسوب: $it سنة") }
                                        if (scan.isExpired) append("  |  ⚠ الجواز منتهي الصلاحية!")
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (scan.isExpired) CriticalCrimson else ClearanceEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!state.savedMessage.isNullOrBlank()) {
            item {
                Surface(
                    color = ClearanceEmerald.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ClearanceEmerald)
                        Text(state.savedMessage.orEmpty(), color = ClearanceEmerald)
                    }
                }
            }
        }

        // Editable Traveler Registration Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("بيانات المسافر المستخرجة (قابلة للتعديل)", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = arabicName,
                        onValueChange = { arabicName = it },
                        label = { Text("الاسم الكامل (عربي / لاتيني)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = passportName,
                        onValueChange = { passportName = it },
                        label = { Text("الاسم كما بالجواز (MRZ)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = passportNumber,
                            onValueChange = { passportNumber = it },
                            label = { Text("رقم الجواز") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = country,
                            onValueChange = { country = it },
                            label = { Text("الدولة / الجنسية") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = birthDate,
                            onValueChange = { birthDate = it },
                            label = { Text("تاريخ الميلاد") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it },
                            label = { Text("تاريخ الانتهاء") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = { gender = it },
                            label = { Text("الجنس") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = placeOfIssue,
                            onValueChange = { placeOfIssue = it },
                            label = { Text("مكان الإصدار") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            label = { Text("الوجهة") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = agency,
                            onValueChange = { agency = it },
                            label = { Text("الوكالة / المنظمة") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("السماح بالسفر:", style = MaterialTheme.typography.labelLarge)
                        FilterChip(
                            selected = travelAllowed == "نعم",
                            onClick = { travelAllowed = "نعم" },
                            label = { Text("نعم (سماح)") }
                        )
                        FilterChip(
                            selected = travelAllowed == "لا",
                            onClick = { travelAllowed = "لا" },
                            label = { Text("لا (منع/إرجاع)") }
                        )
                    }

                    if (travelAllowed == "لا") {
                        OutlinedTextField(
                            value = returnReason,
                            onValueChange = { returnReason = it },
                            label = { Text("سبب المنع / الإرجاع") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveScannedTraveler(
                                    arabicName = arabicName,
                                    passportName = passportName,
                                    passportNumber = passportNumber,
                                    country = country,
                                    gender = gender,
                                    birthDate = birthDate,
                                    issueDate = issueDate,
                                    expiryDate = expiryDate,
                                    placeOfIssue = placeOfIssue,
                                    destination = destination,
                                    agency = agency,
                                    travelAllowed = travelAllowed,
                                    returnReason = returnReason,
                                    actor = currentUser
                                )
                            },
                            enabled = passportNumber.isNotBlank() || arabicName.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_scanned_traveler_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("حفظ / دمج ذكي")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.createSeizureFromScan(
                                    holderName = arabicName.ifBlank { passportName },
                                    passportNumber = passportNumber,
                                    nationality = country,
                                    reason = returnReason,
                                    actor = currentUser
                                )
                            },
                            enabled = passportNumber.isNotBlank(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CriticalCrimson),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Gavel, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("ضبط الجواز")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraMrzPreview(
    liveEnabled: Boolean,
    onTextDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            recognizer.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && liveEnabled) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        recognizer.process(image)
                            .addOnSuccessListener { result ->
                                if (result.text.isNotBlank()) {
                                    onTextDetected(result.text)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }
                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
