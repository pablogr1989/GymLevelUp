package com.gymlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "day_slots",
    foreignKeys = [
        ForeignKey(
            entity = WeekEntity::class,
            parentColumns = ["id"],
            childColumns = ["weekId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("weekId"), Index("dayOfWeek")]
)
data class DaySlotEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val weekId: String,
    val dayOfWeek: DayOfWeek,
    val categoryList: String = "",  // comma-separated
    val selectedExerciseIds: String = "",  // comma-separated
    val completed: Boolean = false
)

enum class DayOfWeek(val displayName: String, val dayNumber: Int) {
    MONDAY("Lunes", 1),
    TUESDAY("Martes", 2),
    WEDNESDAY("Miércoles", 3),
    THURSDAY("Jueves", 4),
    FRIDAY("Viernes", 5),
    SATURDAY("Sábado", 6),
    SUNDAY("Domingo", 7);

    companion object {
        fun fromDayNumber(number: Int): DayOfWeek {
            return values().find { it.dayNumber == number } ?: MONDAY
        }
    }
}

enum class DayCategory(val displayName: String, val abbreviation: String) {
    FULL_BODY("Full Body", "FB"),
    CHEST("Torso", "TO"),
    LEGS("Pierna", "PI"),
    GLUTES("Glúteo", "GL"),
    BACK("Espalda", "ES"),
    BICEPS("Bíceps", "BI"),
    TRICEPS("Tríceps", "TR"),
    SHOULDERS("Hombros", "HO"),
    CARDIO("Cardio", "CA"),
    REST("Descanso", "DE");

    companion object {
        fun fromDisplayName(name: String): DayCategory? {
            return values().find { it.displayName == name }
        }
    }
}