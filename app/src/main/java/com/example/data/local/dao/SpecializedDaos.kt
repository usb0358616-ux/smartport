package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ForeignerResidentEntity
import com.example.data.local.entities.InternalMovementPermitEntity
import com.example.data.local.entities.MofaVisaRequestEntity
import com.example.data.local.entities.PassportSeizureEntity
import com.example.data.local.entities.PortOfficerEntity
import com.example.data.local.entities.RefugeeRecordEntity
import com.example.data.local.entities.TravelAgencyEntity
import com.example.data.local.entities.UndocumentedMigrantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PassportSeizureDao {
    @Query("SELECT * FROM passport_seizures ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PassportSeizureEntity>>

    @Query("SELECT * FROM passport_seizures")
    suspend fun getAllOnce(): List<PassportSeizureEntity>

    @Query("SELECT COUNT(*) FROM passport_seizures")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PassportSeizureEntity): Long

    @Update
    suspend fun update(item: PassportSeizureEntity)

    @Delete
    suspend fun delete(item: PassportSeizureEntity)
}

@Dao
interface MofaVisaRequestDao {
    @Query("SELECT * FROM mofa_visa_requests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MofaVisaRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MofaVisaRequestEntity): Long

    @Update
    suspend fun update(item: MofaVisaRequestEntity)

    @Delete
    suspend fun delete(item: MofaVisaRequestEntity)
}

@Dao
interface ForeignerResidentDao {
    @Query("SELECT * FROM foreigner_residents ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ForeignerResidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ForeignerResidentEntity): Long

    @Update
    suspend fun update(item: ForeignerResidentEntity)

    @Delete
    suspend fun delete(item: ForeignerResidentEntity)
}

@Dao
interface RefugeeRecordDao {
    @Query("SELECT * FROM refugee_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RefugeeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RefugeeRecordEntity): Long

    @Delete
    suspend fun delete(item: RefugeeRecordEntity)
}

@Dao
interface UndocumentedMigrantDao {
    @Query("SELECT * FROM undocumented_migrants ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UndocumentedMigrantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: UndocumentedMigrantEntity): Long

    @Update
    suspend fun update(item: UndocumentedMigrantEntity)

    @Delete
    suspend fun delete(item: UndocumentedMigrantEntity)
}

@Dao
interface InternalMovementPermitDao {
    @Query("SELECT * FROM internal_movement_permits ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InternalMovementPermitEntity>>

    @Query("SELECT * FROM internal_movement_permits WHERE permitCode = :code LIMIT 1")
    suspend fun findByPermitCode(code: String): InternalMovementPermitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InternalMovementPermitEntity): Long

    @Update
    suspend fun update(item: InternalMovementPermitEntity)

    @Delete
    suspend fun delete(item: InternalMovementPermitEntity)
}

@Dao
interface TravelAgencyDao {
    @Query("SELECT * FROM travel_agencies ORDER BY agencyName ASC")
    fun observeAll(): Flow<List<TravelAgencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TravelAgencyEntity): Long

    @Delete
    suspend fun delete(item: TravelAgencyEntity)
}

@Dao
interface PortOfficerDao {
    @Query("SELECT * FROM port_officers ORDER BY fullName ASC")
    fun observeAll(): Flow<List<PortOfficerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PortOfficerEntity): Long

    @Delete
    suspend fun delete(item: PortOfficerEntity)
}
