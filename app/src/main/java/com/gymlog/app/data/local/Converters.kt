package com.gymlog.app.data.local

import androidx.room.TypeConverter
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
}
