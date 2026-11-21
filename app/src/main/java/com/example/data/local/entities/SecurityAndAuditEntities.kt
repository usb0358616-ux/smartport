package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.SecurityReportType
import com.example.domain.model.SecuritySeverity
import com.example.domain.model.UserRole

@Entity(
    tableName = "security_reports",
    indices = [Index(value = ["passportOrIdNumber"]), Index(value = ["isActive"])]
)
data class SecurityReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val circularNumber: String,
    val subjectName: String,
    val passportOrIdNumber: String = "",
    val birthDate: String = "",
    val nationality: String = "",
    val reportType: SecurityReportType = SecurityReportType.WANTED,
    val severity: SecuritySeverity = SecuritySeverity.HIGH,
    val issuingAuthority: String = "",
    val reason: String,
    val actionRequired: String = "الضبط والإحالة الفورية",
    val isActive: Boolean = true,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val badgeNumber: String,
    val role: UserRole,
    val assignedPort: String,
    val passwordHash: String,
    val passwordSalt: String,
    val biometricEnabled: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["timestamp"]), Index(value = ["actionType"])]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val entityType: String,
    val entityId: String = "",
    val summary: String,
    val details: String = "",
    val username: String,
    val userRole: String,
    val portName: String,
    val severity: String = "INFO",
    val timestamp: Long = System.currentTimeMillis(),
    val integrityHash: String = ""
)

@Entity(
    tableName = "watchlist_alerts",
    indices = [Index(value = ["isAcknowledged"]), Index(value = ["timestamp"])]
)
data class WatchlistAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceModule: String,
    val matchedName: String,
    val matchedDocumentNumber: String,
    val matchedAgainstType: String,
    val matchedCircularOrRecordId: String,
    val matchScore: Double,
    val severity: SecuritySeverity,
    val details: String,
    val officerName: String,
    val portName: String,
    val isAcknowledged: Boolean = false,
    val acknowledgedBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
