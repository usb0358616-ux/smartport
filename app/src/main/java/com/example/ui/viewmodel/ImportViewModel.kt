package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BorderRepository
import com.example.data.local.entities.UserAccountEntity
import com.example.domain.importer.ExcelExportEngine
import com.example.domain.importer.ParsedImportBatch
import com.example.domain.importer.StagingImportRow
import com.example.domain.importer.UnifiedDocumentParser
import com.example.domain.model.DocumentCategory
import com.example.domain.security.WatchlistGuardEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportUiState(
    val isParsing: Boolean = false,
    val isCommitting: Boolean = false,
    val pdfRotationDegrees: Int = 0,
    val batches: List<ParsedImportBatch> = emptyList(),
    val watchlistHitsCount: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class ImportViewModel(
    private val parser: UnifiedDocumentParser,
    private val repository: BorderRepository,
    private val watchlistGuard: WatchlistGuardEngine,
    private val exportEngine: ExcelExportEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun setPdfRotation(degrees: Int) {
        _uiState.update { it.copy(pdfRotationDegrees = degrees) }
    }

    fun parseSelectedFiles(uris: List<Uri>, actor: UserAccountEntity?) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isParsing = true, errorMessage = null, statusMessage = null) }
            val rawBatches = parser.parseFiles(uris, _uiState.value.pdfRotationDegrees)

            // Snapshot watchlist data once and evaluate every imported row
            val reports = repository.getActiveSecurityReportsOnce()
            val travelers = repository.getAllTravelersOnce()
            val seizures = repository.getAllSeizuresOnce()

            var hits = 0
            val checkedBatches = rawBatches.map { batch ->
                val checkedRows = batch.rows.map { row ->
                    val check = watchlistGuard.checkAgainstSnapshots(
                        candidateName = row.fullName,
                        candidateDocNumber = row.documentNumber,
                        candidateBirthDate = row.birthDate,
                        reports = reports,
                        travelers = travelers,
                        seizures = seizures
                    )
                    if (check.isMatch) {
                        hits++
                        if (actor != null) {
                            watchlistGuard.evaluateIdentity(
                                candidateName = row.fullName,
                                candidateDocNumber = row.documentNumber,
                                candidateBirthDate = row.birthDate,
                                sourceModule = "مركز الاستيراد (${batch.fileName})",
                                actor = actor,
                                recordAlertOnMatch = true,
                                playSoundAndVibrate = hits == 1
                            )
                        }
                        row.copy(
                            travelAllowed = "لا",
                            watchlistWarning = check.matches.firstOrNull()?.matchReason
                        )
                    } else {
                        row
                    }
                }
                batch.copy(rows = checkedRows)
            }

            _uiState.update {
                it.copy(
                    isParsing = false,
                    batches = checkedBatches,
                    watchlistHitsCount = hits,
                    statusMessage = "تم تحليل ${checkedBatches.size} ملف وإجمالي ${checkedBatches.sumOf { b -> b.rows.size }} سجل للمراجعة"
                )
            }
        }
    }

    fun updateBatchCategory(batchIndex: Int, category: DocumentCategory) {
        _uiState.update { state ->
            val updated = state.batches.toMutableList()
            if (batchIndex in updated.indices) {
                updated[batchIndex] = updated[batchIndex].copy(detectedCategory = category)
            }
            state.copy(batches = updated)
        }
    }

    fun updateStagingRow(batchIndex: Int, editedRow: StagingImportRow) {
        _uiState.update { state ->
            val updatedBatches = state.batches.toMutableList()
            if (batchIndex in updatedBatches.indices) {
                val batch = updatedBatches[batchIndex]
                val newRows = batch.rows.map { if (it.tempId == editedRow.tempId) editedRow else it }
                updatedBatches[batchIndex] = batch.copy(rows = newRows)
            }
            state.copy(batches = updatedBatches)
        }
    }

    fun deleteStagingRow(batchIndex: Int, tempId: String) {
        _uiState.update { state ->
            val updatedBatches = state.batches.toMutableList()
            if (batchIndex in updatedBatches.indices) {
                val batch = updatedBatches[batchIndex]
                updatedBatches[batchIndex] = batch.copy(rows = batch.rows.filterNot { it.tempId == tempId })
            }
            state.copy(batches = updatedBatches)
        }
    }

    fun addManualStagingRow(batchIndex: Int) {
        _uiState.update { state ->
            val updatedBatches = state.batches.toMutableList()
            if (batchIndex in updatedBatches.indices) {
                val batch = updatedBatches[batchIndex]
                updatedBatches[batchIndex] = batch.copy(rows = batch.rows + StagingImportRow())
            }
            state.copy(batches = updatedBatches)
        }
    }

    fun commitAllBatches(actor: UserAccountEntity) {
        val currentBatches = _uiState.value.batches
        if (currentBatches.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCommitting = true, errorMessage = null) }
            var committedCount = 0
            var mergedCount = 0

            for (batch in currentBatches) {
                for (row in batch.rows) {
                    if (row.fullName.isBlank() && row.documentNumber.isBlank()) continue
                    when (batch.detectedCategory) {
                        DocumentCategory.TRAVELERS -> {
                            val entity = parser.toTravelerEntity(row, actor.assignedPort, actor.fullName)
                            val (_, merged) = repository.saveOrMergeTraveler(entity, "استيراد: ${batch.fileName}", actor)
                            if (merged) mergedCount++
                        }
                        DocumentCategory.MOFA_VISA -> {
                            repository.saveMofaRequest(parser.toMofaVisaRequest(row, actor.assignedPort), actor)
                        }
                        DocumentCategory.HUMANITARIAN_MANIFEST -> {
                            // Save in MOFA/Humanitarian requests AND in Travelers via Smart Merge
                            repository.saveMofaRequest(parser.toMofaVisaRequest(row, actor.assignedPort), actor)
                            val traveler = parser.toTravelerEntity(row, actor.assignedPort, actor.fullName)
                            val (_, merged) = repository.saveOrMergeTraveler(traveler, "مانيفست: ${batch.fileName}", actor)
                            if (merged) mergedCount++
                        }
                        DocumentCategory.TRANSIT_FACILITATION -> {
                            repository.saveTravelAgency(parser.toAgencySummary(row, actor.assignedPort), actor)
                        }
                        DocumentCategory.SECURITY_REPORT -> {
                            repository.saveSecurityReport(parser.toSecurityReport(row, actor.fullName), actor)
                        }
                        DocumentCategory.FOREIGNER_RESIDENT -> {
                            repository.saveForeignerResident(parser.toForeignerResident(row, actor.assignedPort), actor)
                        }
                        DocumentCategory.REFUGEE -> {
                            repository.saveRefugeeRecord(parser.toRefugeeRecord(row, actor.assignedPort), actor)
                        }
                    }
                    committedCount++
                }
            }

            repository.appendAuditLog(
                actionType = "BATCH_IMPORT_COMMITTED",
                entityType = "CentralImport",
                entityId = "${currentBatches.size}",
                summary = "ترحيل دفعة استيراد: $committedCount سجل (دمج ذكي: $mergedCount)",
                details = currentBatches.joinToString(" | ") { "${it.fileName} -> ${it.detectedCategory.arabicLabel}" },
                user = actor
            )

            _uiState.update {
                it.copy(
                    isCommitting = false,
                    batches = emptyList(),
                    watchlistHitsCount = 0,
                    statusMessage = "تم ترحيل $committedCount سجل بنجاح (منها $mergedCount سجل بالدمج الذكي)"
                )
            }
        }
    }

    fun clearBatches() {
        _uiState.value = ImportUiState()
    }

    fun exportAuditLogs(uri: Uri) {
        viewModelScope.launch {
            val logs = repository.getAllAuditLogsOnce()
            exportEngine.exportAuditLogsXlsx(uri, logs)
                .onSuccess { count -> _uiState.update { it.copy(statusMessage = "تم تصدير $count سجل تدقيق إلى Excel") } }
                .onFailure { err -> _uiState.update { it.copy(errorMessage = err.message) } }
        }
    }
}
