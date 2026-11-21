package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BorderRepository
import com.example.data.local.entities.PassportSeizureEntity
import com.example.data.local.entities.TravelerEntity
import com.example.data.local.entities.UserAccountEntity
import com.example.domain.model.SeizureStatus
import com.example.domain.ocr.ArabicTextNormalizer
import com.example.domain.ocr.PassportOcrAnalyzer
import com.example.domain.ocr.PassportScanResult
import com.example.domain.security.WatchlistCheckResult
import com.example.domain.security.WatchlistGuardEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerUiState(
    val isProcessing: Boolean = false,
    val liveAnalysisEnabled: Boolean = true,
    val scanResult: PassportScanResult? = null,
    val watchlistResult: WatchlistCheckResult? = null,
    val savedMessage: String? = null,
    val errorMessage: String? = null
)

class ScannerViewModel(
    private val repository: BorderRepository,
    private val watchlistGuard: WatchlistGuardEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var lastProcessedMrzKey: String = ""

    fun onLiveOcrTextDetected(rawText: String, actor: UserAccountEntity?) {
        if (!_uiState.value.liveAnalysisEnabled) return
        val parsed = PassportOcrAnalyzer.analyzeOcrText(rawText)
        if (!parsed.mrzDetected || parsed.passportNumber.isBlank()) return

        val key = "${parsed.passportNumber}|${parsed.birthDate}"
        if (key == lastProcessedMrzKey && _uiState.value.scanResult?.mrzDetected == true) return
        lastProcessedMrzKey = key

        viewModelScope.launch {
            val guard = watchlistGuard.evaluateIdentity(
                candidateName = parsed.fullLatinName,
                candidateDocNumber = parsed.passportNumber,
                candidateBirthDate = parsed.birthDate,
                sourceModule = "ماسح الكاميرا المباشر",
                actor = actor,
                recordAlertOnMatch = true,
                playSoundAndVibrate = true
            )
            _uiState.update {
                it.copy(
                    scanResult = parsed,
                    watchlistResult = guard,
                    liveAnalysisEnabled = false
                )
            }
        }
    }

    fun analyzeBitmap(bitmap: Bitmap, actor: UserAccountEntity?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null, savedMessage = null) }
            try {
                val rawText = PassportOcrAnalyzer.recognizeTextFromBitmap(bitmap)
                val parsed = PassportOcrAnalyzer.analyzeOcrText(rawText)
                val guard = watchlistGuard.evaluateIdentity(
                    candidateName = parsed.fullLatinName,
                    candidateDocNumber = parsed.passportNumber,
                    candidateBirthDate = parsed.birthDate,
                    sourceModule = "فحص صورة جواز",
                    actor = actor,
                    recordAlertOnMatch = true,
                    playSoundAndVibrate = true
                )
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        scanResult = parsed,
                        watchlistResult = guard,
                        liveAnalysisEnabled = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isProcessing = false, errorMessage = "تعذر تحليل الصورة: ${e.message}")
                }
            }
        }
    }

    fun saveScannedTraveler(
        arabicName: String,
        passportName: String,
        passportNumber: String,
        country: String,
        gender: String,
        birthDate: String,
        issueDate: String,
        expiryDate: String,
        placeOfIssue: String,
        destination: String,
        agency: String,
        travelAllowed: String,
        returnReason: String,
        actor: UserAccountEntity
    ) {
        viewModelScope.launch {
            val parts = arabicName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val traveler = TravelerEntity(
                firstName = parts.getOrElse(0) { "" },
                secondName = parts.getOrElse(1) { "" },
                thirdName = parts.getOrElse(2) { "" },
                fourthName = parts.getOrElse(3) { "" },
                lastName = if (parts.size > 4) parts.drop(4).joinToString(" ") else "",
                passportName = passportName.ifBlank { arabicName },
                idNumber = passportNumber.trim(),
                country = country.trim(),
                gender = gender.trim(),
                birthDate = ArabicTextNormalizer.normalizeDateString(birthDate),
                age = ArabicTextNormalizer.calculateAge(birthDate),
                issueDate = ArabicTextNormalizer.normalizeDateString(issueDate),
                expiryDate = ArabicTextNormalizer.normalizeDateString(expiryDate),
                placeOfIssue = placeOfIssue.trim(),
                passportStatus = if (ArabicTextNormalizer.isDateExpired(expiryDate)) "منتهي" else "صالح",
                destination = destination.trim(),
                agency = agency.trim(),
                center = actor.assignedPort,
                portName = actor.assignedPort,
                travelAllowed = travelAllowed,
                returnReason = returnReason.trim(),
                vettingResult = if (_uiState.value.watchlistResult?.isMatch == true) "تطابق قائمة مراقبة" else "سليم",
                investigatingOfficer = actor.fullName
            )
            val (_, merged) = repository.saveOrMergeTraveler(traveler, "ماسح الجوازات OCR/MRZ", actor)
            _uiState.update {
                it.copy(
                    savedMessage = if (merged) "تم دمج بيانات الجواز مع سجل المسافر السابق" else "تم تسجيل المسافر من الماسح بنجاح"
                )
            }
        }
    }

    fun createSeizureFromScan(
        holderName: String,
        passportNumber: String,
        nationality: String,
        reason: String,
        actor: UserAccountEntity
    ) {
        viewModelScope.launch {
            val seizure = PassportSeizureEntity(
                seizureNumber = "SZ-${System.currentTimeMillis() % 1_000_000}",
                holderName = holderName,
                passportNumber = passportNumber,
                nationality = nationality,
                seizureReason = reason.ifBlank { "اشتباه / تطابق قائمة مطلوبين أثناء الفحص الضوئي" },
                portName = actor.assignedPort,
                officerName = actor.fullName,
                status = SeizureStatus.NEW
            )
            repository.saveSeizure(seizure, actor)
            _uiState.update {
                it.copy(savedMessage = "تم تحرير محضر ضبط جواز (${seizure.seizureNumber}) فوراً")
            }
        }
    }

    fun resetScan() {
        lastProcessedMrzKey = ""
        _uiState.value = ScannerUiState(liveAnalysisEnabled = true)
    }

    fun resumeLiveScanning() {
        lastProcessedMrzKey = ""
        _uiState.update { it.copy(liveAnalysisEnabled = true, savedMessage = null) }
    }
}
