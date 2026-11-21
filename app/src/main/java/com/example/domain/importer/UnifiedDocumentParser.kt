package com.example.domain.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.entities.ForeignerResidentEntity
import com.example.data.local.entities.MofaVisaRequestEntity
import com.example.data.local.entities.RefugeeRecordEntity
import com.example.data.local.entities.SecurityReportEntity
import com.example.data.local.entities.TravelAgencyEntity
import com.example.data.local.entities.TravelerEntity
import com.example.domain.model.DocumentCategory
import com.example.domain.model.SecurityReportType
import com.example.domain.model.SecuritySeverity
import com.example.domain.ocr.ArabicTextNormalizer
import com.example.domain.ocr.PassportOcrAnalyzer
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.DataFormatter
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.Locale
import java.util.UUID

data class StagingImportRow(
    val tempId: String = UUID.randomUUID().toString(),
    val fullName: String = "",
    val passportName: String = "",
    val documentNumber: String = "",
    val nationality: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val issueDate: String = "",
    val expiryDate: String = "",
    val agencyOrOrg: String = "",
    val portOrCenter: String = "",
    val destination: String = "",
    val flightOrMemo: String = "",
    val travelAllowed: String = "نعم",
    val notes: String = "",
    val watchlistWarning: String? = null
)

data class ParsedImportBatch(
    val fileName: String,
    val detectedCategory: DocumentCategory,
    val extractedMetadata: String = "",
    val rows: List<StagingImportRow> = emptyList(),
    val errorMessage: String? = null
)

class UnifiedDocumentParser(private val context: Context) {

    suspend fun parseFiles(
        uris: List<Uri>,
        pdfRotationDegrees: Int = 0
    ): List<ParsedImportBatch> = withContext(Dispatchers.IO) {
        uris.map { uri -> parseSingleFile(uri, pdfRotationDegrees) }
    }

    private suspend fun parseSingleFile(uri: Uri, pdfRotationDegrees: Int): ParsedImportBatch {
        val fileName = resolveFileName(uri)
        val mime = context.contentResolver.getType(uri)?.lowercase(Locale.ROOT).orEmpty()
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)

        return try {
            when {
                ext == "xlsx" || mime.contains("spreadsheetml") -> parseXlsx(uri, fileName)
                ext == "xls" || mime.contains("ms-excel") -> parseXls(uri, fileName)
                ext == "csv" || ext == "tsv" || mime.contains("csv") || mime.contains("comma-separated") ->
                    parseCsv(uri, fileName)
                ext == "pdf" || mime.contains("pdf") -> parsePdf(uri, fileName, pdfRotationDegrees)
                mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp") ->
                    parseImage(uri, fileName)
                else -> parseCsv(uri, fileName)
            }
        } catch (e: Exception) {
            ParsedImportBatch(
                fileName = fileName,
                detectedCategory = DocumentCategory.TRAVELERS,
                errorMessage = e.localizedMessage ?: "تعذر تحليل الملف"
            )
        }
    }

    private fun parseXlsx(uri: Uri, fileName: String): ParsedImportBatch {
        val table = mutableListOf<List<String>>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ReadableWorkbook(stream).use { wb ->
                val firstSheet = wb.firstSheet
                firstSheet.openStream().use { rows ->
                    rows.forEach { row ->
                        val cells = (0 until row.cellCount).map { idx ->
                            val raw = row.getCellText(idx).orEmpty()
                            ArabicTextNormalizer.cleanDocumentText(raw)
                        }
                        if (cells.any { it.isNotBlank() }) {
                            table.add(cells)
                        }
                    }
                }
            }
        } ?: error("تعذر فتح ملف Excel")
        return buildBatchFromTable(fileName, table)
    }

    private fun parseXls(uri: Uri, fileName: String): ParsedImportBatch {
        val table = mutableListOf<List<String>>()
        val formatter = DataFormatter(Locale.US)
        context.contentResolver.openInputStream(uri)?.use { stream ->
            HSSFWorkbook(stream).use { wb ->
                val sheet = wb.getSheetAt(0)
                for (row in sheet) {
                    val lastCell = row.lastCellNum.toInt().coerceAtLeast(0)
                    val cells = (0 until lastCell).map { idx ->
                        val cell = row.getCell(idx)
                        val raw = if (cell != null) formatter.formatCellValue(cell) else ""
                        ArabicTextNormalizer.cleanDocumentText(raw)
                    }
                    if (cells.any { it.isNotBlank() }) {
                        table.add(cells)
                    }
                }
            }
        } ?: error("تعذر فتح ملف XLS")
        return buildBatchFromTable(fileName, table)
    }

    private fun parseCsv(uri: Uri, fileName: String): ParsedImportBatch {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("تعذر قراءة ملف CSV")

        val text = decodeBestArabicCharset(bytes)
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ParsedImportBatch(fileName, DocumentCategory.TRAVELERS, errorMessage = "الملف فارغ")
        }
        val delimiter = when {
            lines.first().count { it == '\t' } >= 2 -> '\t'
            lines.first().count { it == ';' } >= 2 -> ';'
            else -> ','
        }
        val table = lines.map { line ->
            parseDelimitedLine(line, delimiter).map { ArabicTextNormalizer.cleanDocumentText(it) }
        }.filter { row -> row.any { it.isNotBlank() } }

        return buildBatchFromTable(fileName, table)
    }

    private suspend fun parsePdf(
        uri: Uri,
        fileName: String,
        rotationDegrees: Int
    ): ParsedImportBatch {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("تعذر فتح ملف PDF")

        // 1. Try PDFBox text extraction first
        val extractedText = runCatching {
            PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
                PDFTextStripper().apply { sortByPosition = true }.getText(doc)
            }
        }.getOrDefault("")

        val cleanedText = ArabicTextNormalizer.cleanDocumentText(extractedText)
        if (cleanedText.length >= 60) {
            return buildBatchFromFreeText(fileName, extractedText)
        }

        // 2. Scanned PDF fallback: render pages via PdfRenderer (with rotation support) + ML Kit OCR
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("تعذر قراءة PDF للعرض الضوئي")

        val ocrPagesText = StringBuilder()
        pfd.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                val maxPages = minOf(renderer.pageCount, 12)
                for (i in 0 until maxPages) {
                    renderer.openPage(i).use { page ->
                        val scale = 2
                        val bmp = Bitmap.createBitmap(
                            page.width * scale,
                            page.height * scale,
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val rotated = if (rotationDegrees % 360 != 0) {
                            rotateBitmap(bmp, rotationDegrees.toFloat()).also { bmp.recycle() }
                        } else bmp
                        val pageText = runCatching { PassportOcrAnalyzer.recognizeTextFromBitmap(rotated) }
                            .getOrDefault("")
                        rotated.recycle()
                        ocrPagesText.appendLine(pageText)
                    }
                }
            }
        }
        return buildBatchFromFreeText(fileName, ocrPagesText.toString())
    }

    private suspend fun parseImage(uri: Uri, fileName: String): ParsedImportBatch {
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: error("تعذر قراءة الصورة")
        val rawText = PassportOcrAnalyzer.recognizeTextFromBitmap(bmp)
        bmp.recycle()
        val scan = PassportOcrAnalyzer.analyzeOcrText(rawText)

        return if (scan.mrzDetected || scan.passportNumber.isNotBlank()) {
            ParsedImportBatch(
                fileName = fileName,
                detectedCategory = DocumentCategory.TRAVELERS,
                extractedMetadata = if (scan.mrzDetected) "تم قراءة سطر MRZ بنجاح" else "استخراج ضوئي OCR",
                rows = listOf(
                    StagingImportRow(
                        fullName = scan.fullLatinName,
                        passportName = scan.fullLatinName,
                        documentNumber = scan.passportNumber,
                        nationality = scan.nationalityArabic.ifBlank { scan.issuingCountryArabic },
                        gender = scan.gender,
                        birthDate = scan.birthDate,
                        issueDate = scan.issueDate,
                        expiryDate = scan.expiryDate,
                        notes = if (scan.isExpired) "تنبيه: الجواز منتهي الصلاحية" else ""
                    )
                )
            )
        } else {
            buildBatchFromFreeText(fileName, rawText)
        }
    }

    private fun buildBatchFromTable(fileName: String, table: List<List<String>>): ParsedImportBatch {
        if (table.isEmpty()) {
            return ParsedImportBatch(fileName, DocumentCategory.TRAVELERS, errorMessage = "الجدول فارغ")
        }

        // Find header row in top 8 rows (handles title banners above table)
        val headerIdx = findLikelyHeaderRow(table)
        val headers = table[headerIdx]
        val dataRows = table.drop(headerIdx + 1)
            .filterNot { isSummaryOrTotalRow(it) }
            .filter { row -> row.any { it.length >= 2 } }

        val fullContextText = (table.take(headerIdx + 1).flatten() + fileName).joinToString(" ")
        val category = classifyDocument(fullContextText)

        // Two-pass column matcher
        val columnMap = matchColumnsTwoPass(headers, dataRows)

        val flightMeta = extractFlightAndRouteMetadata(table.take(headerIdx + 1).flatten().joinToString(" "))

        val parsedRows = dataRows.mapNotNull { row ->
            fun cell(role: ColRole): String {
                val idx = columnMap[role] ?: return ""
                return row.getOrElse(idx) { "" }.trim()
            }

            val first = cell(ColRole.FIRST_NAME)
            val second = cell(ColRole.SECOND_NAME)
            val third = cell(ColRole.THIRD_NAME)
            val fourth = cell(ColRole.FOURTH_NAME)
            val combinedName = cell(ColRole.FULL_NAME).ifBlank {
                listOf(first, second, third, fourth).filter { it.isNotBlank() }.joinToString(" ")
            }
            val passportName = cell(ColRole.PASSPORT_NAME).ifBlank { combinedName }
            val docNum = ArabicTextNormalizer.normalizeDigits(cell(ColRole.PASSPORT_NO))
            val nationality = cell(ColRole.NATIONALITY)
            val gender = normalizeGender(cell(ColRole.GENDER))
            val dob = ArabicTextNormalizer.normalizeDateString(cell(ColRole.BIRTH_DATE))
            val issueDate = ArabicTextNormalizer.normalizeDateString(cell(ColRole.ISSUE_DATE))
            val expiryDate = ArabicTextNormalizer.normalizeDateString(cell(ColRole.EXPIRY_DATE))
            val agency = cell(ColRole.AGENCY)
            val port = cell(ColRole.PORT)
            val dest = cell(ColRole.DESTINATION)
            val allowedRaw = cell(ColRole.ALLOWED)
            val allowed = if (allowedRaw.contains("لا") || allowedRaw.contains("منع") || allowedRaw.equals("no", true)) "لا" else "نعم"
            val notes = cell(ColRole.NOTES)

            if (combinedName.isBlank() && docNum.isBlank()) return@mapNotNull null

            StagingImportRow(
                fullName = combinedName.ifBlank { passportName },
                passportName = passportName,
                documentNumber = docNum,
                nationality = nationality,
                gender = gender,
                birthDate = dob,
                issueDate = issueDate,
                expiryDate = expiryDate,
                agencyOrOrg = agency.ifBlank { flightMeta.organization },
                portOrCenter = port,
                destination = dest.ifBlank { flightMeta.route },
                flightOrMemo = flightMeta.flightOrMemo,
                travelAllowed = allowed,
                notes = notes
            )
        }

        return ParsedImportBatch(
            fileName = fileName,
            detectedCategory = category,
            extractedMetadata = flightMeta.summary,
            rows = parsedRows
        )
    }

    private fun buildBatchFromFreeText(fileName: String, rawText: String): ParsedImportBatch {
        val category = classifyDocument("$fileName $rawText")
        val flightMeta = extractFlightAndRouteMetadata(rawText)
        val lines = rawText.lines()
            .map { ArabicTextNormalizer.cleanDocumentText(it) }
            .filter { it.length >= 6 }

        // Check if MRZ exists inside PDF text
        val mrzResult = PassportOcrAnalyzer.analyzeOcrText(rawText)
        val rows = mutableListOf<StagingImportRow>()
        if (mrzResult.mrzDetected) {
            rows.add(
                StagingImportRow(
                    fullName = mrzResult.fullLatinName,
                    passportName = mrzResult.fullLatinName,
                    documentNumber = mrzResult.passportNumber,
                    nationality = mrzResult.nationalityArabic.ifBlank { mrzResult.issuingCountryArabic },
                    gender = mrzResult.gender,
                    birthDate = mrzResult.birthDate,
                    issueDate = mrzResult.issueDate,
                    expiryDate = mrzResult.expiryDate,
                    flightOrMemo = flightMeta.flightOrMemo,
                    agencyOrOrg = flightMeta.organization
                )
            )
        }

        // Extract passenger/memo lines containing passport-like tokens ([A-Z0-9]{6,12})
        val docRegex = Regex("\\b([A-Z]{0,2}\\d{6,10}|[A-Z]\\d{5,9})\\b")
        for (line in lines) {
            if (isSummaryOrTotalRow(listOf(line))) continue
            val docMatch = docRegex.find(line.uppercase(Locale.ROOT)) ?: continue
            val docNo = docMatch.value
            if (rows.any { it.documentNumber.equals(docNo, ignoreCase = true) }) continue

            val beforeDoc = line.substring(0, docMatch.range.first).trim()
                .replace(Regex("^\\d+[.)\\-\\s]+"), "")
                .trim()
            val afterDoc = line.substring(docMatch.range.last + 1).trim()
            val name = beforeDoc.ifBlank { afterDoc.take(40) }.trim()
            if (name.length < 3) continue

            val dobMatch = Regex("\\d{2}[/-]\\d{2}[/-]\\d{4}").find(line)
            rows.add(
                StagingImportRow(
                    fullName = name,
                    passportName = name,
                    documentNumber = docNo,
                    birthDate = dobMatch?.value?.let { ArabicTextNormalizer.normalizeDateString(it) }.orEmpty(),
                    agencyOrOrg = flightMeta.organization,
                    destination = flightMeta.route,
                    flightOrMemo = flightMeta.flightOrMemo,
                    notes = if (category == DocumentCategory.HUMANITARIAN_MANIFEST) "مستخرج من مانيفست رحلة إنسانية" else ""
                )
            )
        }

        return ParsedImportBatch(
            fileName = fileName,
            detectedCategory = category,
            extractedMetadata = flightMeta.summary,
            rows = rows,
            errorMessage = if (rows.isEmpty()) "لم يتم العثور على صفوف واضحة في الوثيقة — يمكنك الإدخال اليدوي" else null
        )
    }

    private fun classifyDocument(text: String): DocumentCategory {
        val norm = ArabicTextNormalizer.normalizeForMatching(text)
        return when {
            norm.contains("UNHAS") || norm.contains("MSF") || norm.contains("OSESGY") ||
                norm.contains("MANIFEST") || norm.contains("مانيفست") || norm.contains("طيران") ||
                norm.contains("اطباء بلا حدود") || norm.contains("رحله انسانيه") ->
                DocumentCategory.HUMANITARIAN_MANIFEST
            norm.contains("الخارجيه") || norm.contains("MOFA") || norm.contains("تاشيره") ||
                norm.contains("منظمات") || norm.contains("مذكره") ->
                DocumentCategory.MOFA_VISA
            norm.contains("تسهيل مرور") || norm.contains("نقل الجوازات") || norm.contains("ملخص الوكالات") ||
                norm.contains("سياره") ->
                DocumentCategory.TRANSIT_FACILITATION
            norm.contains("مطلوب") || norm.contains("تعميم") || norm.contains("ضبط") ||
                norm.contains("منع سفر") || norm.contains("بلاغ") ->
                DocumentCategory.SECURITY_REPORT
            norm.contains("اقامه") || norm.contains("وافد") || norm.contains("كفيل") ->
                DocumentCategory.FOREIGNER_RESIDENT
            norm.contains("لاجئ") || norm.contains("UNHCR") || norm.contains("المفوضيه") ->
                DocumentCategory.REFUGEE
            else -> DocumentCategory.TRAVELERS
        }
    }

    private data class FlightMeta(
        val organization: String = "",
        val flightOrMemo: String = "",
        val route: String = "",
        val summary: String = ""
    )

    private fun extractFlightAndRouteMetadata(text: String): FlightMeta {
        val upper = text.uppercase(Locale.ROOT)
        val org = when {
            upper.contains("UNHAS") -> "UNHAS"
            upper.contains("OSESGY") -> "OSESGY"
            upper.contains("MSF") || text.contains("أطباء بلا حدود") -> "MSF"
            upper.contains("UNHCR") -> "UNHCR"
            upper.contains("ICRC") -> "ICRC"
            upper.contains("WHO") -> "WHO"
            else -> ""
        }
        val flightMatch = Regex("\\b(?:UNHAS|UN|IY|MSF|FLIGHT)[-\\s]?\\d{2,4}\\b").find(upper)?.value.orEmpty()
        val routeMatch = Regex("\\b[A-Z]{3}\\s*[-/->]+\\s*[A-Z]{3}\\b").find(upper)?.value.orEmpty()
        val memoMatch = Regex("(?:مذكرة|رقم|MEMO|REF)[:\\s#-]*([A-Z0-9/-]{3,18})").find(upper)?.groupValues?.getOrNull(1).orEmpty()
        val ref = flightMatch.ifBlank { memoMatch }
        val summary = listOfNotNull(
            org.takeIf { it.isNotBlank() }?.let { "الجهة: $it" },
            ref.takeIf { it.isNotBlank() }?.let { "رقم الرحلة/المذكرة: $it" },
            routeMatch.takeIf { it.isNotBlank() }?.let { "الخط الجوي: $it" }
        ).joinToString(" | ")
        return FlightMeta(org, ref, routeMatch, summary)
    }

    private enum class ColRole {
        FULL_NAME, FIRST_NAME, SECOND_NAME, THIRD_NAME, FOURTH_NAME,
        PASSPORT_NAME, PASSPORT_NO, NATIONALITY, GENDER,
        BIRTH_DATE, ISSUE_DATE, EXPIRY_DATE, AGENCY, PORT, DESTINATION, ALLOWED, NOTES
    }

    private fun findLikelyHeaderRow(table: List<List<String>>): Int {
        val keywords = listOf("الاسم", "اسم", "جواز", "رقم", "جنسيه", "ميلاد", "name", "passport", "nationality")
        var bestIdx = 0
        var bestScore = -1
        for (i in 0 until minOf(table.size, 8)) {
            val rowText = ArabicTextNormalizer.normalizeForMatching(table[i].joinToString(" "))
            val score = keywords.count { rowText.contains(ArabicTextNormalizer.normalizeForMatching(it)) }
            if (score > bestScore) {
                bestScore = score
                bestIdx = i
            }
        }
        return bestIdx
    }

    /**
     * Pass 1: Header alias matching (Arabic + English).
     * Pass 2: Content-pattern inference on unmatched columns.
     */
    private fun matchColumnsTwoPass(
        headers: List<String>,
        sampleRows: List<List<String>>
    ): Map<ColRole, Int> {
        val map = mutableMapOf<ColRole, Int>()

        // Pass 1: exact & fuzzy header matching
        headers.forEachIndexed { idx, rawHeader ->
            val h = ArabicTextNormalizer.normalizeForMatching(rawHeader)
            when {
                h.contains("الاسم الاول") || h == "FIRST NAME" -> map.putIfAbsent(ColRole.FIRST_NAME, idx)
                h.contains("الاسم الثاني") || h.contains("اسم الاب") -> map.putIfAbsent(ColRole.SECOND_NAME, idx)
                h.contains("الاسم الثالث") || h.contains("اسم الجد") -> map.putIfAbsent(ColRole.THIRD_NAME, idx)
                h.contains("الاسم الرابع") || h.contains("اللقب") -> map.putIfAbsent(ColRole.FOURTH_NAME, idx)
                h.contains("بالجواز") || h.contains("PASSPORT NAME") || h.contains("LATIN") ->
                    map.putIfAbsent(ColRole.PASSPORT_NAME, idx)
                h.contains("الاسم") || h.contains("المسافر") || h.contains("FULL NAME") || h == "NAME" ->
                    map.putIfAbsent(ColRole.FULL_NAME, idx)
                h.contains("رقم الجواز") || h.contains("رقم الوثيقه") || h.contains("PASSPORT") || h.contains("DOC NO") ->
                    map.putIfAbsent(ColRole.PASSPORT_NO, idx)
                h.contains("الجنسيه") || h.contains("الدوله") || h.contains("NATIONALITY") || h.contains("COUNTRY") ->
                    map.putIfAbsent(ColRole.NATIONALITY, idx)
                h == "الجنس" || h == "النوع" || h.contains("GENDER") || h.contains("SEX") ->
                    map.putIfAbsent(ColRole.GENDER, idx)
                h.contains("الميلاد") || h.contains("BIRTH") || h == "DOB" ->
                    map.putIfAbsent(ColRole.BIRTH_DATE, idx)
                h.contains("الاصدار") || h.contains("ISSUE") ->
                    map.putIfAbsent(ColRole.ISSUE_DATE, idx)
                h.contains("الانتهاء") || h.contains("EXPIRY") || h.contains("EXP") ->
                    map.putIfAbsent(ColRole.EXPIRY_DATE, idx)
                h.contains("الوكاله") || h.contains("المنظمه") || h.contains("الجهه") || h.contains("AGENCY") || h.contains("ORG") ->
                    map.putIfAbsent(ColRole.AGENCY, idx)
                h.contains("المنفذ") || h.contains("المركز") || h.contains("PORT") ->
                    map.putIfAbsent(ColRole.PORT, idx)
                h.contains("الوجهه") || h.contains("DESTINATION") || h.contains("ROUTE") ->
                    map.putIfAbsent(ColRole.DESTINATION, idx)
                h.contains("مسموح") || h.contains("السفر") || h.contains("ALLOWED") ->
                    map.putIfAbsent(ColRole.ALLOWED, idx)
                h.contains("ملاحظات") || h.contains("السبب") || h.contains("NOTES") ->
                    map.putIfAbsent(ColRole.NOTES, idx)
            }
        }

        // Pass 2: Infer missing core roles from sample column values
        val samples = sampleRows.take(15)
        if (samples.isNotEmpty()) {
            val colCount = headers.size.coerceAtLeast(samples.maxOf { it.size })
            for (c in 0 until colCount) {
                if (c in map.values) continue
                val values = samples.mapNotNull { it.getOrNull(c)?.trim() }.filter { it.isNotBlank() }
                if (values.isEmpty()) continue

                if (ColRole.PASSPORT_NO !in map &&
                    values.count { it.matches(Regex("^[A-Z0-9]{6,12}$", RegexOption.IGNORE_CASE)) && it.any { ch -> ch.isDigit() } } >= values.size / 2 + 1
                ) {
                    map[ColRole.PASSPORT_NO] = c
                    continue
                }
                if (ColRole.GENDER !in map &&
                    values.all { it in listOf("ذكر", "أنثى", "انثى", "M", "F", "Male", "Female") }
                ) {
                    map[ColRole.GENDER] = c
                    continue
                }
                if (ColRole.BIRTH_DATE !in map &&
                    values.count { it.matches(Regex("^\\d{2,4}[/-]\\d{1,2}[/-]\\d{2,4}$")) } >= values.size / 2 + 1
                ) {
                    map[ColRole.BIRTH_DATE] = c
                    continue
                }
                if (ColRole.FULL_NAME !in map &&
                    values.count { it.count { ch -> ch == ' ' } >= 1 && it.none { ch -> ch.isDigit() } && it.length >= 6 } >= values.size / 2 + 1
                ) {
                    map[ColRole.FULL_NAME] = c
                }
            }
        }
        return map
    }

    private fun isSummaryOrTotalRow(row: List<String>): Boolean {
        val joined = ArabicTextNormalizer.normalizeForMatching(row.joinToString(" "))
        return joined.contains("الاجمالي") ||
            joined.contains("المجموع") ||
            joined.startsWith("TOTAL") ||
            joined.contains("SUBTOTAL") ||
            joined.contains("عدد السجلات")
    }

    private fun normalizeGender(raw: String): String = when (raw.trim().uppercase(Locale.ROOT)) {
        "M", "MALE", "ذكر" -> "ذكر"
        "F", "FEMALE", "أنثى", "انثى" -> "أنثى"
        else -> raw.trim()
    }

    private fun decodeBestArabicCharset(bytes: ByteArray): String {
        val utf8 = bytes.toString(Charsets.UTF_8)
        // If UTF-8 replacement char appears, fall back to Windows-1256 (common Arabic Excel CSV export)
        return if (utf8.contains('\uFFFD')) {
            BufferedReader(InputStreamReader(ByteArrayInputStream(bytes), Charset.forName("windows-1256"))).readText()
        } else {
            utf8.removePrefix("\uFEFF")
        }
    }

    private fun parseDelimitedLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    out.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        out.add(sb.toString())
        return out
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun resolveFileName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx) ?: "document"
            }
        }
        return uri.lastPathSegment ?: "document"
    }

    // Conversion helpers from committed StagingImportRow to Room Entities
    fun toTravelerEntity(row: StagingImportRow, defaultPort: String, officerName: String): TravelerEntity {
        val parts = row.fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return TravelerEntity(
            firstName = parts.getOrElse(0) { "" },
            secondName = parts.getOrElse(1) { "" },
            thirdName = parts.getOrElse(2) { "" },
            fourthName = parts.getOrElse(3) { "" },
            lastName = if (parts.size > 4) parts.drop(4).joinToString(" ") else "",
            passportName = row.passportName.ifBlank { row.fullName },
            idNumber = row.documentNumber,
            country = row.nationality,
            gender = row.gender,
            birthDate = row.birthDate,
            age = ArabicTextNormalizer.calculateAge(row.birthDate),
            issueDate = row.issueDate,
            expiryDate = row.expiryDate,
            passportStatus = if (ArabicTextNormalizer.isDateExpired(row.expiryDate)) "منتهي" else "صالح",
            agency = row.agencyOrOrg,
            center = row.portOrCenter.ifBlank { defaultPort },
            portName = row.portOrCenter.ifBlank { defaultPort },
            destination = row.destination,
            requestNumber = row.flightOrMemo,
            travelAllowed = row.travelAllowed,
            investigatingOfficer = officerName,
            additionalInfo = row.notes
        )
    }

    fun toMofaVisaRequest(row: StagingImportRow, defaultPort: String): MofaVisaRequestEntity =
        MofaVisaRequestEntity(
            memoNumber = row.flightOrMemo.ifBlank { "MEMO-${System.currentTimeMillis() % 10000}" },
            organizationName = row.agencyOrOrg.ifBlank { "وزارة الخارجية / منظمة دولية" },
            applicantName = row.fullName,
            passportNumber = row.documentNumber,
            nationality = row.nationality,
            airRoute = row.destination,
            entryPort = row.portOrCenter.ifBlank { defaultPort },
            notes = row.notes
        )

    fun toForeignerResident(row: StagingImportRow, defaultPort: String): ForeignerResidentEntity =
        ForeignerResidentEntity(
            fullName = row.fullName,
            passportNumber = row.documentNumber,
            nationality = row.nationality,
            issueDate = row.issueDate,
            expiryDate = row.expiryDate,
            portOfEntry = row.portOrCenter.ifBlank { defaultPort },
            notes = row.notes
        )

    fun toRefugeeRecord(row: StagingImportRow, defaultPort: String): RefugeeRecordEntity =
        RefugeeRecordEntity(
            unhcrNumber = row.documentNumber.ifBlank { "UNHCR-${System.currentTimeMillis() % 100000}" },
            fullName = row.fullName,
            nationality = row.nationality,
            gender = row.gender,
            birthDate = row.birthDate,
            arrivalPort = row.portOrCenter.ifBlank { defaultPort },
            vulnerabilityNotes = row.notes
        )

    fun toSecurityReport(row: StagingImportRow, actorName: String): SecurityReportEntity =
        SecurityReportEntity(
            circularNumber = row.flightOrMemo.ifBlank { "CIR-${System.currentTimeMillis() % 100000}" },
            subjectName = row.fullName,
            passportOrIdNumber = row.documentNumber,
            birthDate = row.birthDate,
            nationality = row.nationality,
            reportType = SecurityReportType.TRAVEL_BAN,
            severity = SecuritySeverity.HIGH,
            issuingAuthority = row.agencyOrOrg.ifBlank { "القيادة الأمنية" },
            reason = row.notes.ifBlank { "مدرج في كشف تعميم مستورد" },
            createdBy = actorName
        )

    fun toAgencySummary(row: StagingImportRow, defaultPort: String): TravelAgencyEntity =
        TravelAgencyEntity(
            agencyName = row.agencyOrOrg.ifBlank { row.fullName },
            licenseNumber = row.documentNumber.ifBlank { "LIC-${System.currentTimeMillis() % 10000}" },
            portName = row.portOrCenter.ifBlank { defaultPort },
            notes = row.notes
        )
}
