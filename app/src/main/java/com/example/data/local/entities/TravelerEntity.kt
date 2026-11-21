package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.MergeChangeRecord

@Entity(
    tableName = "travelers",
    indices = [
        Index(value = ["idNumber"]),
        Index(value = ["passportName"]),
        Index(value = ["travelAllowed"]),
        Index(value = ["center"])
    ]
)
data class TravelerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serialNumber: String = "",
    val firstName: String = "",
    val secondName: String = "",
    val thirdName: String = "",
    val fourthName: String = "",
    val lastName: String = "",
    val passportName: String = "",
    val idNumber: String = "",
    val idType: String = "جواز سفر",
    val passportStatus: String = "صالح",
    val nationalId: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val age: Int? = null,
    val country: String = "",
    val governorate: String = "",
    val profession: String = "",
    val phone: String = "",
    val center: String = "",
    val portName: String = "",
    val agency: String = "",
    val visaType: String = "",
    val destination: String = "",
    val visitReason: String = "",
    val receiveDate: String = "",
    val exitDate: String = "",
    val issueDate: String = "",
    val expiryDate: String = "",
    val placeOfIssue: String = "",
    val requestNumber: String = "",
    val returnReason: String = "",
    val vettingResult: String = "",
    val travelAllowed: String = "نعم",
    val status: String = "مكتمل",
    val investigatingOfficer: String = "",
    val additionalInfo: String = "",
    val mergeHistory: List<MergeChangeRecord> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val fullArabicName: String
        get() = listOf(firstName, secondName, thirdName, fourthName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { passportName }
}
