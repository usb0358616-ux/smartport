package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BorderRepository
import com.example.data.local.entities.ForeignerResidentEntity
import com.example.data.local.entities.InternalMovementPermitEntity
import com.example.data.local.entities.MofaVisaRequestEntity
import com.example.data.local.entities.PassportSeizureEntity
import com.example.data.local.entities.PortOfficerEntity
import com.example.data.local.entities.RefugeeRecordEntity
import com.example.data.local.entities.SecurityReportEntity
import com.example.data.local.entities.TravelAgencyEntity
import com.example.data.local.entities.UndocumentedMigrantEntity
import com.example.data.local.entities.UserAccountEntity
import com.example.domain.security.QrCodeHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ModulesViewModel(private val repository: BorderRepository) : ViewModel() {

    val seizures = repository.seizures.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mofaRequests = repository.mofaRequests.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val foreignerResidents = repository.foreignerResidents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val refugeeRecords = repository.refugeeRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val undocumentedMigrants = repository.undocumentedMigrants.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val movementPermits = repository.movementPermits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val securityReports = repository.securityReports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val travelAgencies = repository.travelAgencies.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val portOfficers = repository.portOfficers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _verifiedPermit = MutableStateFlow<InternalMovementPermitEntity?>(null)
    val verifiedPermit: StateFlow<InternalMovementPermitEntity?> = _verifiedPermit.asStateFlow()

    private val _moduleMessage = MutableStateFlow<String?>(null)
    val moduleMessage: StateFlow<String?> = _moduleMessage.asStateFlow()

    fun saveSeizure(item: PassportSeizureEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            repository.saveSeizure(item, actor)
            _moduleMessage.value = "تم حفظ محضر الضبط (${item.status.arabicLabel})"
        }
    }

    fun saveMofaRequest(item: MofaVisaRequestEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            repository.saveMofaRequest(item, actor)
            _moduleMessage.value = "تم حفظ مذكرة الخارجية/المنظمة"
        }
    }

    fun saveResident(item: ForeignerResidentEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            repository.saveForeignerResident(item, actor)
            _moduleMessage.value = "تم حفظ سجل الوافد الأجنبي"
        }
    }

    fun saveRefugee(item: RefugeeRecordEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            repository.saveRefugeeRecord(item, actor)
            _moduleMessage.value = "تم حفظ سجل اللاجئ"
        }
    }

    fun saveMigrant(item: UndocumentedMigrantEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            repository.saveUndocumentedMigrant(item, actor)
            _moduleMessage.value = "تم حفظ ملف الهجرة غير النظامية"
        }
    }

    fun savePermit(item: InternalMovementPermitEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            repository.saveMovementPermit(item, actor)
            _moduleMessage.value = "تم إصدار تصريح التنقل الداخلي (${item.permitCode})"
        }
    }

    fun verifyPermitQr(rawQrText: String, actor: UserAccountEntity) {
        viewModelScope.launch {
            val code = QrCodeHelper.extractPermitCodeFromQr(rawQrText)
            val found = repository.verifyMovementPermitCode(code, actor)
            _verifiedPermit.value = found
            _moduleMessage.value = if (found != null) {
                "تصريح مطابق: ${found.delegationName} (${found.status.arabicLabel})"
            } else {
                "تحذير: رمز التصريح ($code) غير مسجل في قاعدة البيانات!"
            }
        }
    }

    fun saveSecurityReport(item: SecurityReportEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            repository.saveSecurityReport(item, actor)
                .onSuccess { _moduleMessage.value = "تم اعتماد وحفظ التعميم الأمني" }
                .onFailure { _moduleMessage.value = it.message }
        }
    }

    fun toggleReportActive(item: SecurityReportEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            repository.toggleSecurityReportActive(item, actor)
                .onSuccess { _moduleMessage.value = "تم تحديث حالة التعميم الأمني" }
                .onFailure { _moduleMessage.value = it.message }
        }
    }

    fun saveAgency(item: TravelAgencyEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            repository.saveTravelAgency(item, actor)
            _moduleMessage.value = "تم حفظ بيانات الوكالة"
        }
    }

    fun savePortOfficer(item: PortOfficerEntity, actor: UserAccountEntity) {
        viewModelScope.launch {
            repository.savePortOfficer(item, actor)
            _moduleMessage.value = "تم حفظ بيانات ضابط المنفذ"
        }
    }

    fun clearMessage() {
        _moduleMessage.value = null
    }

    fun clearVerifiedPermit() {
        _verifiedPermit.value = null
    }
}
