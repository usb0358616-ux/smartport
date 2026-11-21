package com.example.domain.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PassportScanResult(
    val rawText: String = "",
    val mrzDetected: Boolean = false,
    val mrzLine1: String = "",
    val mrzLine2: String = "",
    val documentType: String = "P",
    val issuingCountryCode: String = "",
    val issuingCountryArabic: String = "",
    val nationalityCode: String = "",
    val nationalityArabic: String = "",
    val surname: String = "",
    val givenNames: String = "",
    val fullLatinName: String = "",
    val passportNumber: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val age: Int? = null,
    val expiryDate: String = "",
    val isExpired: Boolean = false,
    val personalNumber: String = "",
    val issueDate: String = "",
    val placeOfIssue: String = "",
    val checkDigitsValid: Boolean = false,
    val passportCheckValid: Boolean = false,
    val birthCheckValid: Boolean = false,
    val expiryCheckValid: Boolean = false,
    val compositeCheckValid: Boolean = false
)

object PassportOcrAnalyzer {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return recognizeText(image)
    }

    suspend fun recognizeText(image: InputImage): String =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    cont.resume(visionText.text)
                }
                .addOnFailureListener { exc ->
                    cont.resumeWithException(exc)
                }
        }

    fun analyzeOcrText(rawText: String): PassportScanResult {
        val normalizedRaw = ArabicTextNormalizer.normalizeDigits(rawText)
        val lines = normalizedRaw
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Extract candidate MRZ lines (TD3: 44 chars starting with P< or V< or I<, or dense '<' lines)
        val mrzCandidates = lines.map { sanitizeMrzCandidate(it) }
            .filter { it.length >= 28 && (it.contains('<') || it.matches(Regex("^[A-Z0-9<]{30,48}$"))) }

        // Try TD3 pair (2 lines ~44 chars where line 1 starts with P/V/I/A/C)
        val td3Pair = findTd3Pair(mrzCandidates)
        if (td3Pair != null) {
            return parseTd3(td3Pair.first, td3Pair.second, normalizedRaw, lines)
        }

        // Try TD2 pair (2 lines ~36 chars)
        val td2Pair = findTd2Pair(mrzCandidates)
        if (td2Pair != null) {
            return parseTd2(td2Pair.first, td2Pair.second, normalizedRaw, lines)
        }

        // Visual Inspection Zone (VIZ) fallback parser when MRZ isn't clearly framed
        return parseVisualZoneFallback(normalizedRaw, lines)
    }

    private fun sanitizeMrzCandidate(line: String): String {
        return line.uppercase(Locale.ROOT)
            .replace(" ", "")
            .replace("«", "<<")
            .replace("‹", "<")
            .replace("(", "<")
            .replace(")", "<")
            .replace("{", "<")
            .replace("}", "<")
            .replace(Regex("[^A-Z0-9<]"), "")
    }

    private fun findTd3Pair(candidates: List<String>): Pair<String, String>? {
        for (i in 0 until candidates.size - 1) {
            val l1 = candidates[i]
            val l2 = candidates[i + 1]
            if (l1.length in 38..48 && l2.length in 38..48 &&
                (l1.startsWith("P") || l1.startsWith("V") || l1.startsWith("I") || l1.contains("<<"))
            ) {
                return l1.padEnd(44, '<').take(44) to l2.padEnd(44, '<').take(44)
            }
        }
        // Check adjacent lines even if split by another line
        val line1 = candidates.firstOrNull { it.length in 38..48 && (it.startsWith("P<") || it.contains("<<")) }
        val line2 = candidates.firstOrNull { it != line1 && it.length in 38..48 && it.count { c -> c.isDigit() } >= 10 }
        if (line1 != null && line2 != null) {
            return line1.padEnd(44, '<').take(44) to line2.padEnd(44, '<').take(44)
        }
        return null
    }

    private fun findTd2Pair(candidates: List<String>): Pair<String, String>? {
        for (i in 0 until candidates.size - 1) {
            val l1 = candidates[i]
            val l2 = candidates[i + 1]
            if (l1.length in 32..37 && l2.length in 32..37) {
                return l1.padEnd(36, '<').take(36) to l2.padEnd(36, '<').take(36)
            }
        }
        return null
    }

    private fun parseTd3(
        rawL1: String,
        rawL2: String,
        rawText: String,
        lines: List<String>
    ): PassportScanResult {
        val l1 = rawL1.padEnd(44, '<').take(44)
        val l2 = rawL2.padEnd(44, '<').take(44)

        val docType = l1.substring(0, 2).replace("<", "").ifBlank { "P" }
        val issuingCode = fixAlphaZone(l1.substring(2, 5)).replace("<", "")
        val nameSection = l1.substring(5, 44)
        val nameParts = nameSection.split("<<", limit = 2)
        val surname = fixAlphaZone(nameParts.getOrElse(0) { "" }).replace("<", " ").trim()
        val givenNames = fixAlphaZone(nameParts.getOrElse(1) { "" }).replace("<", " ").replace(Regex("\\s+"), " ").trim()
        val fullLatin = listOf(givenNames, surname).filter { it.isNotBlank() }.joinToString(" ")

        // Line 2 TD3 layout:
        // 0..8: Passport number (9)
        // 9: Check digit
        // 10..12: Nationality (3)
        // 13..18: Birth date YYMMDD (6)
        // 19: Check digit
        // 20: Sex (M/F/<)
        // 21..26: Expiry date YYMMDD (6)
        // 27: Check digit
        // 28..41: Personal number (14)
        // 42: Personal check digit
        // 43: Composite check digit
        val passportField = l2.substring(0, 9)
        val passportCheckChar = l2[9]
        val passportNumber = passportField.replace("<", "")

        val natCode = fixAlphaZone(l2.substring(10, 13)).replace("<", "")
        val dobField = fixNumericZone(l2.substring(13, 19))
        val dobCheckChar = fixNumericZone(l2[19].toString())[0]
        val sexChar = l2[20]
        val expField = fixNumericZone(l2.substring(21, 27))
        val expCheckChar = fixNumericZone(l2[27].toString())[0]
        val personalField = l2.substring(28, 42)
        val personalNumber = personalField.replace("<", "")
        val personalCheckChar = l2[42]
        val compositeCheckChar = fixNumericZone(l2[43].toString())[0]

        val pCheckOk = verifyCheckDigit(passportField, passportCheckChar)
        val dobCheckOk = verifyCheckDigit(dobField, dobCheckChar)
        val expCheckOk = verifyCheckDigit(expField, expCheckChar)
        val compositeData = l2.substring(0, 10) + l2.substring(13, 20) + l2.substring(21, 43)
        val compCheckOk = verifyCheckDigit(compositeData, compositeCheckChar)

        val dobFormatted = ArabicTextNormalizer.parseMrzDate(dobField, isBirthDate = true)
        val expFormatted = ArabicTextNormalizer.parseMrzDate(expField, isBirthDate = false)
        val age = ArabicTextNormalizer.calculateAge(dobFormatted)
        val expired = ArabicTextNormalizer.isDateExpired(expFormatted)

        val genderArabic = when (sexChar) {
            'M' -> "ذكر"
            'F' -> "أنثى"
            else -> ""
        }

        val visualIssueInfo = extractIssueFieldsFromVisualZone(lines)

        return PassportScanResult(
            rawText = rawText,
            mrzDetected = true,
            mrzLine1 = l1,
            mrzLine2 = l2,
            documentType = docType,
            issuingCountryCode = issuingCode,
            issuingCountryArabic = countryArabicName(issuingCode),
            nationalityCode = natCode,
            nationalityArabic = countryArabicName(natCode),
            surname = surname,
            givenNames = givenNames,
            fullLatinName = fullLatin,
            passportNumber = passportNumber,
            gender = genderArabic,
            birthDate = dobFormatted,
            age = age,
            expiryDate = expFormatted,
            isExpired = expired,
            personalNumber = personalNumber,
            issueDate = visualIssueInfo.first,
            placeOfIssue = visualIssueInfo.second,
            checkDigitsValid = pCheckOk && dobCheckOk && expCheckOk,
            passportCheckValid = pCheckOk,
            birthCheckValid = dobCheckOk,
            expiryCheckValid = expCheckOk,
            compositeCheckValid = compCheckOk
        )
    }

    private fun parseTd2(
        rawL1: String,
        rawL2: String,
        rawText: String,
        lines: List<String>
    ): PassportScanResult {
        val l1 = rawL1.padEnd(36, '<').take(36)
        val l2 = rawL2.padEnd(36, '<').take(36)
        val issuingCode = fixAlphaZone(l1.substring(2, 5)).replace("<", "")
        val nameParts = l1.substring(5, 36).split("<<", limit = 2)
        val surname = fixAlphaZone(nameParts.getOrElse(0) { "" }).replace("<", " ").trim()
        val givenNames = fixAlphaZone(nameParts.getOrElse(1) { "" }).replace("<", " ").trim()
        val passportField = l2.substring(0, 9)
        val pCheckOk = verifyCheckDigit(passportField, l2[9])
        val natCode = fixAlphaZone(l2.substring(10, 13)).replace("<", "")
        val dobField = fixNumericZone(l2.substring(13, 19))
        val dobCheckOk = verifyCheckDigit(dobField, l2[19])
        val sexChar = l2[20]
        val expField = fixNumericZone(l2.substring(21, 27))
        val expCheckOk = verifyCheckDigit(expField, l2[27])
        val dobFormatted = ArabicTextNormalizer.parseMrzDate(dobField, true)
        val expFormatted = ArabicTextNormalizer.parseMrzDate(expField, false)
        val visualIssueInfo = extractIssueFieldsFromVisualZone(lines)

        return PassportScanResult(
            rawText = rawText,
            mrzDetected = true,
            mrzLine1 = l1,
            mrzLine2 = l2,
            documentType = l1.substring(0, 2).replace("<", ""),
            issuingCountryCode = issuingCode,
            issuingCountryArabic = countryArabicName(issuingCode),
            nationalityCode = natCode,
            nationalityArabic = countryArabicName(natCode),
            surname = surname,
            givenNames = givenNames,
            fullLatinName = listOf(givenNames, surname).filter { it.isNotBlank() }.joinToString(" "),
            passportNumber = passportField.replace("<", ""),
            gender = if (sexChar == 'M') "ذكر" else if (sexChar == 'F') "أنثى" else "",
            birthDate = dobFormatted,
            age = ArabicTextNormalizer.calculateAge(dobFormatted),
            expiryDate = expFormatted,
            isExpired = ArabicTextNormalizer.isDateExpired(expFormatted),
            issueDate = visualIssueInfo.first,
            placeOfIssue = visualIssueInfo.second,
            checkDigitsValid = pCheckOk && dobCheckOk && expCheckOk,
            passportCheckValid = pCheckOk,
            birthCheckValid = dobCheckOk,
            expiryCheckValid = expCheckOk
        )
    }

    private fun parseVisualZoneFallback(rawText: String, lines: List<String>): PassportScanResult {
        val passportMatch = Regex("\\b([A-Z]{1,2}\\d{6,8}|\\d{8,10})\\b").find(rawText.uppercase(Locale.ROOT))
        val dateMatches = Regex("\\b(\\d{2}[/-]\\d{2}[/-]\\d{4}|\\d{4}[/-]\\d{2}[/-]\\d{2})\\b")
            .findAll(rawText).map { ArabicTextNormalizer.normalizeDateString(it.value) }.toList()
        val dob = dateMatches.firstOrNull().orEmpty()
        val exp = if (dateMatches.size >= 2) dateMatches.last() else ""
        val issueInfo = extractIssueFieldsFromVisualZone(lines)

        val likelyName = lines.firstOrNull { line ->
            val upper = line.uppercase(Locale.ROOT)
            upper.matches(Regex("^[A-Z\\s]{6,45}$")) &&
                !upper.contains("PASSPORT") &&
                !upper.contains("REPUBLIC") &&
                !upper.contains("DATE") &&
                !upper.contains("NATIONALITY")
        }.orEmpty()

        return PassportScanResult(
            rawText = rawText,
            mrzDetected = false,
            fullLatinName = likelyName,
            passportNumber = passportMatch?.value.orEmpty(),
            birthDate = dob,
            age = ArabicTextNormalizer.calculateAge(dob),
            expiryDate = exp,
            isExpired = ArabicTextNormalizer.isDateExpired(exp),
            issueDate = issueInfo.first,
            placeOfIssue = issueInfo.second
        )
    }

    private fun extractIssueFieldsFromVisualZone(lines: List<String>): Pair<String, String> {
        var issueDate = ""
        var placeOfIssue = ""
        for ((idx, line) in lines.withIndex()) {
            val lower = line.lowercase(Locale.ROOT)
            if ((lower.contains("issue") && lower.contains("date")) || lower.contains("تاريخ الاصدار")) {
                val next = lines.getOrNull(idx + 1).orEmpty()
                val found = Regex("\\d{2}[/-]\\d{2}[/-]\\d{4}").find(line) ?: Regex("\\d{2}[/-]\\d{2}[/-]\\d{4}").find(next)
                if (found != null) issueDate = ArabicTextNormalizer.normalizeDateString(found.value)
            }
            if (lower.contains("authority") || lower.contains("place of issue") || lower.contains("مكان الاصدار")) {
                val next = lines.getOrNull(idx + 1).orEmpty()
                if (next.length in 3..30) placeOfIssue = next
            }
        }
        return issueDate to placeOfIssue
    }

    fun verifyCheckDigit(data: String, checkChar: Char): Boolean {
        val expectedDigit = computeCheckDigit(data)
        val actual = if (checkChar == '<') 0 else checkChar.digitToIntOrNull() ?: return false
        return expectedDigit == actual
    }

    fun computeCheckDigit(data: String): Int {
        val weights = intArrayOf(7, 3, 1)
        var sum = 0
        for (i in data.indices) {
            val v = mrzCharValue(data[i])
            sum += v * weights[i % 3]
        }
        return sum % 10
    }

    private fun mrzCharValue(c: Char): Int = when (c) {
        '<' -> 0
        in '0'..'9' -> c - '0'
        in 'A'..'Z' -> c - 'A' + 10
        else -> 0
    }

    private fun fixNumericZone(s: String): String = buildString {
        for (c in s) {
            append(
                when (c) {
                    'O', 'Q', 'D' -> '0'
                    'I', 'L' -> '1'
                    'Z' -> '2'
                    'S' -> '5'
                    'G' -> '6'
                    'B' -> '8'
                    else -> c
                }
            )
        }
    }

    private fun fixAlphaZone(s: String): String = buildString {
        for (c in s) {
            append(
                when (c) {
                    '0' -> 'O'
                    '1' -> 'I'
                    '2' -> 'Z'
                    '5' -> 'S'
                    '6' -> 'G'
                    '8' -> 'B'
                    else -> c
                }
            )
        }
    }

    fun countryArabicName(iso3: String): String = COUNTRY_MAP[iso3.uppercase(Locale.ROOT)] ?: iso3

    private val COUNTRY_MAP = mapOf(
        "YEM" to "اليمن",
        "SAU" to "السعودية",
        "ARE" to "الإمارات",
        "EGY" to "مصر",
        "JOR" to "الأردن",
        "OMN" to "عُمان",
        "KWT" to "الكويت",
        "QAT" to "قطر",
        "BHR" to "البحرين",
        "IRQ" to "العراق",
        "SYR" to "سوريا",
        "LBN" to "لبنان",
        "PSE" to "فلسطين",
        "SDN" to "السودان",
        "SOM" to "الصومال",
        "DJI" to "جيبوتي",
        "ETH" to "إثيوبيا",
        "ERI" to "إريتريا",
        "MAR" to "المغرب",
        "TUN" to "تونس",
        "DZA" to "الجزائر",
        "LBY" to "ليبيا",
        "TUR" to "تركيا",
        "IND" to "الهند",
        "PAK" to "باكستان",
        "BGD" to "بنغلاديش",
        "PHL" to "الفلبين",
        "IDN" to "إندونيسيا",
        "MYS" to "ماليزيا",
        "GBR" to "المملكة المتحدة",
        "USA" to "الولايات المتحدة",
        "FRA" to "فرنسا",
        "DEU" to "ألمانيا",
        "ITA" to "إيطاليا",
        "ESP" to "إسبانيا",
        "CAN" to "كندا",
        "AUS" to "أستراليا",
        "CHN" to "الصين",
        "RUS" to "روسيا",
        "UNO" to "الأمم المتحدة"
    )
}
