package com.example.data.local

import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.ForeignerResidentEntity
import com.example.data.local.entities.InternalMovementPermitEntity
import com.example.data.local.entities.MofaVisaRequestEntity
import com.example.data.local.entities.PassportSeizureEntity
import com.example.data.local.entities.PortOfficerEntity
import com.example.data.local.entities.RefugeeRecordEntity
import com.example.data.local.entities.SecurityReportEntity
import com.example.data.local.entities.TravelAgencyEntity
import com.example.data.local.entities.TravelerEntity
import com.example.data.local.entities.UndocumentedMigrantEntity
import com.example.data.local.entities.UserAccountEntity
import com.example.data.local.entities.WatchlistAlertEntity
import com.example.domain.model.TravelerSmartMerge
import com.example.domain.model.UserRole
import com.example.domain.ocr.ArabicTextNormalizer
import com.example.domain.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DashboardMetrics(
    val totalTravelers: Int = 0,
    val allowedTravelers: Int = 0,
    val blockedOrReturned: Int = 0,
    val activeSecurityReports: Int = 0,
    val totalSeizures: Int = 0
)

class BorderRepository(private val db: BorderDatabase) {

    private val auditMutex = Mutex()

    // Reactive Flows
    val travelers: Flow<List<TravelerEntity>> = db.travelerDao().observeAllTravelers()
    val seizures: Flow<List<PassportSeizureEntity>> = db.passportSeizureDao().observeAll()
    val mofaRequests: Flow<List<MofaVisaRequestEntity>> = db.mofaVisaRequestDao().observeAll()
    val foreignerResidents: Flow<List<ForeignerResidentEntity>> = db.foreignerResidentDao().observeAll()
    val refugeeRecords: Flow<List<RefugeeRecordEntity>> = db.refugeeRecordDao().observeAll()
    val undocumentedMigrants: Flow<List<UndocumentedMigrantEntity>> = db.undocumentedMigrantDao().observeAll()
    val movementPermits: Flow<List<InternalMovementPermitEntity>> = db.internalMovementPermitDao().observeAll()
    val securityReports: Flow<List<SecurityReportEntity>> = db.securityReportDao().observeAll()
    val travelAgencies: Flow<List<TravelAgencyEntity>> = db.travelAgencyDao().observeAll()
    val portOfficers: Flow<List<PortOfficerEntity>> = db.portOfficerDao().observeAll()
    val users: Flow<List<UserAccountEntity>> = db.userAccountDao().observeAllUsers()
    val userCount: Flow<Int> = db.userAccountDao().observeUserCount()
    val auditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().observeAllLogs()
    val allAlerts: Flow<List<WatchlistAlertEntity>> = db.watchlistAlertDao().observeAllAlerts()
    val activeAlerts: Flow<List<WatchlistAlertEntity>> = db.watchlistAlertDao().observeUnacknowledgedAlerts()

    val dashboardMetrics: Flow<DashboardMetrics> = combine(
        db.travelerDao().observeTotalCount(),
        db.travelerDao().observeAllowedCount(),
        db.travelerDao().observeBlockedOrReturnedCount(),
        db.securityReportDao().observeActiveCount(),
        db.passportSeizureDao().observeCount()
    ) { total, allowed, blocked, reports, seizures ->
        DashboardMetrics(
            totalTravelers = total,
            allowedTravelers = allowed,
            blockedOrReturned = blocked,
            activeSecurityReports = reports,
            totalSeizures = seizures
        )
    }

    // --- Authentication & RBAC ---

    suspend fun bootstrapFirstAdmin(
        username: String,
        fullName: String,
        badgeNumber: String,
        assignedPort: String,
        password: String
    ): Result<UserAccountEntity> = runCatching {
        require(db.userAccountDao().getUserCountOnce() == 0) { "تمت تهيئة النظام مسبقاً" }
        require(username.trim().length >= 3) { "اسم المستخدم يجب أن يكون 3 أحرف على الأقل" }
        require(password.length >= 6) { "كلمة المرور يجب أن تكون 6 خانات على الأقل" }

        val salt = PasswordHasher.generateSaltBase64()
        val hash = PasswordHasher.hashPassword(password, salt)
        val admin = UserAccountEntity(
            username = username.trim().lowercase(),
            fullName = fullName.trim(),
            badgeNumber = badgeNumber.trim(),
            role = UserRole.DIRECTOR_GENERAL,
            assignedPort = assignedPort.trim().ifBlank { "المركز الرئيسي" },
            passwordHash = hash,
            passwordSalt = salt,
            biometricEnabled = false,
            isActive = true,
            lastLoginAt = System.currentTimeMillis()
        )
        val id = db.userAccountDao().insert(admin)
        val saved = admin.copy(id = id)
        appendAuditLog(
            actionType = "SYSTEM_BOOTSTRAP",
            entityType = "UserAccount",
            entityId = id.toString(),
            summary = "تهيئة حساب القيادة الأول: ${saved.fullName}",
            details = "المنفذ: ${saved.assignedPort} | الرقم الوظيفي: ${saved.badgeNumber}",
            user = saved,
            severity = "CRITICAL"
        )
        saved
    }

    suspend fun authenticate(username: String, password: String): Result<UserAccountEntity> = runCatching {
        val cleanUser = username.trim().lowercase()
        val user = db.userAccountDao().findByUsername(cleanUser)
            ?: throw IllegalArgumentException("بيانات الدخول غير صحيحة")
        require(user.isActive) { "تم تعطيل هذا الحساب من قبل القيادة" }
        val ok = PasswordHasher.verifyPassword(password, user.passwordSalt, user.passwordHash)
        require(ok) { "بيانات الدخول غير صحيحة" }

        val updated = user.copy(lastLoginAt = System.currentTimeMillis())
        db.userAccountDao().update(updated)
        appendAuditLog(
            actionType = "LOGIN_SUCCESS",
            entityType = "UserAccount",
            entityId = updated.id.toString(),
            summary = "تسجيل دخول ضابط: ${updated.fullName}",
            details = "الدور: ${updated.role.arabicLabel}",
            user = updated
        )
        updated
    }

    suspend fun authenticateByBiometricUsername(username: String): Result<UserAccountEntity> = runCatching {
        val user = db.userAccountDao().findByUsername(username.trim().lowercase())
            ?: throw IllegalArgumentException("الحساب المرتبط بالبصمة غير موجود")
        require(user.isActive && user.biometricEnabled) { "الدخول بالبصمة غير مفعّل لهذا الحساب" }
        val updated = user.copy(lastLoginAt = System.currentTimeMillis())
        db.userAccountDao().update(updated)
        appendAuditLog(
            actionType = "BIOMETRIC_LOGIN",
            entityType = "UserAccount",
            entityId = updated.id.toString(),
            summary = "دخول بالبصمة الحيوية: ${updated.fullName}",
            details = "الدور: ${updated.role.arabicLabel}",
            user = updated
        )
        updated
    }

    suspend fun createUserAccount(
        actor: UserAccountEntity,
        username: String,
        fullName: String,
        badgeNumber: String,
        role: UserRole,
        assignedPort: String,
        password: String
    ): Result<UserAccountEntity> = runCatching {
        require(actor.role.canManageUsers) { "صلاحية غير كافية لإدارة المستخدمين" }
        require(username.trim().length >= 3) { "اسم المستخدم قصير جداً" }
        require(password.length >= 6) { "كلمة المرور يجب أن تكون 6 خانات على الأقل" }

        val salt = PasswordHasher.generateSaltBase64()
        val hash = PasswordHasher.hashPassword(password, salt)
        val newUser = UserAccountEntity(
            username = username.trim().lowercase(),
            fullName = fullName.trim(),
            badgeNumber = badgeNumber.trim(),
            role = role,
            assignedPort = assignedPort.trim().ifBlank { actor.assignedPort },
            passwordHash = hash,
            passwordSalt = salt
        )
        val id = db.userAccountDao().insert(newUser)
        val saved = newUser.copy(id = id)
        appendAuditLog(
            actionType = "USER_CREATED",
            entityType = "UserAccount",
            entityId = id.toString(),
            summary = "إنشاء حساب جديد: ${saved.fullName} (${saved.role.arabicLabel})",
            details = "بواسطة: ${actor.fullName} | المنفذ: ${saved.assignedPort}",
            user = actor,
            severity = "WARNING"
        )
        saved
    }

    suspend fun updateUserRoleOrStatus(
        actor: UserAccountEntity,
        target: UserAccountEntity,
        newRole: UserRole,
        isActive: Boolean
    ): Result<Unit> = runCatching {
        require(actor.role.canManageUsers) { "صلاحية غير كافية لتعديل الصلاحيات" }
        require(actor.id != target.id || isActive) { "لا يمكن تعطيل حسابك النشط حالياً" }
        val updated = target.copy(role = newRole, isActive = isActive)
        db.userAccountDao().update(updated)
        appendAuditLog(
            actionType = "ROLE_PERMISSION_CHANGED",
            entityType = "UserAccount",
            entityId = target.id.toString(),
            summary = "تحديث صلاحية/حالة ${target.fullName}",
            details = "الدور: ${target.role.arabicLabel} -> ${newRole.arabicLabel} | نشط: $isActive",
            user = actor,
            severity = "CRITICAL"
        )
    }

    suspend fun setBiometricEnabled(user: UserAccountEntity, enabled: Boolean): UserAccountEntity {
        val updated = user.copy(biometricEnabled = enabled)
        db.userAccountDao().update(updated)
        appendAuditLog(
            actionType = "BIOMETRIC_TOGGLED",
            entityType = "UserAccount",
            entityId = user.id.toString(),
            summary = if (enabled) "تفعيل البصمة الحيوية" else "إلغاء البصمة الحيوية",
            details = "المستخدم: ${user.username}",
            user = updated
        )
        return updated
    }

    // --- Travelers & Smart Merge ---

    suspend fun saveOrMergeTraveler(
        incoming: TravelerEntity,
        source: String,
        actor: UserAccountEntity
    ): Pair<Long, Boolean> {
        val normalizedId = ArabicTextNormalizer.normalizeDocumentId(incoming.idNumber)
        val existingByDoc = if (normalizedId.isNotBlank()) {
            db.travelerDao().findByNormalizedIdNumber(normalizedId)
        } else null

        val existing = existingByDoc ?: findTravelerByFuzzyNameAndDob(incoming)

        return if (existing != null) {
            val (merged, changes) = TravelerSmartMerge.merge(
                existing = existing,
                incoming = incoming,
                source = source,
                officerName = actor.fullName,
                allowUnblockOverride = actor.role.canApproveTravelBan
            )
            db.travelerDao().update(merged)
            appendAuditLog(
                actionType = "TRAVELER_SMART_MERGE",
                entityType = "Traveler",
                entityId = merged.id.toString(),
                summary = "دمج ذكي لسجل المسافر: ${merged.fullArabicName}",
                details = if (changes.isEmpty()) "لا توجد تغييرات جديدة" else changes.joinToString(" | "),
                user = actor
            )
            merged.id to true
        } else {
            val sanitized = if (!actor.role.canApproveTravelBan && incoming.travelAllowed == "لا") {
                // Non-leadership can flag returnReason / vettingResult, or if explicitly marked by scanner alert
                incoming
            } else incoming
            val newId = db.travelerDao().insert(sanitized)
            appendAuditLog(
                actionType = if (sanitized.travelAllowed == "لا") "TRAVEL_BAN_RECORDED" else "TRAVELER_CREATED",
                entityType = "Traveler",
                entityId = newId.toString(),
                summary = "تسجيل مسافر: ${sanitized.fullArabicName} (${sanitized.idNumber})",
                details = "المصدر: $source | مسموح بالسفر: ${sanitized.travelAllowed}",
                user = actor,
                severity = if (sanitized.travelAllowed == "لا") "WARNING" else "INFO"
            )
            newId to false
        }
    }

    private suspend fun findTravelerByFuzzyNameAndDob(incoming: TravelerEntity): TravelerEntity? {
        val incomingName = incoming.fullArabicName
        if (incomingName.isBlank()) return null
        val all = db.travelerDao().getAllTravelersOnce()
        val normDob = ArabicTextNormalizer.normalizeDateString(incoming.birthDate)
        return all.firstOrNull { candidate ->
            val sim = ArabicTextNormalizer.similarityScore(candidate.fullArabicName, incomingName)
            val dobMatches = normDob.isNotBlank() &&
                ArabicTextNormalizer.normalizeDateString(candidate.birthDate) == normDob
            sim >= 0.90 && (dobMatches || normDob.isBlank())
        }
    }

    suspend fun deleteTraveler(actor: UserAccountEntity, traveler: TravelerEntity): Result<Unit> = runCatching {
        require(actor.role.canDeleteRecords) { "الحذف مقتصر على القيادة والمشرفين" }
        db.travelerDao().delete(traveler)
        appendAuditLog(
            actionType = "TRAVELER_DELETED",
            entityType = "Traveler",
            entityId = traveler.id.toString(),
            summary = "حذف سجل مسافر: ${traveler.fullArabicName}",
            details = "رقم الوثيقة: ${traveler.idNumber}",
            user = actor,
            severity = "WARNING"
        )
    }

    // --- Specialized Modules CRUD ---

    suspend fun saveSeizure(seizure: PassportSeizureEntity, actor: UserAccountEntity): Long {
        val id = db.passportSeizureDao().insert(seizure.copy(updatedAt = System.currentTimeMillis()))
        appendAuditLog(
            actionType = "SEIZURE_SAVED",
            entityType = "PassportSeizure",
            entityId = id.toString(),
            summary = "محضر ضبط جواز (${seizure.status.arabicLabel}): ${seizure.holderName}",
            details = "رقم الجواز: ${seizure.passportNumber} | السبب: ${seizure.seizureReason}",
            user = actor,
            severity = "WARNING"
        )
        return id
    }

    suspend fun saveMofaRequest(item: MofaVisaRequestEntity, actor: UserAccountEntity): Long {
        val id = db.mofaVisaRequestDao().insert(item)
        appendAuditLog(
            actionType = "MOFA_VISA_SAVED",
            entityType = "MofaVisaRequest",
            entityId = id.toString(),
            summary = "مذكرة خارجية/منظمة: ${item.organizationName} - ${item.applicantName}",
            details = "رقم المذكرة: ${item.memoNumber} | الوثيقة: ${item.passportNumber}",
            user = actor
        )
        return id
    }

    suspend fun saveForeignerResident(item: ForeignerResidentEntity, actor: UserAccountEntity): Long {
        val id = db.foreignerResidentDao().insert(item)
        appendAuditLog(
            actionType = "RESIDENT_SAVED",
            entityType = "ForeignerResident",
            entityId = id.toString(),
            summary = "سجل مقيم أجنبي: ${item.fullName}",
            details = "الإقامة: ${item.residencyNumber} | الجنسية: ${item.nationality}",
            user = actor
        )
        return id
    }

    suspend fun saveRefugeeRecord(item: RefugeeRecordEntity, actor: UserAccountEntity): Long {
        val id = db.refugeeRecordDao().insert(item)
        appendAuditLog(
            actionType = "REFUGEE_SAVED",
            entityType = "RefugeeRecord",
            entityId = id.toString(),
            summary = "سجل لاجئ: ${item.fullName}",
            details = "رقم المفوضية: ${item.unhcrNumber}",
            user = actor
        )
        return id
    }

    suspend fun saveUndocumentedMigrant(item: UndocumentedMigrantEntity, actor: UserAccountEntity): Long {
        val id = db.undocumentedMigrantDao().insert(item)
        appendAuditLog(
            actionType = "MIGRANT_CASE_SAVED",
            entityType = "UndocumentedMigrant",
            entityId = id.toString(),
            summary = "ملف تسلل/هجرة غير نظامية: ${item.caseNumber}",
            details = "الاسم: ${item.fullName} | الموقع: ${item.apprehensionLocation}",
            user = actor
        )
        return id
    }

    suspend fun saveMovementPermit(item: InternalMovementPermitEntity, actor: UserAccountEntity): Long {
        val id = db.internalMovementPermitDao().insert(item)
        appendAuditLog(
            actionType = "PERMIT_ISSUED",
            entityType = "InternalMovementPermit",
            entityId = id.toString(),
            summary = "تصريح تنقل وفود: ${item.delegationName} (${item.permitCode})",
            details = "المسار: ${item.originGovernorate} -> ${item.destinationGovernorate}",
            user = actor
        )
        return id
    }

    suspend fun verifyMovementPermitCode(code: String, actor: UserAccountEntity): InternalMovementPermitEntity? {
        val clean = code.trim()
        val permit = db.internalMovementPermitDao().findByPermitCode(clean)
        if (permit != null) {
            appendAuditLog(
                actionType = "PERMIT_QR_VERIFIED",
                entityType = "InternalMovementPermit",
                entityId = permit.id.toString(),
                summary = "تحقق QR لتصريح: ${permit.permitCode} (${permit.status.arabicLabel})",
                details = "الوفد: ${permit.delegationName}",
                user = actor
            )
        }
        return permit
    }

    suspend fun saveSecurityReport(report: SecurityReportEntity, actor: UserAccountEntity): Result<Long> = runCatching {
        require(actor.role.canScanAndInvestigate) { "صلاحية غير كافية لإصدار بلاغات الضبط" }
        val id = db.securityReportDao().insert(report)
        appendAuditLog(
            actionType = "SECURITY_REPORT_ISSUED",
            entityType = "SecurityReport",
            entityId = id.toString(),
            summary = "بلاغ/تعميم أمني (${report.reportType.arabicLabel}): ${report.subjectName}",
            details = "رقم التعميم: ${report.circularNumber} | الوثيقة: ${report.passportOrIdNumber}",
            user = actor,
            severity = "CRITICAL"
        )
        id
    }

    suspend fun toggleSecurityReportActive(report: SecurityReportEntity, actor: UserAccountEntity): Result<Unit> = runCatching {
        require(actor.role.canApproveTravelBan) { "تعديل حالة التعميم مقتصر على القيادة والمشرفين" }
        val updated = report.copy(isActive = !report.isActive)
        db.securityReportDao().update(updated)
        appendAuditLog(
            actionType = "SECURITY_REPORT_STATUS",
            entityType = "SecurityReport",
            entityId = report.id.toString(),
            summary = "تغيير حالة تعميم ${report.circularNumber} إلى نشط=${updated.isActive}",
            details = report.subjectName,
            user = actor,
            severity = "WARNING"
        )
    }

    suspend fun saveTravelAgency(item: TravelAgencyEntity, actor: UserAccountEntity): Long {
        val id = db.travelAgencyDao().insert(item)
        appendAuditLog(
            actionType = "AGENCY_SAVED",
            entityType = "TravelAgency",
            entityId = id.toString(),
            summary = "تحديث دليل الوكالات: ${item.agencyName}",
            details = "ترخيص: ${item.licenseNumber}",
            user = actor
        )
        return id
    }

    suspend fun savePortOfficer(item: PortOfficerEntity, actor: UserAccountEntity): Long {
        val id = db.portOfficerDao().insert(item)
        appendAuditLog(
            actionType = "PORT_OFFICER_SAVED",
            entityType = "PortOfficer",
            entityId = id.toString(),
            summary = "تحديث دليل ضباط المنافذ: ${item.fullName}",
            details = "المنفذ: ${item.assignedPort}",
            user = actor
        )
        return id
    }

    suspend fun recordWatchlistAlert(alert: WatchlistAlertEntity, actor: UserAccountEntity): Long {
        val id = db.watchlistAlertDao().insert(alert)
        appendAuditLog(
            actionType = "WATCHLIST_HIT",
            entityType = "WatchlistAlert",
            entityId = id.toString(),
            summary = "تطابق أمني حرج: ${alert.matchedName} (${alert.matchedDocumentNumber})",
            details = alert.details,
            user = actor,
            severity = "CRITICAL"
        )
        return id
    }

    suspend fun acknowledgeAlert(alertId: Long, actor: UserAccountEntity) {
        db.watchlistAlertDao().acknowledgeAlert(alertId, actor.fullName)
        appendAuditLog(
            actionType = "WATCHLIST_ALERT_ACK",
            entityType = "WatchlistAlert",
            entityId = alertId.toString(),
            summary = "مراجعة واعتماد تنبيه أمني #$alertId",
            details = "بواسطة: ${actor.fullName}",
            user = actor
        )
    }

    // --- Immutable Audit Log & Chain Integrity Verification ---

    suspend fun appendAuditLog(
        actionType: String,
        entityType: String,
        entityId: String,
        summary: String,
        details: String,
        user: UserAccountEntity,
        severity: String = "INFO"
    ) {
        auditMutex.withLock {
            val timestamp = System.currentTimeMillis()
            val prevHash = db.auditLogDao().getLatestIntegrityHash() ?: "GENESIS_SOVEREIGN_BORDER"
            val payload = "$prevHash|$timestamp|$actionType|$entityType|$entityId|${user.username}|$summary|$details"
            val hash = PasswordHasher.sha256Hex(payload)
            db.auditLogDao().insertImmutable(
                AuditLogEntity(
                    actionType = actionType,
                    entityType = entityType,
                    entityId = entityId,
                    summary = summary,
                    details = details,
                    username = user.username,
                    userRole = user.role.arabicLabel,
                    portName = user.assignedPort,
                    severity = severity,
                    timestamp = timestamp,
                    integrityHash = hash
                )
            )
        }
    }

    suspend fun verifyAuditChainIntegrity(): Boolean {
        val logs = db.auditLogDao().getAllLogsChronological()
        var prevHash = "GENESIS_SOVEREIGN_BORDER"
        for (log in logs) {
            val payload = "$prevHash|${log.timestamp}|${log.actionType}|${log.entityType}|${log.entityId}|${log.username}|${log.summary}|${log.details}"
            val expected = PasswordHasher.sha256Hex(payload)
            if (expected != log.integrityHash) return false
            prevHash = log.integrityHash
        }
        return true
    }

    suspend fun getAllTravelersOnce() = db.travelerDao().getAllTravelersOnce()
    suspend fun getActiveSecurityReportsOnce() = db.securityReportDao().getActiveOnce()
    suspend fun getAllSeizuresOnce() = db.passportSeizureDao().getAllOnce()
    suspend fun getAllAuditLogsOnce() = db.auditLogDao().getAllLogsChronological()
}
