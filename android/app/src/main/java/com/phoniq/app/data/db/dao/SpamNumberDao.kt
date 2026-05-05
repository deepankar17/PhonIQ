package com.phoniq.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phoniq.app.data.db.entity.SpamNumberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpamNumberDao {
    @Query("SELECT * FROM spam_numbers ORDER BY added_at DESC")
    fun observeAll(): Flow<List<SpamNumberEntity>>

    @Query("SELECT * FROM spam_numbers WHERE number = :number LIMIT 1")
    suspend fun findByNumber(number: String): SpamNumberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spam: SpamNumberEntity): Long

    @Delete
    suspend fun delete(spam: SpamNumberEntity)

    @Query("DELETE FROM spam_numbers WHERE number = :number")
    suspend fun deleteByNumber(number: String)

    @Query("SELECT COUNT(*) FROM spam_numbers")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(list: List<SpamNumberEntity>)
}
