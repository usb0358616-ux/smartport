package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.TravelerEntity
import com.example.data.local.entities.UserAccountEntity
import com.example.ui.theme.ClearanceEmerald
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.TravelerFilter
import com.example.ui.viewmodel.TravelersViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TravelersScreen(
    currentUser: UserAccountEntity,
    viewModel: TravelersViewModel
) {
    val travelers by viewModel.filteredTravelers.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.filter.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    var selectedDetail by remember { mutableStateOf<TravelerEntity?>(null) }
    var editingTraveler by remember { mutableStateOf<TravelerEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToXlsx(uri)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_traveler_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة مسافر جديد")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل المسافرين (${travelers.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedButton(
                    onClick = { exportLauncher.launch("travelers_export.xlsx") },
                    enabled = travelers.isNotEmpty()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("تصدير Excel")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("بحث بالاسم، رقم الجواز، الهوية، المنفذ، أو الوكالة...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("traveler_search_input")
            )

            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TravelerFilter.entries) { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.arabicLabel) }
                    )
                }
            }

            if (!feedback.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = ClearanceEmerald.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(feedback.orEmpty(), style = MaterialTheme.typography.bodySmall, color = ClearanceEmerald)
                        TextButton(onClick = viewModel::clearFeedback) { Text("إغلاق") }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (travelers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.PersonSearch,
                        contentDescription = null,
                        tint = SovereignGold,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("لا توجد سجلات مسافرين مطابقة حالياً", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "ابدأ بمسح جواز بالكاميرا، أو استيراد كشوفات Excel/PDF، أو إضافة مسافر يدوياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(travelers, key = { it.id }) { traveler ->
                        val isBlocked = traveler.travelAllowed == "لا" || traveler.returnReason.isNotBlank()
                        val badgeColor = if (isBlocked) CriticalCrimson else ClearanceEmerald
                        Card(
                            onClick = { selectedDetail = traveler },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
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
                                    Text(
                                        text = traveler.fullArabicName.ifBlank { "مسافر بدون اسم" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isBlocked) "ممنوع / إرتجاع" else "مسموح بالعبور",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${traveler.idType}: ${traveler.idNumber}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = SovereignGold
                                    )
                                    Text(
                                        text = traveler.country.ifBlank { "-" },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المنفذ: ${traveler.portName.ifBlank { traveler.center.ifBlank { "-" } }} • الوكالة: ${traveler.agency.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (traveler.mergeHistory.isNotEmpty()) {
                                        Icon(
                                            Icons.Filled.History,
                                            contentDescription = "سجل دمج ذكي",
                                            tint = WarningAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedDetail?.let { traveler ->
        TravelerDetailDialog(
            traveler = traveler,
            canDelete = currentUser.role.canDeleteRecords,
            onDismiss = { selectedDetail = null },
            onEdit = {
                selectedDetail = null
                editingTraveler = traveler
            },
            onDelete = {
                viewModel.deleteTraveler(traveler, currentUser)
                selectedDetail = null
            }
        )
    }

    if (showAddDialog || editingTraveler != null) {
        TravelerEditorDialog(
            initial = editingTraveler ?: TravelerEntity(
                center = currentUser.assignedPort,
                portName = currentUser.assignedPort,
                investigatingOfficer = currentUser.fullName
            ),
            canApproveBan = currentUser.role.canApproveTravelBan,
            onDismiss = {
                showAddDialog = false
                editingTraveler = null
            },
            onSave = { updated ->
                viewModel.saveTraveler(updated, currentUser)
                showAddDialog = false
                editingTraveler = null
            }
        )
    }
}

@Composable
private fun TravelerDetailDialog(
    traveler: TravelerEntity,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(traveler.fullArabicName, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DetailRow("الاسم بالجواز", traveler.passportName)
                DetailRow("رقم الوثيقة (${traveler.idType})", traveler.idNumber)
                DetailRow("حالة الجواز", traveler.passportStatus)
                DetailRow("الرقم الوطني", traveler.nationalId)
                DetailRow("الجنس / الميلاد / العمر", "${traveler.gender} • ${traveler.birthDate} (${traveler.age ?: "-"} سنة)")
                DetailRow("الدولة / المحافظة", "${traveler.country} / ${traveler.governorate}")
                DetailRow("المهنة / الهاتف", "${traveler.profession} • ${traveler.phone}")
                DetailRow("المركز / المنفذ", "${traveler.center} / ${traveler.portName}")
                DetailRow("الوكالة / التأشيرة", "${traveler.agency} • ${traveler.visaType}")
                DetailRow("الوجهة / سبب الزيارة", "${traveler.destination} • ${traveler.visitReason}")
                DetailRow("تاريخ الإصدار / الانتهاء", "${traveler.issueDate} -> ${traveler.expiryDate}")
                DetailRow("مكان الإصدار / رقم الطلب", "${traveler.placeOfIssue} • ${traveler.requestNumber}")
                DetailRow("تاريخ الاستلام / المغادرة", "${traveler.receiveDate} -> ${traveler.exitDate}")
                DetailRow("السماح بالسفر", traveler.travelAllowed)
                DetailRow("سبب الإرجاع", traveler.returnReason)
                DetailRow("نتيجة التدقيق", traveler.vettingResult)
                DetailRow("ضابط التحقيق", traveler.investigatingOfficer)
                DetailRow("معلومات إضافية", traveler.additionalInfo)

                if (traveler.mergeHistory.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Text("سجل الدمج الذكي والتعديلات:", style = MaterialTheme.typography.titleSmall, color = SovereignGold)
                    traveler.mergeHistory.forEach { rec ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "${dateFmt.format(Date(rec.timestamp))} • ${rec.officerName} (${rec.source})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = rec.changedFields.joinToString("، "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف السجل", tint = CriticalCrimson)
                    }
                }
                Button(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("تعديل")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank() || value == " • " || value == " / " || value == " -> ") return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$label:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TravelerEditorDialog(
    initial: TravelerEntity,
    canApproveBan: Boolean,
    onDismiss: () -> Unit,
    onSave: (TravelerEntity) -> Unit
) {
    var first by remember { mutableStateOf(initial.firstName) }
    var second by remember { mutableStateOf(initial.secondName) }
    var third by remember { mutableStateOf(initial.thirdName) }
    var last by remember { mutableStateOf(initial.lastName) }
    var passportName by remember { mutableStateOf(initial.passportName) }
    var idNumber by remember { mutableStateOf(initial.idNumber) }
    var country by remember { mutableStateOf(initial.country) }
    var birthDate by remember { mutableStateOf(initial.birthDate) }
    var expiryDate by remember { mutableStateOf(initial.expiryDate) }
    var portName by remember { mutableStateOf(initial.portName) }
    var agency by remember { mutableStateOf(initial.agency) }
    var destination by remember { mutableStateOf(initial.destination) }
    var phone by remember { mutableStateOf(initial.phone) }
    var travelAllowed by remember { mutableStateOf(initial.travelAllowed) }
    var returnReason by remember { mutableStateOf(initial.returnReason) }
    var notes by remember { mutableStateOf(initial.additionalInfo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "إضافة مسافر جديد" else "تعديل بيانات المسافر") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(first, { first = it }, label = { Text("الأول") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(second, { second = it }, label = { Text("الثاني") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(third, { third = it }, label = { Text("الثالث") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(last, { last = it }, label = { Text("اللقب") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(passportName, { passportName = it }, label = { Text("الاسم كما بالجواز") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(idNumber, { idNumber = it }, label = { Text("رقم الجواز/الهوية") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(country, { country = it }, label = { Text("الدولة") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(birthDate, { birthDate = it }, label = { Text("تاريخ الميلاد") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(expiryDate, { expiryDate = it }, label = { Text("انتهاء الجواز") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(portName, { portName = it }, label = { Text("المنفذ") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(agency, { agency = it }, label = { Text("الوكالة") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(destination, { destination = it }, label = { Text("الوجهة") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(phone, { phone = it }, label = { Text("الهاتف") }, modifier = Modifier.weight(1f))
                }

                if (canApproveBan || initial.travelAllowed == "نعم") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("السماح بالسفر:", style = MaterialTheme.typography.labelLarge)
                        FilterChip(selected = travelAllowed == "نعم", onClick = { travelAllowed = "نعم" }, label = { Text("نعم") })
                        FilterChip(selected = travelAllowed == "لا", onClick = { travelAllowed = "لا" }, label = { Text("لا") })
                    }
                }
                if (travelAllowed == "لا") {
                    OutlinedTextField(returnReason, { returnReason = it }, label = { Text("سبب المنع / الإرجاع") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات إضافية") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            firstName = first,
                            secondName = second,
                            thirdName = third,
                            lastName = last,
                            passportName = passportName,
                            idNumber = idNumber,
                            country = country,
                            birthDate = birthDate,
                            expiryDate = expiryDate,
                            portName = portName,
                            center = portName,
                            agency = agency,
                            destination = destination,
                            phone = phone,
                            travelAllowed = travelAllowed,
                            returnReason = returnReason,
                            additionalInfo = notes
                        )
                    )
                },
                enabled = first.isNotBlank() || passportName.isNotBlank() || idNumber.isNotBlank()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
