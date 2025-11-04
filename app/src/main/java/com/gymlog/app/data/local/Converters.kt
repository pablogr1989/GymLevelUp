package com.gymlog.app.data.local

import androidx.room.TypeConverter
import com.gymlog.app.data.local.entity.DayOfWeek
import com.gymlog.app.data.local.entity.MuscleGroup

class Converters {
    @TypeConverter
    fun fromMuscleGroup(muscleGroup: MuscleGroup): String {
        return muscleGroup.name
    }
    
    @TypeConverter
    fun toMuscleGroup(muscleGroupName: String): MuscleGroup {
        return MuscleGroup.valueOf(muscleGroupName)
    }
    
    @TypeConverter
    fun fromDayOfWeek(dayOfWeek: DayOfWeek): String {
        return dayOfWeek.name
    }
    
    @TypeConverter
    fun toDayOfWeek(dayOfWeekName: String): DayOfWeek {
        return DayOfWeek.valueOf(dayOfWeekName)
    }
}
