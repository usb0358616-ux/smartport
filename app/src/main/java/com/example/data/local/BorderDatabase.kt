package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.ForeignerResidentDao
import com.example.data.local.dao.InternalMovementPermitDao
import com.example.data.local.dao.MofaVisaRequestDao
import com.example.data.local.dao.PassportSeizureDao
import com.example.data.local.dao.PortOfficerDao
import com.example.data.local.dao.RefugeeRecordDao
import com.example.data.local.dao.SecurityReportDao
import com.example.data.local.dao.TravelAgencyDao
import com.example.data.local.dao.TravelerDao
import com.example.data.local.dao.UndocumentedMigrantDao
import com.example.data.local.dao.UserAccountDao
import com.example.data.local.dao.WatchlistAlertDao
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
import com.example.domain.security.EncryptionKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        TravelerEntity::class,
        PassportSeizureEntity::class,
        MofaVisaRequestEntity::class,
        ForeignerResidentEntity::class,
        RefugeeRecordEntity::class,
        UndocumentedMigrantEntity::class,
        InternalMovementPermitEntity::class,
        TravelAgencyEntity::class,
        PortOfficerEntity::class,
        SecurityReportEntity::class,
        UserAccountEntity::class,
        AuditLogEntity::class,
        WatchlistAlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BorderDatabase : RoomDatabase() {
    abstract fun travelerDao(): TravelerDao
    abstract fun passportSeizureDao(): PassportSeizureDao
    abstract fun mofaVisaRequestDao(): MofaVisaRequestDao
    abstract fun foreignerResidentDao(): ForeignerResidentDao
    abstract fun refugeeRecordDao(): RefugeeRecordDao
    abstract fun undocumentedMigrantDao(): UndocumentedMigrantDao
    abstract fun internalMovementPermitDao(): InternalMovementPermitDao
    abstract fun travelAgencyDao(): TravelAgencyDao
    abstract fun portOfficerDao(): PortOfficerDao
    abstract fun securityReportDao(): SecurityReportDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun watchlistAlertDao(): WatchlistAlertDao

    companion object {
        private const val DB_NAME = "sovereign_border_ports_encrypted.db"

        fun createEncrypted(context: Context): BorderDatabase {
            System.loadLibrary("sqlcipher")
            val passphrase = EncryptionKeyManager.getOrCreateDatabasePassphrase(context)
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                BorderDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .build()
        }
    }
}
