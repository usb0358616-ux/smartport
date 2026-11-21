package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.SecurityReportEntity
import com.example.data.local.entities.UserAccountEntity
import com.example.data.local.entities.WatchlistAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityReportDao {
    @Query("SELECT * FROM security_reports ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SecurityReportEntity>>

    @Query("SELECT * FROM security_reports WHERE isActive = 1")
    suspend fun getActiveOnce(): List<SecurityReportEntity>

    @Query("SELECT COUNT(*) FROM security_reports WHERE isActive = 1")
    fun observeActiveCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SecurityReportEntity): Long

    @Update
    suspend fun update(item: SecurityReportEntity)

    @Delete
    suspend fun delete(item: SecurityReportEntity)
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts ORDER BY createdAt ASC")
    fun observeAllUsers(): Flow<List<UserAccountEntity>>

    @Query("SELECT COUNT(*) FROM user_accounts")
    fun observeUserCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun getUserCountOnce(): Int

    @Query("SELECT * FROM user_accounts WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserAccountEntity): Long

    @Update
    suspend fun update(user: UserAccountEntity)
}

/**
 * Append-only immutable Audit Log DAO. Updates and Deletes are intentionally omitted.
 */
@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC, id DESC")
    fun observeAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp ASC, id ASC")
    suspend fun getAllLogsChronological(): List<AuditLogEntity>

    @Query("SELECT integrityHash FROM audit_logs ORDER BY id DESC LIMIT 1")
    suspend fun getLatestIntegrityHash(): String?

    @Insert
    suspend fun insertImmutable(log: AuditLogEntity): Long
}

@Dao
interface WatchlistAlertDao {
    @Query("SELECT * FROM watchlist_alerts ORDER BY isAcknowledged ASC, timestamp DESC")
    fun observeAllAlerts(): Flow<List<WatchlistAlertEntity>>

    @Query("SELECT * FROM watchlist_alerts WHERE isAcknowledged = 0 ORDER BY timestamp DESC")
    fun observeUnacknowledgedAlerts(): Flow<List<WatchlistAlertEntity>>

    @Insert
    suspend fun insert(alert: WatchlistAlertEntity): Long

    @Query("UPDATE watchlist_alerts SET isAcknowledged = 1, acknowledgedBy = :officerName WHERE id = :alertId")
    suspend fun acknowledgeAlert(alertId: Long, officerName: String)
}
