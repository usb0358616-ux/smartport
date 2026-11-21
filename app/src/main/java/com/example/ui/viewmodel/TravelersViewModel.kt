package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BorderRepository
import com.example.data.local.entities.TravelerEntity
import com.example.data.local.entities.UserAccountEntity
import com.example.domain.importer.ExcelExportEngine
import com.example.domain.ocr.ArabicTextNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TravelerFilter(val arabicLabel: String) {
    ALL("الكل"),
    ALLOWED("مسموح بالسفر"),
    BLOCKED("ممنوع / إرتجاع"),
    EXPIRED_PASSPORT("جواز منتهي")
}

class TravelersViewModel(
    private val repository: BorderRepository,
    private val exportEngine: ExcelExportEngine
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(TravelerFilter.ALL)
    val filter: StateFlow<TravelerFilter> = _filter.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val filteredTravelers: StateFlow<List<TravelerEntity>> = combine(
        repository.travelers,
        _searchQuery,
        _filter
    ) { list, query, activeFilter ->
        val normQuery = ArabicTextNormalizer.normalizeForMatching(query)
        list.filter { t ->
            val matchesFilter = when (activeFilter) {
                TravelerFilter.ALL -> true
                TravelerFilter.ALLOWED -> t.travelAllowed == "نعم" && t.returnReason.isBlank()
                TravelerFilter.BLOCKED -> t.travelAllowed == "لا" || t.returnReason.isNotBlank()
                TravelerFilter.EXPIRED_PASSPORT -> t.passportStatus.contains("منتهي") ||
                    ArabicTextNormalizer.isDateExpired(t.expiryDate)
            }
            if (!matchesFilter) return@filter false
            if (normQuery.isBlank()) return@filter true

            val hay = ArabicTextNormalizer.normalizeForMatching(
                "${t.fullArabicName} ${t.passportName} ${t.idNumber} ${t.nationalId} ${t.country} ${t.agency} ${t.center} ${t.portName} ${t.destination}"
            )
            hay.contains(normQuery) || ArabicTextNormalizer.normalizeDocumentId(t.idNumber).contains(
                ArabicTextNormalizer.normalizeDocumentId(query)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setFilter(f: TravelerFilter) {
        _filter.value = f
    }

    fun saveTraveler(traveler: TravelerEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            val computedAge = traveler.age ?: ArabicTextNormalizer.calculateAge(traveler.birthDate)
            val statusPassport = if (ArabicTextNormalizer.isDateExpired(traveler.expiryDate)) "منتهي" else traveler.passportStatus
            val normalized = traveler.copy(
                age = computedAge,
                passportStatus = statusPassport,
                birthDate = ArabicTextNormalizer.normalizeDateString(traveler.birthDate),
                expiryDate = ArabicTextNormalizer.normalizeDateString(traveler.expiryDate),
                issueDate = ArabicTextNormalizer.normalizeDateString(traveler.issueDate),
                investigatingOfficer = traveler.investigatingOfficer.ifBlank { actor.fullName }
            )
            val (_, merged) = repository.saveOrMergeTraveler(normalized, "إدخال يدوي / تعديل", actor)
            _feedbackMessage.value = if (merged) {
                "تم الدمج الذكي مع السجل السابق وتحديث سجل التغييرات"
            } else {
                "تم حفظ سجل المسافر بنجاح"
            }
        }
    }

    fun deleteTraveler(traveler: TravelerEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            repository.deleteTraveler(actor, traveler)
                .onSuccess { _feedbackMessage.value = "تم حذف السجل وتسجيله في التدقيق" }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun exportToXlsx(uri: Uri) {
        viewModelScope.launch {
            val data = filteredTravelers.value
            exportEngine.exportTravelersXlsx(uri, data)
                .onSuccess { count -> _feedbackMessage.value = "تم تصدير $count سجل مسافر إلى ملف Excel" }
                .onFailure { err -> _feedbackMessage.value = "فشل التصدير: ${err.message}" }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
