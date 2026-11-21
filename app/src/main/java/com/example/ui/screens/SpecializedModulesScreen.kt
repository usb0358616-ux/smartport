package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.ModuleIds
import com.example.ui.theme.ClearanceEmerald
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ModulesViewModel

private data class ModuleHubItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val count: Int
)

@Composable
fun SpecializedModulesScreen(
    viewModel: ModulesViewModel,
    onOpenModule: (String) -> Unit
) {
    val seizures by viewModel.seizures.collectAsStateWithLifecycle()
    val mofa by viewModel.mofaRequests.collectAsStateWithLifecycle()
    val residents by viewModel.foreignerResidents.collectAsStateWithLifecycle()
    val refugees by viewModel.refugeeRecords.collectAsStateWithLifecycle()
    val migrants by viewModel.undocumentedMigrants.collectAsStateWithLifecycle()
    val permits by viewModel.movementPermits.collectAsStateWithLifecycle()
    val reports by viewModel.securityReports.collectAsStateWithLifecycle()
    val agencies by viewModel.travelAgencies.collectAsStateWithLifecycle()
    val officers by viewModel.portOfficers.collectAsStateWithLifecycle()

    val items = listOf(
        ModuleHubItem(
            id = ModuleIds.SEIZURES,
            title = "محاضر الضبط والجوازات المحتجزة",
            subtitle = "تتبع الفحص والتزوير وتوقيع التسليم",
            icon = Icons.Filled.Gavel,
            accent = CriticalCrimson,
            count = seizures.size
        ),
        ModuleHubItem(
            id = ModuleIds.SECURITY_REPORTS,
            title = "بلاغات الضبط والتعميمات",
            subtitle = "قوائم المطلوبين وأوامر المنع الأمني",
            icon = Icons.Filled.Security,
            accent = WarningAmber,
            count = reports.count { it.isActive }
        ),
        ModuleHubItem(
            id = ModuleIds.PERMITS_QR,
            title = "تصاريح التنقل الداخلي (QR)",
            subtitle = "إصدار ومسح رموز التحقق للوفود",
            icon = Icons.Filled.QrCode2,
            accent = SovereignGold,
            count = permits.size
        ),
        ModuleHubItem(
            id = ModuleIds.MOFA_VISA,
            title = "مذكرات الخارجية والمنظمات",
            subtitle = "تأشيرات ورحلات UN / MSF / OSESGY",
            icon = Icons.Filled.FlightTakeoff,
            accent = TacticalCyan,
            count = mofa.size
        ),
        ModuleHubItem(
            id = ModuleIds.RESIDENTS,
            title = "الوافدون الأجانب والإقامات",
            subtitle = "متابعة صلاحية الإقامات والكفلاء",
            icon = Icons.Filled.Badge,
            accent = ClearanceEmerald,
            count = residents.size
        ),
        ModuleHubItem(
            id = ModuleIds.REFUGEES,
            title = "شؤون اللاجئين (UNHCR)",
            subtitle = "بطاقات المفوضية والتوزيع الجغرافي",
            icon = Icons.Filled.SupportAgent,
            accent = TacticalCyan,
            count = refugees.size
        ),
        ModuleHubItem(
            id = ModuleIds.MIGRANTS,
            title = "المهاجرون غير النظاميين",
            subtitle = "بلاغات التسلل والترحيل والإيواء",
            icon = Icons.Filled.DirectionsWalk,
            accent = WarningAmber,
            count = migrants.size
        ),
        ModuleHubItem(
            id = ModuleIds.AGENCIES_OFFICERS,
            title = "دليل الوكالات وضباط المنافذ",
            subtitle = "مكاتب السفر المعتمدة وكوادر المنافذ",
            icon = Icons.Filled.Business,
            accent = SovereignGold,
            count = agencies.size + officers.size
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("الوحدات التخصصية المتكاملة", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "اختر الوحدة لإدارة السجلات والمحاضر والتصاريح الميدانية",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 165.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { mod ->
                Card(
                    onClick = { onOpenModule(mod.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, mod.accent.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = mod.accent.copy(alpha = 0.16f)
                            ) {
                                Icon(
                                    imageVector = mod.icon,
                                    contentDescription = mod.title,
                                    tint = mod.accent,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(22.dp)
                                )
                            }
                            Text(
                                text = mod.count.toString(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = mod.accent
                            )
                        }
                        Text(text = mod.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = mod.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
