package com.terminuslabs.smsmessenger.extensions

import android.text.TextUtils
import com.simplemobiletools.commons.models.SimpleContact
import com.terminuslabs.smsmessenger.models.Conversation

fun ArrayList<SimpleContact>.getThreadTitle() = TextUtils.join(", ", map { it.name }.toTypedArray())

fun ArrayList<Conversation>.getHashToCompare() = map { it.getStringToCompare() }.hashCode()
