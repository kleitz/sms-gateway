package com.terminuslabs.smsmessenger.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.terminuslabs.smsmessenger.models.ScheduledMessage
import java.util.*

@Dao
interface ScheduledMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(message: ScheduledMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    fun insertOrUpdateAll(messages: List<ScheduledMessage>)

    @Query("SELECT * FROM scheduled_message")
    fun getAll(): List<ScheduledMessage>


    @Query("SELECT * FROM scheduled_message WHERE  state= :filterState")
    fun getByState(filterState: String): List<ScheduledMessage>


    @Query("SELECT * FROM scheduled_message WHERE  state <> :filterState ORDER BY scheduled_date")
    fun getByDistinctState(filterState: String): List<ScheduledMessage>


    @Query("SELECT * FROM scheduled_message WHERE  state= :filterState AND scheduled_date <= :maxDate")
    fun getToSend(filterState: String, maxDate: Date): List<ScheduledMessage>

    @Query("UPDATE scheduled_message SET state = :newState WHERE id = :id")
    fun changeState(id: Long, newState: String)

    @Query("UPDATE scheduled_message SET result_code = :newCode WHERE id = :id")
    fun changeResult(id: Long, newCode: Int)

    @Query("DELETE FROM scheduled_message WHERE id = :id")
    fun delete(id: Long)

    @Query("DELETE FROM scheduled_message WHERE id IN (:ids)")
    fun delete(ids: List<Long>)


}
