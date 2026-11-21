package com.example.domain.model

import kotlinx.serialization.Serializable

enum class UserRole(val arabicLabel: String, val tierLabel: String) {
    DIRECTOR_GENERAL("مدير عام", "القيادة والمشرفون"),
    ADMIN("آدمن النظام", "القيادة والمشرفون"),
    DEPARTMENT_DIRECTOR("مدير إدارة", "القيادة والمشرفون"),
    SECTION_HEAD("رئيس قسم", "القيادة والمشرفون"),
    LEAD_INVESTIGATOR("محقق أول", "ضباط المنافذ والتحقيق"),
    PASSPORT_OFFICER("ضابط جوازات", "ضباط المنافذ والتحقيق"),
    PORT_OFFICER("ضابط منفذ", "ضباط المنافذ والتحقيق"),
    DATA_ENTRY("موظف إدخال", "موظفو الإدخال");

    val isLeadership: Boolean
        get() = this == DIRECTOR_GENERAL || this == ADMIN || this == DEPARTMENT_DIRECTOR || this == SECTION_HEAD

    val canApproveTravelBan: Boolean
        get() = isLeadership

    val canManageUsers: Boolean
        get() = isLeadership

    val canViewAuditLogs: Boolean
        get() = isLeadership

    val canScanAndInvestigate: Boolean
        get() = isLeadership || this == LEAD_INVESTIGATOR || this == PASSPORT_OFFICER || this == PORT_OFFICER

    val canDeleteRecords: Boolean
        get() = isLeadership
}

enum class SeizureStatus(val arabicLabel: String) {
    NEW("جديد"),
    INSPECTION("قيد الفحص"),
    AUTHENTIC("سليم"),
    FORGED("مزور"),
    REFERRED("محال للتحقيق"),
    DELIVERED("تم التسليم")
}

enum class SecurityReportType(val arabicLabel: String) {
    WANTED("مطلوب أمنياً"),
    TRAVEL_BAN("منع من السفر"),
    FORGED_DOC("وثيقة مزورة"),
    SURVEILLANCE("مراقبة وملاحظة"),
    ARREST_WARRANT("أمر ضبط وإحضار")
}

enum class SecuritySeverity(val arabicLabel: String) {
    CRITICAL("حرج للغاية"),
    HIGH("عالي الخطورة"),
    MEDIUM("متوسط")
}

enum class PermitStatus(val arabicLabel: String) {
    ACTIVE("ساري المفعول"),
    USED("تم العبور"),
    EXPIRED("منتهي الصلاحية"),
    REVOKED("ملغي")
}

enum class DocumentCategory(val arabicLabel: String, val description: String) {
    TRAVELERS("كشف مسافرين وتأشيرات", "سجلات العبور والقدوم والمغادرة"),
    MOFA_VISA("مذكرة خارجية ومنظمات دولية", "طلبات التأشيرات والمذكرات الرسمية"),
    HUMANITARIAN_MANIFEST("مانيفست طيران إنساني", "رحلات الأمم المتحدة والمنظمات الإنسانية (UN / MSF / OSESGY)"),
    TRANSIT_FACILITATION("تسهيل مرور وملخص وكالات", "خطابات سيارات نقل الجوازات وملخصات الوكالات"),
    SECURITY_REPORT("تعميم وبلاغ ضبط أمني", "قوائم المطلوبين وأوامر المنع والتعميمات"),
    FOREIGNER_RESIDENT("كشف وافدين وإقامات", "سجلات الإقامة للوافدين الأجانب"),
    REFUGEE("سجل شؤون لاجئين", "قوائم المفوضية السامية واللاجئين")
}

enum class ThemePreference(val arabicLabel: String) {
    SYSTEM("تلقائي حسب النظام"),
    DARK("الوضع الميداني الداكن"),
    LIGHT("الوضع النهاري عالي التباين")
}

@Serializable
data class MergeChangeRecord(
    val timestamp: Long,
    val source: String,
    val officerName: String,
    val changedFields: List<String>
)

@Serializable
data class SignaturePoint(
    val x: Float,
    val y: Float,
    val isNewStroke: Boolean = false
)
