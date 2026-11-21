package com.example.domain.model

import com.example.data.local.entities.TravelerEntity

object TravelerSmartMerge {

    fun merge(
        existing: TravelerEntity,
        incoming: TravelerEntity,
        source: String,
        officerName: String,
        allowUnblockOverride: Boolean = false
    ): Pair<TravelerEntity, List<String>> {
        val changedFields = mutableListOf<String>()

        fun pick(fieldName: String, oldVal: String, newVal: String): String {
            val cleanNew = newVal.trim()
            val cleanOld = oldVal.trim()
            return if (cleanNew.isNotEmpty() && cleanNew != cleanOld) {
                if (cleanOld.isEmpty()) {
                    changedFields.add("$fieldName (إضافة)")
                    cleanNew
                } else {
                    changedFields.add("$fieldName: $cleanOld -> $cleanNew")
                    cleanNew
                }
            } else {
                cleanOld
            }
        }

        // Security precedence: never silently override an existing "لا" ban unless leadership overrides
        val mergedTravelAllowed = when {
            existing.travelAllowed == "لا" && incoming.travelAllowed == "نعم" && !allowUnblockOverride -> {
                "لا"
            }
            incoming.travelAllowed.isNotBlank() && incoming.travelAllowed != existing.travelAllowed -> {
                changedFields.add("السماح بالسفر: ${existing.travelAllowed} -> ${incoming.travelAllowed}")
                incoming.travelAllowed
            }
            else -> existing.travelAllowed
        }

        val mergedAge = incoming.age ?: existing.age
        if (incoming.age != null && incoming.age != existing.age) {
            changedFields.add("العمر: ${existing.age ?: "-"} -> ${incoming.age}")
        }

        val serialNumber = pick("الرقم التسلسلي", existing.serialNumber, incoming.serialNumber)
        val firstName = pick("الاسم الأول", existing.firstName, incoming.firstName)
        val secondName = pick("الاسم الثاني", existing.secondName, incoming.secondName)
        val thirdName = pick("الاسم الثالث", existing.thirdName, incoming.thirdName)
        val fourthName = pick("الاسم الرابع", existing.fourthName, incoming.fourthName)
        val lastName = pick("اللقب", existing.lastName, incoming.lastName)
        val passportName = pick("الاسم بالجواز", existing.passportName, incoming.passportName)
        val idNumber = pick("رقم الوثيقة", existing.idNumber, incoming.idNumber)
        val idType = pick("نوع الوثيقة", existing.idType, incoming.idType)
        val passportStatus = pick("حالة الجواز", existing.passportStatus, incoming.passportStatus)
        val nationalId = pick("الرقم الوطني", existing.nationalId, incoming.nationalId)
        val gender = pick("الجنس", existing.gender, incoming.gender)
        val birthDate = pick("تاريخ الميلاد", existing.birthDate, incoming.birthDate)
        val country = pick("الدولة", existing.country, incoming.country)
        val governorate = pick("المحافظة", existing.governorate, incoming.governorate)
        val profession = pick("المهنة", existing.profession, incoming.profession)
        val phone = pick("الهاتف", existing.phone, incoming.phone)
        val center = pick("المركز", existing.center, incoming.center)
        val portName = pick("المنفذ", existing.portName, incoming.portName)
        val agency = pick("الوكالة", existing.agency, incoming.agency)
        val visaType = pick("نوع التأشيرة", existing.visaType, incoming.visaType)
        val destination = pick("الوجهة", existing.destination, incoming.destination)
        val visitReason = pick("سبب الزيارة", existing.visitReason, incoming.visitReason)
        val receiveDate = pick("تاريخ الاستلام", existing.receiveDate, incoming.receiveDate)
        val exitDate = pick("تاريخ المغادرة", existing.exitDate, incoming.exitDate)
        val issueDate = pick("تاريخ الإصدار", existing.issueDate, incoming.issueDate)
        val expiryDate = pick("تاريخ الانتهاء", existing.expiryDate, incoming.expiryDate)
        val placeOfIssue = pick("مكان الإصدار", existing.placeOfIssue, incoming.placeOfIssue)
        val requestNumber = pick("رقم الطلب", existing.requestNumber, incoming.requestNumber)
        val returnReason = pick("سبب الإرجاع", existing.returnReason, incoming.returnReason)
        val vettingResult = pick("نتيجة التدقيق", existing.vettingResult, incoming.vettingResult)
        val status = pick("الحالة", existing.status, incoming.status)
        val investigatingOfficer = pick("ضابط التحقيق", existing.investigatingOfficer, incoming.investigatingOfficer)
        val additionalInfo = pick("معلومات إضافية", existing.additionalInfo, incoming.additionalInfo)

        val newHistory = if (changedFields.isNotEmpty()) {
            existing.mergeHistory + MergeChangeRecord(
                timestamp = System.currentTimeMillis(),
                source = source,
                officerName = officerName,
                changedFields = changedFields.toList()
            )
        } else {
            existing.mergeHistory
        }

        val merged = existing.copy(
            serialNumber = serialNumber,
            firstName = firstName,
            secondName = secondName,
            thirdName = thirdName,
            fourthName = fourthName,
            lastName = lastName,
            passportName = passportName,
            idNumber = idNumber,
            idType = idType,
            passportStatus = passportStatus,
            nationalId = nationalId,
            gender = gender,
            birthDate = birthDate,
            age = mergedAge,
            country = country,
            governorate = governorate,
            profession = profession,
            phone = phone,
            center = center,
            portName = portName,
            agency = agency,
            visaType = visaType,
            destination = destination,
            visitReason = visitReason,
            receiveDate = receiveDate,
            exitDate = exitDate,
            issueDate = issueDate,
            expiryDate = expiryDate,
            placeOfIssue = placeOfIssue,
            requestNumber = requestNumber,
            returnReason = returnReason,
            vettingResult = vettingResult,
            travelAllowed = mergedTravelAllowed,
            status = status,
            investigatingOfficer = investigatingOfficer,
            additionalInfo = additionalInfo,
            mergeHistory = newHistory,
            updatedAt = System.currentTimeMillis()
        )
        return merged to changedFields
    }
}
