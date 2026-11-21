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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.local.entities.UserAccountEntity
import com.example.domain.importer.StagingImportRow
import com.example.domain.model.DocumentCategory
import com.example.ui.theme.ClearanceEmerald
import com.example.ui.theme.CriticalCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TacticalCyan
import com.example.ui.viewmodel.ImportViewModel

@Composable
fun CentralImportScreen(
    currentUser: UserAccountEntity,
    viewModel: ImportViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val multiFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.parseSelectedFiles(uris, currentUser)
        }
    }

    var editingTarget by remember { mutableStateOf<Pair<Int, StagingImportRow>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("مركز الاستيراد والمعالجة الموحد", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "يدعم استيراد دفعات Excel (.xlsx / .xls)، CSV، ملفات PDF (النصية والمصورة)، والصور مع التصنيف التلقائي والمطابقة الأمنية.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // File picker & PDF rotation toolbar
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.RotateRight, contentDescription = null, tint = SovereignGold)
                        Text("تدوير صفحات PDF الممسوحة ضوئياً:", style = MaterialTheme.typography.labelLarge)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(0, 90, 180, 270)) { deg ->
                            FilterChip(
                                selected = state.pdfRotationDegrees == deg,
                                onClick = { viewModel.setPdfRotation(deg) },
                                label = { Text("${deg}°") }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            multiFilePicker.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "application/pdf",
                                    "image/*"
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("select_import_files_button")
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("اختيار ملفات متعددة للاستيراد (Excel / PDF / CSV / صور)")
                    }

                    if (state.isParsing || state.isCommitting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (!state.statusMessage.isNullOrBlank()) {
            item {
                Surface(
                    color = ClearanceEmerald.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ClearanceEmerald)
                        Text(state.statusMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = ClearanceEmerald)
                    }
                }
            }
        }

        if (state.watchlistHitsCount > 0) {
            item {
                Surface(
                    color = CriticalCrimson.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = CriticalCrimson)
                        Text(
                            text = "تحذير أمني: تم رصد ${state.watchlistHitsCount} تطابق مع قوائم المطلوبين/المنع داخل الكشوفات المستوردة!",
                            style = MaterialTheme.typography.labelLarge,
                            color = CriticalCrimson
                        )
                    }
                }
            }
        }

        // Parsed Batches Interactive Review Staging
        itemsIndexed(state.batches) { batchIndex, batch ->
            var categoryMenuExpanded by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.35f))
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(batch.fileName, style = MaterialTheme.typography.titleMedium)
                            if (batch.extractedMetadata.isNotBlank()) {
                                Text(
                                    text = batch.extractedMetadata,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TacticalCyan
                                )
                            }
                        }
                        Box {
                            OutlinedButton(onClick = { categoryMenuExpanded = true }) {
                                Text(batch.detectedCategory.arabicLabel, style = MaterialTheme.typography.labelSmall)
                            }
                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false }
                            ) {
                                DocumentCategory.entries.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.arabicLabel) },
                                        onClick = {
                                            viewModel.updateBatchCategory(batchIndex, cat)
                                            categoryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (!batch.errorMessage.isNullOrBlank()) {
                        Text(
                            text = batch.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = CriticalCrimson
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معاينة ومراجعة السجلات (${batch.rows.size})",
                            style = MaterialTheme.typography.labelLarge
                        )
                        TextButton(onClick = { viewModel.addManualStagingRow(batchIndex) }) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("إضافة صف")
                        }
                    }

                    batch.rows.forEach { row ->
                        val hasAlert = !row.watchlistWarning.isNullOrBlank()
                        Surface(
                            color = if (hasAlert) CriticalCrimson.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = row.fullName.ifBlank { "(بدون اسم — اضغط تعديل)" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "وثيقة: ${row.documentNumber.ifBlank { "-" }} | ميلاد: ${row.birthDate.ifBlank { "-" }} | مسموح: ${row.travelAllowed}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily)
                                    )
                                    if (hasAlert) {
                                        Text(
                                            text = "⚠ ${row.watchlistWarning}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CriticalCrimson
                                        )
                                    }
                                }
                                IconButton(onClick = { editingTarget = batchIndex to row }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "تعديل الصف")
                                }
                                IconButton(onClick = { viewModel.deleteStagingRow(batchIndex, row.tempId) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف الصف", tint = CriticalCrimson)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.batches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.commitAllBatches(currentUser) },
                        enabled = !state.isCommitting,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("commit_import_button")
                    ) {
                        Text("اعتماد وترحيل الكل (مع الدمج الذكي)")
                    }
                    OutlinedButton(
                        onClick = viewModel::clearBatches,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }

    editingTarget?.let { (bIdx, row) ->
        StagingRowEditorDialog(
            row = row,
            onDismiss = { editingTarget = null },
            onSave = { edited ->
                viewModel.updateStagingRow(bIdx, edited)
                editingTarget = null
            }
        )
    }
}

@Composable
private fun StagingRowEditorDialog(
    row: StagingImportRow,
    onDismiss: () -> Unit,
    onSave: (StagingImportRow) -> Unit
) {
    var name by remember { mutableStateOf(row.fullName) }
    var doc by remember { mutableStateOf(row.documentNumber) }
    var nationality by remember { mutableStateOf(row.nationality) }
    var birthDate by remember { mutableStateOf(row.birthDate) }
    var expiryDate by remember { mutableStateOf(row.expiryDate) }
    var agency by remember { mutableStateOf(row.agencyOrOrg) }
    var destination by remember { mutableStateOf(row.destination) }
    var allowed by remember { mutableStateOf(row.travelAllowed) }
    var notes by remember { mutableStateOf(row.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل السجل قبل الترحيل النهائي") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("الاسم الكامل") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(doc, { doc = it }, label = { Text("رقم الجواز / الوثيقة") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(nationality, { nationality = it }, label = { Text("الجنسية") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(birthDate, { birthDate = it }, label = { Text("تاريخ الميلاد") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(expiryDate, { expiryDate = it }, label = { Text("تاريخ الانتهاء") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(agency, { agency = it }, label = { Text("الوكالة / المنظمة") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(destination, { destination = it }, label = { Text("الوجهة / الخط الجوي") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("السماح بالسفر:")
                    FilterChip(selected = allowed == "نعم", onClick = { allowed = "نعم" }, label = { Text("نعم") })
                    FilterChip(selected = allowed == "لا", onClick = { allowed = "لا" }, label = { Text("لا") })
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        row.copy(
                            fullName = name,
                            passportName = name,
                            documentNumber = doc,
                            nationality = nationality,
                            birthDate = birthDate,
                            expiryDate = expiryDate,
                            agencyOrOrg = agency,
                            destination = destination,
                            travelAllowed = allowed,
                            notes = notes
                        )
                    )
                }
            ) { Text("حفظ التعديل") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
