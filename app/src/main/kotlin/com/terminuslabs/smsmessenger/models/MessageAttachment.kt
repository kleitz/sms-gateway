package com.terminuslabs.smsmessenger.models

data class MessageAttachment(val id: Int, var text: String, var attachments: ArrayList<Attachment>)
