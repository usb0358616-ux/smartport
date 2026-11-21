package com.example.domain.ocr

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ArabicTextNormalizer {

    private val EASTERN_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    private val DIACRITICS_REGEX = Regex("[\\u064B-\\u065F\\u0670\\u0640]")
    private val BIDI_MARKS_REGEX = Regex("[\\u200E\\u200F\\u202A-\\u202E\\uFEFF]")
    private val NOISE_PHRASES_REGEX = Regex(
        "(?:في\\s+الجدول|في\\s+الكشف|حسب\\s+الكشف|المذكور\\s+اعلاه|المذكور\\s+أعلاه|" +
            "صفحة\\s*\\d+\\s*(?:من\\s*\\d+)?|ملاحظات\\s*:|الرقم\\s+التسلسلي)",
        RegexOption.IGNORE_CASE
    )
    private val MULTI_SPACE_REGEX = Regex("\\s+")

    /**
     * Converts Eastern Arabic (٠-٩) and Persian (۰-۹) digits to Western ASCII (0-9).
     */
    fun normalizeDigits(input: String): String {
        if (input.isEmpty()) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idxEastern = EASTERN_DIGITS.indexOf(ch)
            if (idxEastern >= 0) {
                sb.append(('0'.code + idxEastern).toChar())
                continue
            }
            val idxPersian = PERSIAN_DIGITS.indexOf(ch)
            if (idxPersian >= 0) {
                sb.append(('0'.code + idxPersian).toChar())
                continue
            }
            sb.append(ch)
        }
        return sb.toString()
    }

    /**
     * Cleans extracted PDF/Excel/OCR text from noise boilerplate, bidi control chars, and extra spaces.
     */
    fun cleanDocumentText(input: String): String {
        val noBidi = input.replace(BIDI_MARKS_REGEX, "")
        val normalizedDigits = normalizeDigits(noBidi)
        val noNoise = normalizedDigits.replace(NOISE_PHRASES_REGEX, " ")
        return noNoise.replace(MULTI_SPACE_REGEX, " ").trim()
    }

    /**
     * Normalizes Arabic & Latin names for fuzzy/phonetic watchlist matching.
     */
    fun normalizeForMatching(input: String): String {
        var s = normalizeDigits(input)
            .replace(BIDI_MARKS_REGEX, "")
            .replace(DIACRITICS_REGEX, "")
            .trim()
            .uppercase(Locale.ROOT)
        s = s.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ى', 'ي')
            .replace('ئ', 'ي')
            .replace('ؤ', 'و')
            .replace('ة', 'ه')
            .replace("عبد ال", "عبدال")
            .replace("ابو ", "ابو")
            .replace(Regex("[^A-Z0-9\\u0621-\\u064A\\s]"), " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
        return s
    }

    fun normalizeDocumentId(input: String): String {
        return normalizeDigits(input)
            .uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9]"), "")
    }

    /**
     * Parses a 6-digit MRZ date (YYMMDD) into dd/MM/yyyy.
     * For birth dates, dates in the future belong to 19xx. For expiry dates, assume 20xx unless > currentYear + 30.
     */
    fun parseMrzDate(yymmdd: String, isBirthDate: Boolean): String {
        val clean = normalizeDigits(yymmdd).filter { it.isDigit() }
        if (clean.length != 6) return ""
        val yy = clean.substring(0, 2).toIntOrNull() ?: return ""
        val mm = clean.substring(2, 4).toIntOrNull() ?: return ""
        val dd = clean.substring(4, 6).toIntOrNull() ?: return ""
        if (mm !in 1..12 || dd !in 1..31) return ""

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentTwoDigit = currentYear % 100
        val century = if (isBirthDate) {
            if (yy > currentTwoDigit) 1900 else 2000
        } else {
            if (yy > currentTwoDigit + 35) 1900 else 2000
        }
        val yyyy = century + yy
        return "%02d/%02d/%04d".format(Locale.US, dd, mm, yyyy)
    }

    fun normalizeDateString(raw: String): String {
        val clean = normalizeDigits(raw).trim()
        if (clean.isEmpty()) return ""
        val formats = listOf(
            "dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd", "yyyy/MM/dd",
            "dd-MM-yyyy", "d-M-yyyy", "dd.MM.yyyy"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = sdf.parse(clean)
                if (parsed != null) {
                    val out = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                    return out.format(parsed)
                }
            } catch (_: Exception) {
            }
        }
        return clean
    }

    fun calculateAge(birthDateStr: String): Int? {
        val normalized = normalizeDateString(birthDateStr)
        if (normalized.isBlank()) return null
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
            val dob = sdf.parse(normalized) ?: return null
            val dobCal = Calendar.getInstance().apply { time = dob }
            val now = Calendar.getInstance()
            var age = now.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
            if (now.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            if (age in 0..130) age else null
        } catch (_: Exception) {
            null
        }
    }

    fun isDateExpired(expiryDateStr: String): Boolean {
        val normalized = normalizeDateString(expiryDateStr)
        if (normalized.isBlank()) return false
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
            val exp = sdf.parse(normalized) ?: return false
            val now = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            exp.before(now)
        } catch (_: Exception) {
            false
        }
    }

    fun similarityScore(a: String, b: String): Double {
        val normA = normalizeForMatching(a)
        val normB = normalizeForMatching(b)
        if (normA.isEmpty() || normB.isEmpty()) return 0.0
        if (normA == normB) return 1.0

        val dist = levenshtein(normA, normB)
        val maxLen = max(normA.length, normB.length)
        val charSim = 1.0 - (dist.toDouble() / maxLen.toDouble())

        val tokensA = normA.split(" ").filter { it.length > 1 }.toSet()
        val tokensB = normB.split(" ").filter { it.length > 1 }.toSet()
        val tokenSim = if (tokensA.isEmpty() || tokensB.isEmpty()) {
            0.0
        } else {
            val intersection = tokensA.intersect(tokensB).size.toDouble()
            val union = tokensA.union(tokensB).size.toDouble()
            intersection / union
        }

        // Also check Latin phonetic similarity if both are Latin
        val phoneticBoost = if (isMostlyLatin(normA) && isMostlyLatin(normB)) {
            val sA = soundexSentence(normA)
            val sB = soundexSentence(normB)
            if (sA == sB && sA.isNotEmpty()) 0.92 else 0.0
        } else 0.0

        return max(max(charSim, tokenSim), phoneticBoost)
    }

    private fun isMostlyLatin(s: String): Boolean =
        s.count { it in 'A'..'Z' } > s.length / 2

    private fun soundexSentence(s: String): String =
        s.split(" ").filter { it.length >= 2 }.joinToString("-") { soundexWord(it) }

    private fun soundexWord(word: String): String {
        val clean = word.filter { it in 'A'..'Z' }
        if (clean.isEmpty()) return ""
        val first = clean[0]
        fun code(c: Char): Char = when (c) {
            'B', 'F', 'P', 'V' -> '1'
            'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z' -> '2'
            'D', 'T' -> '3'
            'L' -> '4'
            'M', 'N' -> '5'
            'R' -> '6'
            else -> '0'
        }
        val sb = StringBuilder().append(first)
        var prev = code(first)
        for (i in 1 until clean.length) {
            val c = code(clean[i])
            if (c != '0' && c != prev) {
                sb.append(c)
                if (sb.length == 4) break
            }
            if (c != '0') prev = c
        }
        while (sb.length < 4) sb.append('0')
        return sb.toString()
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                dp[j] = if (a[i - 1] == b[j - 1]) {
                    prev
                } else {
                    1 + min(prev, min(dp[j], dp[j - 1]))
                }
                prev = temp
            }
        }
        return dp[b.length]
    }
}
