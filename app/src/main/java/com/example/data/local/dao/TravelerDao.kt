package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.TravelerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelerDao {
    @Query("SELECT * FROM travelers ORDER BY updatedAt DESC")
    fun observeAllTravelers(): Flow<List<TravelerEntity>>

    @Query("SELECT * FROM travelers")
    suspend fun getAllTravelersOnce(): List<TravelerEntity>

    @Query("SELECT * FROM travelers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TravelerEntity?

    @Query("SELECT * FROM travelers WHERE UPPER(REPLACE(idNumber, ' ', '')) = :normalizedId LIMIT 1")
    suspend fun findByNormalizedIdNumber(normalizedId: String): TravelerEntity?

    @Query("SELECT COUNT(*) FROM travelers")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM travelers WHERE travelAllowed = 'نعم'")
    fun observeAllowedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM travelers WHERE travelAllowed = 'لا' OR returnReason != ''")
    fun observeBlockedOrReturnedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(traveler: TravelerEntity): Long

    @Update
    suspend fun update(traveler: TravelerEntity)

    @Delete
    suspend fun delete(traveler: TravelerEntity)
}
