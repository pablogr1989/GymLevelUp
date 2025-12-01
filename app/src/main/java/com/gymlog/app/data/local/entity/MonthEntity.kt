@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.gymlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable
@Entity(
    tableName = "months",
    foreignKeys = [
        ForeignKey(
            entity = CalendarEntity::class,
            parentColumns = ["id"],
            childColumns = ["calendarId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("calendarId"), Index("monthNumber")]
)
@Serializable
data class MonthEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val calendarId: String,
    val name: String,
    val monthNumber: Int  // 1, 2, 3... para ordenar
)
