package com.terminuslabs.smsmessenger.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.terminuslabs.smsmessenger.interfaces.ConversationsDao
import com.terminuslabs.smsmessenger.interfaces.ScheduledMessageDao
import com.terminuslabs.smsmessenger.models.Conversation
import com.terminuslabs.smsmessenger.models.ScheduledMessage

@Database(entities = [Conversation::class, ScheduledMessage::class], version = 1)
@TypeConverters(Converters::class)
abstract class MessagesDatabase : RoomDatabase() {

    abstract fun ConversationsDao(): ConversationsDao

    abstract  fun ScheduledMessageDao(): ScheduledMessageDao

    companion object {
        private var db: MessagesDatabase? = null

        fun getInstance(context: Context): MessagesDatabase {
            if (db == null) {
                synchronized(MessagesDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, MessagesDatabase::class.java, "conversations.db")
                            .build()
                    }
                }
            }
            return db!!
        }
    }
}
