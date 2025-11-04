package com.gymlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

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
data class MonthEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val calendarId: String,
    val name: String,
    val monthNumber: Int  // 1, 2, 3... para ordenar
)
