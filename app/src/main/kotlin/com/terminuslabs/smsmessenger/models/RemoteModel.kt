package com.terminuslabs.smsmessenger.models

import java.util.*

object RemoteModel {
    data class ResponseMessage(val id: Long, val phoneNumber: String, val body : String, val scheduledDate: Date)
    data class RequestMessageInfo(val id: Long, val status: String, val resultCode : Int?)
}

