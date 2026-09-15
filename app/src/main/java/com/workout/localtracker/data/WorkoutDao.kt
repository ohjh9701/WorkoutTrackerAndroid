package com.workout.localtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workouts ORDER BY date ASC, createdAt ASC")
    fun observeWorkouts(): Flow<List<WorkoutWithSets>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getWorkout(id: Long): WorkoutWithSets?

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId")
    suspend fun deleteSets(workoutId: Long)

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query("SELECT * FROM goals ORDER BY yearMonth ASC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Upsert
    suspend fun upsertGoal(goal: GoalEntity)
}
