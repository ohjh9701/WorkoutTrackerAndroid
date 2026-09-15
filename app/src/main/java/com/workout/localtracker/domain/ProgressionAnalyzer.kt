package com.workout.localtracker.domain

import com.workout.localtracker.data.ExerciseRecord
import com.workout.localtracker.data.WorkoutWithSets
import com.workout.localtracker.data.exerciseRecords
import kotlin.math.abs
import kotlin.math.max

enum class ProgressStatus {
    NEW, IMPROVE, SAME, DECLINE, MIXED
}

data class PerformanceMetrics(
    val maxWeight: Double,
    val totalVolume: Double,
    val estimated1RM: Double
)

data class ExerciseProgression(
    val exerciseName: String,
    val status: ProgressStatus,
    val current: PerformanceMetrics,
    val previous: PerformanceMetrics?,
    val previousDate: String? = null
)

data class WorkoutProgression(
    val status: ProgressStatus,
    val improve: Int,
    val same: Int,
    val decline: Int,
    val newCount: Int,
    val exercises: List<ExerciseProgression>
)

object ProgressionAnalyzer {
    fun analyze(workouts: List<WorkoutWithSets>): Map<Long, WorkoutProgression> {
        val sorted = workouts.sortedWith(
            compareBy<WorkoutWithSets> { it.workout.date }
                .thenBy { it.workout.createdAt }
                .thenBy { it.workout.id }
        )

        data class Previous(
            val date: String,
            val metrics: PerformanceMetrics
        )

        val previousByExercise = mutableMapOf<String, Previous>()
        val result = mutableMapOf<Long, WorkoutProgression>()

        sorted.forEach { workout ->
            val currentProgressions = mutableListOf<ExerciseProgression>()
            val pending = mutableMapOf<String, Previous>()

            workout.exerciseRecords().forEach { exercise ->
                val key = normalize(exercise.name)
                val current = metrics(exercise)
                val previous = previousByExercise[key]

                val status = when {
                    previous == null -> ProgressStatus.NEW
                    current.estimated1RM > previous.metrics.estimated1RM + 0.05 -> ProgressStatus.IMPROVE
                    current.estimated1RM < previous.metrics.estimated1RM - 0.05 -> ProgressStatus.DECLINE
                    current.totalVolume > previous.metrics.totalVolume + 0.5 -> ProgressStatus.IMPROVE
                    current.totalVolume < previous.metrics.totalVolume - 0.5 -> ProgressStatus.DECLINE
                    else -> ProgressStatus.SAME
                }

                currentProgressions += ExerciseProgression(
                    exerciseName = exercise.name,
                    status = status,
                    current = current,
                    previous = previous?.metrics,
                    previousDate = previous?.date
                )

                pending[key] = Previous(
                    date = workout.workout.date,
                    metrics = current
                )
            }

            val improve = currentProgressions.count { it.status == ProgressStatus.IMPROVE }
            val same = currentProgressions.count { it.status == ProgressStatus.SAME }
            val decline = currentProgressions.count { it.status == ProgressStatus.DECLINE }
            val newCount = currentProgressions.count { it.status == ProgressStatus.NEW }
            val comparable = improve + same + decline

            val summary = when {
                comparable == 0 -> ProgressStatus.NEW
                improve > 0 && decline > 0 -> ProgressStatus.MIXED
                improve > 0 -> ProgressStatus.IMPROVE
                decline > 0 -> ProgressStatus.DECLINE
                else -> ProgressStatus.SAME
            }

            result[workout.workout.id] = WorkoutProgression(
                status = summary,
                improve = improve,
                same = same,
                decline = decline,
                newCount = newCount,
                exercises = currentProgressions
            )

            previousByExercise.putAll(pending)
        }

        return result
    }

    fun metrics(exercise: ExerciseRecord): PerformanceMetrics {
        var maxWeight = 0.0
        var totalVolume = 0.0
        var e1rm = 0.0

        exercise.sets.forEach { set ->
            maxWeight = max(maxWeight, set.weight)
            totalVolume += set.weight * set.reps
            if (set.weight > 0 && set.reps > 0) {
                e1rm = max(e1rm, set.weight * (1.0 + set.reps / 30.0))
            }
        }

        return PerformanceMetrics(
            maxWeight = round1(maxWeight),
            totalVolume = round1(totalVolume),
            estimated1RM = round1(e1rm)
        )
    }

    private fun normalize(value: String): String =
        value.trim().replace(Regex("""\s+"""), " ").lowercase()

    private fun round1(value: Double): Double =
        kotlin.math.round(value * 10.0) / 10.0
}
