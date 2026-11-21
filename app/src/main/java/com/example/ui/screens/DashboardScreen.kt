package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DashboardMetrics
import com.example.data.local.entities.UserAccountEntity
import com.example.data.local.entities.WatchlistAlertEntity
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.PulsingSecurityAlertBanner
import com.example.ui.theme.ClearanceEmerald
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SovereignNavyDeep
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    currentUser: UserAccountEntity,
    activePort: String,
    metrics: DashboardMetrics,
    activeAlerts: List<WatchlistAlertEntity>,
    onNavigateScanner: () -> Unit,
    onNavigateImport: () -> Unit,
    onNavigateTravelers: () -> Unit,
    onNavigatePermitQr: () -> Unit,
    onNavigateSecurityReports: () -> Unit,
    onNavigateSeizures: () -> Unit,
    onAcknowledgeAlert: (Long) -> Unit
) {
    val timeFmt = SimpleDateFormat("HH:mm dd/MM", Locale.US)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Sovereign Operations Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_port_banner),
                    contentDescription = "بوابة المنفذ الحدودي الذكي",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    SovereignNavyDeep.copy(alpha = 0.92f),
                                    SovereignNavyDeep.copy(alpha = 0.65f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = ClearanceEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "متصل محلياً • مشفر 100%",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClearanceEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = SovereignGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = currentUser.role.arabicLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = SovereignGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "مرحباً، ${currentUser.fullName}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "المنفذ النشط: $activePort • الرقم الوظيفي: ${currentUser.badgeNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }

        // Critical Security Alert Banner when active Watchlist hits exist
        if (activeAlerts.isNotEmpty()) {
            item {
                val latest = activeAlerts.first()
                PulsingSecurityAlertBanner(
                    alertCount = activeAlerts.size,
                    latestSummary = "${latest.matchedName} (${latest.matchedDocumentNumber}) — ${latest.details}",
                    onReviewClick = { onAcknowledgeAlert(latest.id) }
                )
            }
        }

        // Quick Actions Grid
        item {
            Text(
                text = "الوصول السريع للعمليات الميدانية",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = "مسح جواز",
                    subtitle = "OCR + MRZ",
                    icon = Icons.Filled.DocumentScanner,
                    tint = SovereignGold,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_scan_button"),
                    onClick = onNavigateScanner
                )
                QuickActionTile(
                    title = "استيراد كشف",
                    subtitle = "Excel / PDF",
                    icon = Icons.Filled.FolderOpen,
                    tint = TacticalCyan,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_import_button"),
                    onClick = onNavigateImport
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = "بحث شامل",
                    subtitle = "سجلات المسافرين",
                    icon = Icons.Filled.Search,
                    tint = ClearanceEmerald,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_search_button"),
                    onClick = onNavigateTravelers
                )
                QuickActionTile(
                    title = "تحقق QR",
                    subtitle = "تصاريح الوفود",
                    icon = Icons.Filled.QrCodeScanner,
                    tint = WarningAmber,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_qr_button"),
                    onClick = onNavigatePermitQr
                )
            }
        }

        // Live KPIs
        item {
            Text(
                text = "الإحصائيات الفورية للمنفذ",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    title = "إجمالي المسافرين",
                    value = metrics.totalTravelers.toString(),
                    icon = Icons.Filled.Groups,
                    accentColor = TacticalCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateTravelers
                )
                KpiMetricCard(
                    title = "مسموح بالعبور",
                    value = metrics.allowedTravelers.toString(),
                    icon = Icons.Filled.CheckCircle,
                    accentColor = ClearanceEmerald,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateTravelers
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    title = "ممنوع / مرتجع",
                    value = metrics.blockedOrReturned.toString(),
                    icon = Icons.Filled.Block,
                    accentColor = CriticalCrimson,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateTravelers
                )
                KpiMetricCard(
                    title = "البلاغات النشطة",
                    value = metrics.activeSecurityReports.toString(),
                    icon = Icons.Filled.Shield,
                    accentColor = WarningAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateSecurityReports
                )
            }
            Spacer(Modifier.height(10.dp))
            KpiMetricCard(
                title = "محاضر المضبوطات والجوازات المحتجزة",
                value = metrics.totalSeizures.toString(),
                icon = Icons.Filled.Gavel,
                accentColor = SovereignGold,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateSeizures
            )
        }

        // Active Watchlist Alert Log Feed
        if (activeAlerts.isNotEmpty()) {
            item {
                Text(
                    text = "سجل تنبيهات المطلوبين النشطة",
                    style = MaterialTheme.typography.titleMedium,
                    color = CriticalCrimson
                )
            }
            items(activeAlerts, key = { it.id }) { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = CriticalCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${alert.matchedName} (${alert.matchedDocumentNumber})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = timeFmt.format(Date(alert.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${alert.sourceModule} • ${alert.details}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onAcknowledgeAlert(alert.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ClearanceEmerald)
                            ) {
                                Text("تأكيد المراجعة والإغلاق")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tint.copy(alpha = 0.16f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp)
                )
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
