package com.gymlog.app.data.local.dao

import androidx.room.*
import com.gymlog.app.data.local.entity.SetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId")
    fun getSetsForExercise(exerciseId: String): Flow<List<SetEntity>>

    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId")
    suspend fun getSetsForExerciseSync(exerciseId: String): List<SetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<SetEntity>)

    @Update
    suspend fun updateSet(set: SetEntity)

    @Delete
    suspend fun deleteSet(set: SetEntity)

    @Query("DELETE FROM sets WHERE exerciseId = :exerciseId")
    suspend fun deleteSetsForExercise(exerciseId: String)

    @Query("SELECT * FROM sets")
    suspend fun getAllSets(): List<SetEntity>

    @Query("DELETE FROM sets")
    suspend fun deleteAllSets()
}