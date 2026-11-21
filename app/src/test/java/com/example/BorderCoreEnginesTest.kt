package com.example

import com.example.data.local.entities.PassportSeizureEntity
import com.example.data.local.entities.SecurityReportEntity
import com.example.data.local.entities.TravelerEntity
import com.example.domain.model.SecurityReportType
import com.example.domain.model.SecuritySeverity
import com.example.domain.model.SeizureStatus
import com.example.domain.model.TravelerSmartMerge
import com.example.domain.ocr.ArabicTextNormalizer
import com.example.domain.ocr.PassportOcrAnalyzer
import com.example.domain.security.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BorderCoreEnginesTest {

    @Test
    fun mrzTd3Parser_extractsFieldsAndValidatesCheckDigits() {
        // Construct a valid TD3 MRZ sample with real ICAO check digits
        val docField = "098765432"
        val docCheck = PassportOcrAnalyzer.computeCheckDigit(docField)
        val dobField = "920514"
        val dobCheck = PassportOcrAnalyzer.computeCheckDigit(dobField)
        val expField = "300820"
        val expCheck = PassportOcrAnalyzer.computeCheckDigit(expField)
        val personal = "<<<<<<<<<<<<<<"
        val personalCheck = PassportOcrAnalyzer.computeCheckDigit(personal)
        val line2NoComp = "${docField}${docCheck}YEM${dobField}${dobCheck}M${expField}${expCheck}${personal}${personalCheck}"
        val compositeInput = line2NoComp.substring(0, 10) + line2NoComp.substring(13, 20) + line2NoComp.substring(21, 43)
        val compCheck = PassportOcrAnalyzer.computeCheckDigit(compositeInput)

        val line1 = "P<YEMALMANSOOR<<AHMED<SALEH<<<<<<<<<<<<<<<<<"
        val line2 = "$line2NoComp$compCheck"

        val result = PassportOcrAnalyzer.analyzeOcrText("$line1\n$line2")
        assertTrue(result.mrzDetected)
        assertTrue(result.checkDigitsValid)
        assertTrue(result.compositeCheckValid)
        assertEquals("098765432", result.passportNumber)
        assertEquals("اليمن", result.nationalityArabic)
        assertEquals("ذكر", result.gender)
        assertEquals("14/05/1992", result.birthDate)
        assertEquals("20/08/2030", result.expiryDate)
        assertTrue(result.fullLatinName.contains("AHMED SALEH"))
    }

    @Test
    fun arabicNormalizer_normalizesDigitsAndNoiseAndMatchesFuzzyNames() {
        val digits = ArabicTextNormalizer.normalizeDigits("جواز رقم ١٢٣٤٥٦٧٨٩٠")
        assertEquals("جواز رقم 1234567890", digits)

        val cleaned = ArabicTextNormalizer.cleanDocumentText("أحمد في الجدول ١٢٣ في الكشف")
        assertEquals("أحمد 123", cleaned)

        val simArabic = ArabicTextNormalizer.similarityScore("عبد الله أحمد صالح", "عبدالله احمد صالح")
        assertTrue("Expected high similarity, got $simArabic", simArabic >= 0.85)

        val simLatin = ArabicTextNormalizer.similarityScore("MOHAMMED ALI", "MUHAMMAD ALY")
        assertTrue("Expected phonetic similarity, got $simLatin", simLatin >= 0.80)
    }

    @Test
    fun smartMerge_preservesSecurityBanPrecedenceAndLogsHistory() {
        val existing = TravelerEntity(
            id = 10,
            firstName = "سعيد",
            lastName = "الحداد",
            idNumber = "P1234567",
            travelAllowed = "لا",
            returnReason = "منع أمني"
        )
        val incoming = TravelerEntity(
            firstName = "سعيد",
            secondName = "محمد",
            lastName = "الحداد",
            idNumber = "P1234567",
            country = "اليمن",
            travelAllowed = "نعم"
        )

        val (merged, changes) = TravelerSmartMerge.merge(
            existing = existing,
            incoming = incoming,
            source = "استيراد كشف",
            officerName = "نقيب/ خالد",
            allowUnblockOverride = false
        )

        assertEquals("لا", merged.travelAllowed)
        assertEquals("محمد", merged.secondName)
        assertEquals("اليمن", merged.country)
        assertTrue(changes.isNotEmpty())
        assertEquals(1, merged.mergeHistory.size)
    }

    @Test
    fun passwordHasher_verifiesSaltedPbkdf2Hashes() {
        val salt = PasswordHasher.generateSaltBase64()
        val hash = PasswordHasher.hashPassword("Sovereign#2026", salt)
        assertTrue(PasswordHasher.verifyPassword("Sovereign#2026", salt, hash))
        assertFalse(PasswordHasher.verifyPassword("WrongPass", salt, hash))
    }

    @Test
    fun watchlistMatching_detectsExactDocumentAndForgedSeizure() {
        val reports = listOf(
            SecurityReportEntity(
                id = 1,
                circularNumber = "CIR-99",
                subjectName = "طلال علي ناجي",
                passportOrIdNumber = "A8877665",
                reportType = SecurityReportType.WANTED,
                severity = SecuritySeverity.CRITICAL,
                reason = "مطلوب للعدالة"
            )
        )
        val seizures = listOf(
            PassportSeizureEntity(
                id = 2,
                seizureNumber = "SZ-55",
                holderName = "Unknown",
                passportNumber = "A8877665",
                seizureReason = "تزوير",
                status = SeizureStatus.FORGED
            )
        )
        val docMatch = ArabicTextNormalizer.normalizeDocumentId("a-8877665") ==
            ArabicTextNormalizer.normalizeDocumentId(reports.first().passportOrIdNumber)
        assertTrue(docMatch)
        assertEquals(SeizureStatus.FORGED, seizures.first().status)
    }
}
