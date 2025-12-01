@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.gymlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

@Entity(tableName = "calendars")
@Serializable
data class CalendarEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
