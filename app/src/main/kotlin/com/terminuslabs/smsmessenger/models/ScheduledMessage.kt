package com.terminuslabs.smsmessenger.models

import androidx.room.*

import java.util.*


@Entity(tableName = "scheduled_message")
class ScheduledMessage {
    @PrimaryKey
    var id : Long =0;

    @ColumnInfo(name = "phone_number")
    var phoneNumber: String = ""

    @ColumnInfo(name = "body")
    var body: String? = null

    @ColumnInfo(name = "scheduled_date")
    var scheduledDate: Date? = null


    @ColumnInfo(name = "creation_date")
    var creationDate: Date? = null


    @ColumnInfo(name = "state")
    var state: String = STATE_PENDING


    @ColumnInfo(name = "result_code")
    var resultCode: Int?= null

    @Ignore
    var contactName: String? = null

    companion object{
        const val STATE_PENDING= "PENDING"
        const val STATE_SENT = "SENT"
        const val STATE_ERROR = "ERROR"
    }
}
