package com.example.domain.importer

import android.content.Context
import android.net.Uri
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.TravelerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExcelExportEngine(private val context: Context) {

    suspend fun exportTravelersXlsx(uri: Uri, travelers: List<TravelerEntity>): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    val wb = Workbook(out, "SmartBorderPorts", "1.0")
                    val ws = wb.newWorksheet("المسافرين")
                    val headers = listOf(
                        "الرقم", "الاسم الكامل", "الاسم بالجواز", "رقم الوثيقة", "نوع الوثيقة",
                        "حالة الجواز", "الجنس", "تاريخ الميلاد", "العمر", "الدولة",
                        "المنفذ", "الوكالة", "الوجهة", "تاريخ الانتهاء", "مسموح بالسفر", "سبب الإرجاع", "ضابط التحقيق"
                    )
                    headers.forEachIndexed { col, title -> ws.value(0, col, title) }
                    travelers.forEachIndexed { idx, t ->
                        val r = idx + 1
                        ws.value(r, 0, t.id.toString())
                        ws.value(r, 1, t.fullArabicName)
                        ws.value(r, 2, t.passportName)
                        ws.value(r, 3, t.idNumber)
                        ws.value(r, 4, t.idType)
                        ws.value(r, 5, t.passportStatus)
                        ws.value(r, 6, t.gender)
                        ws.value(r, 7, t.birthDate)
                        ws.value(r, 8, t.age?.toString().orEmpty())
                        ws.value(r, 9, t.country)
                        ws.value(r, 10, t.portName.ifBlank { t.center })
                        ws.value(r, 11, t.agency)
                        ws.value(r, 12, t.destination)
                        ws.value(r, 13, t.expiryDate)
                        ws.value(r, 14, t.travelAllowed)
                        ws.value(r, 15, t.returnReason)
                        ws.value(r, 16, t.investigatingOfficer)
                    }
                    wb.finish()
                } ?: error("تعذر إنشاء ملف التصدير")
                travelers.size
            }
        }

    suspend fun exportAuditLogsXlsx(uri: Uri, logs: List<AuditLogEntity>): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    val wb = Workbook(out, "SmartBorderPorts", "1.0")
                    val ws = wb.newWorksheet("سجل_التدقيق")
                    val headers = listOf(
                        "المعرف", "الوقت", "العملية", "الكيان", "الملخص", "التفاصيل",
                        "المستخدم", "الدور", "المنفذ", "الأهمية", "بصمة التحقق"
                    )
                    headers.forEachIndexed { col, title -> ws.value(0, col, title) }
                    logs.forEachIndexed { idx, l ->
                        val r = idx + 1
                        ws.value(r, 0, l.id.toString())
                        ws.value(r, 1, fmt.format(Date(l.timestamp)))
                        ws.value(r, 2, l.actionType)
                        ws.value(r, 3, l.entityType)
                        ws.value(r, 4, l.summary)
                        ws.value(r, 5, l.details)
                        ws.value(r, 6, l.username)
                        ws.value(r, 7, l.userRole)
                        ws.value(r, 8, l.portName)
                        ws.value(r, 9, l.severity)
                        ws.value(r, 10, l.integrityHash)
                    }
                    wb.finish()
                } ?: error("تعذر إنشاء ملف التصدير")
                logs.size
            }
        }
}
