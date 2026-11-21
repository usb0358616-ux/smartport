package com.example.ui.components

import android.graphics.Bitmap
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.domain.model.SignaturePoint
import com.example.domain.security.WatchlistCheckResult
import com.example.ui.theme.ClearanceEmerald
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.WarningAmber

fun canUseBiometric(activity: FragmentActivity): Boolean {
    val bm = BiometricManager.from(activity)
    return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
        BiometricManager.BIOMETRIC_SUCCESS
}

fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("لم تتطابق البصمة الحيوية")
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText("استخدام كلمة المرور")
        .build()
    prompt.authenticate(info)
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.16f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = accentColor
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PulsingSecurityAlertBanner(
    alertCount: Int,
    latestSummary: String,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "alert_pulse")
    val bgColor by transition.animateColor(
        initialValue = CriticalCrimson.copy(alpha = 0.18f),
        targetValue = CriticalCrimson.copy(alpha = 0.34f),
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("critical_alert_banner"),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, CriticalCrimson)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.GppBad,
                contentDescription = "تنبيه أمني حرج",
                tint = CriticalCrimson,
                modifier = Modifier.size(34.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تنبيه أمني حرج ($alertCount مطابقة غير معتمدة)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CriticalCrimson
                )
                Text(
                    text = latestSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Button(
                onClick = onReviewClick,
                modifier = Modifier.testTag("review_alert_button")
            ) {
                Text("مراجعة")
            }
        }
    }
}

@Composable
fun WatchlistResultCard(
    result: WatchlistCheckResult,
    modifier: Modifier = Modifier
) {
    val color = if (result.isMatch) CriticalCrimson else ClearanceEmerald
    val icon = if (result.isMatch) Icons.Filled.Warning else Icons.Filled.CheckCircle
    val title = if (result.isMatch) {
        "تحذير أمني: تم رصد ${result.matches.size} تطابق (${result.highestSeverity.arabicLabel})"
    } else {
        "التحقق الأمني الفوري: السجل نظيف ولا توجد قيود"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f)),
        border = BorderStroke(1.5.dp, color)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
            result.matches.forEach { hit ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${hit.sourceType} (${hit.referenceId}) — ${hit.matchedRecordName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = hit.matchReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningAmber
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SignaturePadDialog(
    title: String,
    initialStrokes: List<SignaturePoint>,
    onDismiss: () -> Unit,
    onConfirm: (List<SignaturePoint>) -> Unit
) {
    val points = remember { mutableStateListOf<SignaturePoint>().apply { addAll(initialStrokes) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "يرجى التوقيع بالإصبع داخل الإطار لإثبات الاستلام:",
                    style = MaterialTheme.typography.bodySmall
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    points.add(SignaturePoint(offset.x, offset.y, isNewStroke = true))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    points.add(
                                        SignaturePoint(
                                            change.position.x,
                                            change.position.y,
                                            isNewStroke = false
                                        )
                                    )
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val path = Path()
                        points.forEachIndexed { index, pt ->
                            val clampedX = pt.x.coerceIn(0f, size.width)
                            val clampedY = pt.y.coerceIn(0f, size.height)
                            if (index == 0 || pt.isNewStroke) {
                                path.moveTo(clampedX, clampedY)
                            } else {
                                path.lineTo(clampedX, clampedY)
                            }
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF0B1D33),
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(24f, size.height - 32f),
                            end = Offset(size.width - 24f, size.height - 32f),
                            strokeWidth = 2f
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(points.toList()) }) {
                Text("اعتماد التوقيع")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { points.clear() }) { Text("مسح") }
                OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
            }
        }
    )
}

@Composable
fun QrCodePreviewDialog(
    title: String,
    subtitle: String,
    bitmap: Bitmap?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (bitmap != null) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 4.dp
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "رمز التحقق QR",
                            modifier = Modifier
                                .padding(12.dp)
                                .size(210.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
