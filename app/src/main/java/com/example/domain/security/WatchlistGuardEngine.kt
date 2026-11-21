package com.example.domain.security

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.local.BorderRepository
import com.example.data.local.entities.PassportSeizureEntity
import com.example.data.local.entities.SecurityReportEntity
import com.example.data.local.entities.TravelerEntity
import com.example.data.local.entities.UserAccountEntity
import com.example.data.local.entities.WatchlistAlertEntity
import com.example.domain.model.SecuritySeverity
import com.example.domain.model.SeizureStatus
import com.example.domain.ocr.ArabicTextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WatchlistMatchDetail(
    val sourceType: String,
    val referenceId: String,
    val matchedRecordName: String,
    val matchedDocument: String,
    val matchReason: String,
    val similarityScore: Double,
    val severity: SecuritySeverity
)

data class WatchlistCheckResult(
    val isMatch: Boolean,
    val highestSeverity: SecuritySeverity = SecuritySeverity.MEDIUM,
    val matches: List<WatchlistMatchDetail> = emptyList()
)

class WatchlistGuardEngine(
    private val context: Context,
    private val repository: BorderRepository
) {

    suspend fun evaluateIdentity(
        candidateName: String,
        candidateDocNumber: String,
        candidateBirthDate: String,
        sourceModule: String,
        actor: UserAccountEntity?,
        recordAlertOnMatch: Boolean = true,
        playSoundAndVibrate: Boolean = true
    ): WatchlistCheckResult = withContext(Dispatchers.IO) {
        val reports = repository.getActiveSecurityReportsOnce()
        val travelers = repository.getAllTravelersOnce()
        val seizures = repository.getAllSeizuresOnce()

        val result = checkAgainstSnapshots(
            candidateName = candidateName,
            candidateDocNumber = candidateDocNumber,
            candidateBirthDate = candidateBirthDate,
            reports = reports,
            travelers = travelers,
            seizures = seizures
        )

        if (result.isMatch && recordAlertOnMatch && actor != null) {
            val top = result.matches.first()
            repository.recordWatchlistAlert(
                alert = WatchlistAlertEntity(
                    sourceModule = sourceModule,
                    matchedName = candidateName.ifBlank { top.matchedRecordName },
                    matchedDocumentNumber = candidateDocNumber.ifBlank { top.matchedDocument },
                    matchedAgainstType = top.sourceType,
                    matchedCircularOrRecordId = top.referenceId,
                    matchScore = top.similarityScore,
                    severity = result.highestSeverity,
                    details = result.matches.joinToString(" | ") { "${it.sourceType}: ${it.matchReason}" },
                    officerName = actor.fullName,
                    portName = actor.assignedPort
                ),
                actor = actor
            )
            if (playSoundAndVibrate) {
                triggerSecurityAlertSignal()
            }
        }
        result
    }

    fun checkAgainstSnapshots(
        candidateName: String,
        candidateDocNumber: String,
        candidateBirthDate: String,
        reports: List<SecurityReportEntity>,
        travelers: List<TravelerEntity>,
        seizures: List<PassportSeizureEntity>
    ): WatchlistCheckResult {
        val matches = mutableListOf<WatchlistMatchDetail>()
        val normDoc = ArabicTextNormalizer.normalizeDocumentId(candidateDocNumber)
        val normDob = ArabicTextNormalizer.normalizeDateString(candidateBirthDate)

        // 1. Check active security reports & circulars
        for (report in reports) {
            if (!report.isActive) continue
            val reportDoc = ArabicTextNormalizer.normalizeDocumentId(report.passportOrIdNumber)
            val docHit = normDoc.isNotEmpty() && reportDoc.isNotEmpty() && normDoc == reportDoc
            val nameScore = if (candidateName.isNotBlank() && report.subjectName.isNotBlank()) {
                ArabicTextNormalizer.similarityScore(candidateName, report.subjectName)
            } else 0.0
            val dobMatch = normDob.isNotEmpty() &&
                ArabicTextNormalizer.normalizeDateString(report.birthDate) == normDob

            if (docHit || (nameScore >= 0.84 && (dobMatch || report.birthDate.isBlank()))) {
                val reason = buildString {
                    append(report.reportType.arabicLabel)
                    append(" (تعميم ${report.circularNumber})")
                    if (docHit) append(" — تطابق رقم الوثيقة")
                    else append(" — تشابه اسم ${(nameScore * 100).toInt()}%")
                    if (dobMatch) append(" وتاريخ الميلاد")
                }
                matches.add(
                    WatchlistMatchDetail(
                        sourceType = "تعميم أمني",
                        referenceId = report.circularNumber,
                        matchedRecordName = report.subjectName,
                        matchedDocument = report.passportOrIdNumber,
                        matchReason = reason,
                        similarityScore = if (docHit) 1.0 else nameScore,
                        severity = report.severity
                    )
                )
            }
        }

        // 2. Check travelers with travelAllowed == "لا" or prior returnReason
        for (trav in travelers) {
            val isBlocked = trav.travelAllowed == "لا"
            val hasReturn = trav.returnReason.isNotBlank()
            if (!isBlocked && !hasReturn) continue

            val travDoc = ArabicTextNormalizer.normalizeDocumentId(trav.idNumber)
            val travNatId = ArabicTextNormalizer.normalizeDocumentId(trav.nationalId)
            val docHit = normDoc.isNotEmpty() && (normDoc == travDoc || normDoc == travNatId)
            val nameScore = maxOf(
                ArabicTextNormalizer.similarityScore(candidateName, trav.fullArabicName),
                ArabicTextNormalizer.similarityScore(candidateName, trav.passportName)
            )
            val dobMatch = normDob.isNotEmpty() &&
                ArabicTextNormalizer.normalizeDateString(trav.birthDate) == normDob

            if (docHit || (nameScore >= 0.86 && (dobMatch || trav.birthDate.isBlank()))) {
                val severity = if (isBlocked) SecuritySeverity.CRITICAL else SecuritySeverity.HIGH
                val reason = buildString {
                    if (isBlocked) append("ممنوع من السفر") else append("حالة إرجاع سابقة: ${trav.returnReason}")
                    if (docHit) append(" — تطابق الوثيقة")
                    else append(" — تطابق اسم ${(nameScore * 100).toInt()}%")
                }
                matches.add(
                    WatchlistMatchDetail(
                        sourceType = if (isBlocked) "قائمة المنع" else "سجل الإرتجاع",
                        referenceId = "#${trav.id}",
                        matchedRecordName = trav.fullArabicName,
                        matchedDocument = trav.idNumber,
                        matchReason = reason,
                        similarityScore = if (docHit) 1.0 else nameScore,
                        severity = severity
                    )
                )
            }
        }

        // 3. Check seized or forged passports
        for (seizure in seizures) {
            val sDoc = ArabicTextNormalizer.normalizeDocumentId(seizure.passportNumber)
            val docHit = normDoc.isNotEmpty() && normDoc == sDoc
            val nameScore = if (candidateName.isNotBlank()) {
                ArabicTextNormalizer.similarityScore(candidateName, seizure.holderName)
            } else 0.0

            if (docHit || nameScore >= 0.90) {
                val isForged = seizure.status == SeizureStatus.FORGED
                matches.add(
                    WatchlistMatchDetail(
                        sourceType = "جواز مضبوط",
                        referenceId = seizure.seizureNumber,
                        matchedRecordName = seizure.holderName,
                        matchedDocument = seizure.passportNumber,
                        matchReason = "محضر ضبط (${seizure.status.arabicLabel}): ${seizure.seizureReason}",
                        similarityScore = if (docHit) 1.0 else nameScore,
                        severity = if (isForged) SecuritySeverity.CRITICAL else SecuritySeverity.HIGH
                    )
                )
            }
        }

        if (matches.isEmpty()) return WatchlistCheckResult(isMatch = false)

        val highest = when {
            matches.any { it.severity == SecuritySeverity.CRITICAL } -> SecuritySeverity.CRITICAL
            matches.any { it.severity == SecuritySeverity.HIGH } -> SecuritySeverity.HIGH
            else -> SecuritySeverity.MEDIUM
        }
        return WatchlistCheckResult(
            isMatch = true,
            highestSeverity = highest,
            matches = matches.sortedByDescending { it.similarityScore }
        )
    }

    fun triggerSecurityAlertSignal() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 90)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 650)
        } catch (_: Exception) {
        }
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 220, 120, 220, 120, 350), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 220, 120, 220), -1)
                }
            }
        } catch (_: Exception) {
        }
    }
}
