package com.phoniq.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phoniq.app.data.db.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE is_starred = 1 ORDER BY name ASC")
    fun observeStarred(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE number = :number LIMIT 1")
    suspend fun findByNumber(number: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR number LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}
