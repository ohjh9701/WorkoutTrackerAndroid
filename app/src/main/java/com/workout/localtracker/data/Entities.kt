package com.workout.localtracker.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "workouts",
    indices = [Index("date"), Index("bodyPart")]
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val title: String,
    val bodyPart: String,
    val memo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId"), Index("exerciseName")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseOrder: Int,
    val exerciseName: String,
    val setNo: Int,
    val weight: Double,
    val reps: Int
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val yearMonth: String,
    val totalGoal: Int = 20,
    val backGoal: Int = 4,
    val chestGoal: Int = 4,
    val legsGoal: Int = 4,
    val shouldersGoal: Int = 4,
    val updatedAt: Long = System.currentTimeMillis()
)

data class WorkoutWithSets(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val sets: List<WorkoutSetEntity>
)

data class ExerciseRecord(
    val order: Int,
    val name: String,
    val sets: List<WorkoutSetEntity>
)

fun WorkoutWithSets.exerciseRecords(): List<ExerciseRecord> =
    sets
        .groupBy { it.exerciseOrder to it.exerciseName }
        .map { (key, values) ->
            ExerciseRecord(
                order = key.first,
                name = key.second,
                sets = values.sortedBy { it.setNo }
            )
        }
        .sortedBy { it.order }
