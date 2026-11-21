package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.PermitStatus
import com.example.domain.model.SeizureStatus
import com.example.domain.model.SignaturePoint

@Entity(
    tableName = "passport_seizures",
    indices = [Index(value = ["passportNumber"]), Index(value = ["status"])]
)
data class PassportSeizureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seizureNumber: String,
    val holderName: String,
    val passportNumber: String,
    val nationality: String = "",
    val idType: String = "جواز سفر",
    val seizureReason: String,
    val portName: String = "",
    val officerName: String = "",
    val status: SeizureStatus = SeizureStatus.NEW,
    val forensicNotes: String = "",
    val recipientName: String = "",
    val recipientIdNumber: String = "",
    val signatureStrokes: List<SignaturePoint> = emptyList(),
    val seizedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mofa_visa_requests")
data class MofaVisaRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoNumber: String,
    val memoDate: String = "",
    val organizationName: String,
    val applicantName: String,
    val passportNumber: String,
    val nationality: String = "",
    val visaCategory: String = "دبلوماسية / إنسانية",
    val flightNumber: String = "",
    val airRoute: String = "",
    val entryPort: String = "",
    val purpose: String = "",
    val approvalStatus: String = "قيد المراجعة",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "foreigner_residents",
    indices = [Index(value = ["passportNumber"])]
)
data class ForeignerResidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val passportNumber: String,
    val nationality: String,
    val residencyNumber: String = "",
    val residencyType: String = "عمل",
    val sponsorName: String = "",
    val profession: String = "",
    val issueDate: String = "",
    val expiryDate: String = "",
    val portOfEntry: String = "",
    val status: String = "سارية",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "refugee_records")
data class RefugeeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unhcrNumber: String,
    val fullName: String,
    val nationality: String,
    val gender: String = "",
    val birthDate: String = "",
    val familySize: Int = 1,
    val campOrGovernorate: String = "",
    val arrivalPort: String = "",
    val registrationDate: String = "",
    val vulnerabilityNotes: String = "",
    val status: String = "مسجل",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "undocumented_migrants")
data class UndocumentedMigrantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseNumber: String,
    val fullName: String,
    val claimedNationality: String,
    val gender: String = "ذكر",
    val estimatedAge: Int? = null,
    val apprehensionLocation: String,
    val portOrSector: String = "",
    val apprehensionDate: String = "",
    val detentionFacility: String = "",
    val deportationStatus: String = "قيد الإجراءات",
    val officerName: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "internal_movement_permits",
    indices = [Index(value = ["permitCode"], unique = true)]
)
data class InternalMovementPermitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val permitCode: String,
    val delegationName: String,
    val organization: String,
    val leaderName: String,
    val leaderPassport: String = "",
    val membersCount: Int = 1,
    val vehiclePlate: String = "",
    val originGovernorate: String,
    val destinationGovernorate: String,
    val checkpoints: String = "",
    val validFrom: String,
    val validUntil: String,
    val status: PermitStatus = PermitStatus.ACTIVE,
    val issuedBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "travel_agencies")
data class TravelAgencyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val agencyName: String,
    val licenseNumber: String,
    val managerName: String = "",
    val phone: String = "",
    val portName: String = "",
    val processedCount: Int = 0,
    val isAccredited: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "port_officers")
data class PortOfficerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val badgeNumber: String,
    val rankTitle: String,
    val assignedPort: String,
    val shiftName: String = "الوردية الصباحية",
    val phone: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
