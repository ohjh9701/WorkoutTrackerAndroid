package com.workout.localtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workout.localtracker.data.ExerciseRecord
import com.workout.localtracker.data.GoalEntity
import com.workout.localtracker.data.WorkoutDatabase
import com.workout.localtracker.data.WorkoutDraft
import com.workout.localtracker.data.WorkoutRepository
import com.workout.localtracker.data.WorkoutWithSets
import com.workout.localtracker.data.exerciseRecords
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PreviousExerciseRecord(
    val workoutId: Long,
    val date: String,
    val title: String,
    val bodyPart: String,
    val exercise: ExerciseRecord
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(
        WorkoutDatabase.getInstance(application)
    )

    val workouts = repository.workouts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val goals = repository.goals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun saveWorkout(
        draft: WorkoutDraft,
        editingId: Long?,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(
                runCatching {
                    repository.saveWorkout(draft, editingId)
                }
            )
        }
    }

    fun deleteWorkout(
        workout: WorkoutWithSets,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(
                runCatching {
                    repository.deleteWorkout(workout)
                }
            )
        }
    }

    fun saveGoal(
        goal: GoalEntity,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(
                runCatching {
                    repository.saveGoal(goal)
                }
            )
        }
    }

    fun knownExerciseNames(): List<String> =
        workouts.value
            .flatMap { it.exerciseRecords() }
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()

    fun latestExerciseRecord(
        exerciseName: String,
        currentDate: String,
        excludeWorkoutId: Long? = null
    ): PreviousExerciseRecord? {
        val normalized = normalize(exerciseName)
        if (normalized.isBlank()) return null

        return workouts.value
            .asSequence()
            .filter { it.workout.id != excludeWorkoutId }
            .filter { it.workout.date <= currentDate }
            .flatMap { workout ->
                workout.exerciseRecords()
                    .asSequence()
                    .filter { normalize(it.name) == normalized }
                    .map { exercise ->
                        PreviousExerciseRecord(
                            workoutId = workout.workout.id,
                            date = workout.workout.date,
                            title = workout.workout.title,
                            bodyPart = workout.workout.bodyPart,
                            exercise = exercise
                        )
                    }
            }
            .maxWithOrNull(
                compareBy<PreviousExerciseRecord> { it.date }
                    .thenBy { record ->
                        workouts.value
                            .firstOrNull { it.workout.id == record.workoutId }
                            ?.workout
                            ?.createdAt ?: 0L
                    }
            )
    }

    private fun normalize(value: String): String =
        value.trim().replace(Regex("""\s+"""), " ").lowercase()
}
