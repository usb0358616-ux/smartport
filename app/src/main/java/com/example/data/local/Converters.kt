package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.MergeChangeRecord
import com.example.domain.model.PermitStatus
import com.example.domain.model.SecurityReportType
import com.example.domain.model.SecuritySeverity
import com.example.domain.model.SeizureStatus
import com.example.domain.model.SignaturePoint
import com.example.domain.model.UserRole
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromMergeHistory(list: List<MergeChangeRecord>): String = json.encodeToString(list)

    @TypeConverter
    fun toMergeHistory(raw: String): List<MergeChangeRecord> =
        if (raw.isBlank()) emptyList() else runCatching {
            json.decodeFromString<List<MergeChangeRecord>>(raw)
        }.getOrDefault(emptyList())

    @TypeConverter
    fun fromSignatureStrokes(list: List<SignaturePoint>): String = json.encodeToString(list)

    @TypeConverter
    fun toSignatureStrokes(raw: String): List<SignaturePoint> =
        if (raw.isBlank()) emptyList() else runCatching {
            json.decodeFromString<List<SignaturePoint>>(raw)
        }.getOrDefault(emptyList())

    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(name: String): UserRole = runCatching { UserRole.valueOf(name) }.getOrDefault(UserRole.DATA_ENTRY)

    @TypeConverter
    fun fromSeizureStatus(status: SeizureStatus): String = status.name

    @TypeConverter
    fun toSeizureStatus(name: String): SeizureStatus =
        runCatching { SeizureStatus.valueOf(name) }.getOrDefault(SeizureStatus.NEW)

    @TypeConverter
    fun fromSecurityReportType(type: SecurityReportType): String = type.name

    @TypeConverter
    fun toSecurityReportType(name: String): SecurityReportType =
        runCatching { SecurityReportType.valueOf(name) }.getOrDefault(SecurityReportType.WANTED)

    @TypeConverter
    fun fromSecuritySeverity(severity: SecuritySeverity): String = severity.name

    @TypeConverter
    fun toSecuritySeverity(name: String): SecuritySeverity =
        runCatching { SecuritySeverity.valueOf(name) }.getOrDefault(SecuritySeverity.HIGH)

    @TypeConverter
    fun fromPermitStatus(status: PermitStatus): String = status.name

    @TypeConverter
    fun toPermitStatus(name: String): PermitStatus =
        runCatching { PermitStatus.valueOf(name) }.getOrDefault(PermitStatus.ACTIVE)
}
