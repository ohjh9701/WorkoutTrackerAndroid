package com.workout.localtracker.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

data class SetDraft(
    val weight: Double,
    val reps: Int
)

data class ExerciseDraft(
    val name: String,
    val sets: List<SetDraft>
)

data class WorkoutDraft(
    val date: String,
    val title: String,
    val bodyPart: String,
    val memo: String,
    val exercises: List<ExerciseDraft>
)

class WorkoutRepository(
    private val db: WorkoutDatabase
) {
    private val dao = db.workoutDao()

    val workouts: Flow<List<WorkoutWithSets>> = dao.observeWorkouts()
    val goals: Flow<List<GoalEntity>> = dao.observeGoals()

    suspend fun saveWorkout(draft: WorkoutDraft, editingId: Long? = null): Long =
        db.withTransaction {
            validate(draft)

            val workoutId = if (editingId == null) {
                dao.insertWorkout(
                    WorkoutEntity(
                        date = draft.date,
                        title = draft.title.trim(),
                        bodyPart = draft.bodyPart,
                        memo = draft.memo.trim()
                    )
                )
            } else {
                val old = dao.getWorkout(editingId)
                    ?: error("수정할 운동 기록을 찾지 못했습니다.")

                dao.updateWorkout(
                    old.workout.copy(
                        date = draft.date,
                        title = draft.title.trim(),
                        bodyPart = draft.bodyPart,
                        memo = draft.memo.trim()
                    )
                )
                dao.deleteSets(editingId)
                editingId
            }

            val rows = draft.exercises.flatMapIndexed { exerciseIndex, exercise ->
                exercise.sets.mapIndexed { setIndex, set ->
                    WorkoutSetEntity(
                        workoutId = workoutId,
                        exerciseOrder = exerciseIndex + 1,
                        exerciseName = exercise.name.trim(),
                        setNo = setIndex + 1,
                        weight = set.weight,
                        reps = set.reps
                    )
                }
            }

            dao.insertSets(rows)
            workoutId
        }

    suspend fun deleteWorkout(workout: WorkoutWithSets) {
        dao.deleteWorkout(workout.workout)
    }

    suspend fun saveGoal(goal: GoalEntity) {
        dao.upsertGoal(goal.copy(updatedAt = System.currentTimeMillis()))
    }

    private fun validate(draft: WorkoutDraft) {
        require(draft.date.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            "운동 날짜를 확인해주세요."
        }
        require(draft.title.isNotBlank()) { "운동 제목을 입력해주세요." }
        require(draft.bodyPart in setOf("등", "가슴", "하체", "어깨")) {
            "운동 부위를 선택해주세요."
        }
        require(draft.exercises.isNotEmpty()) { "운동 종목을 하나 이상 추가해주세요." }

        draft.exercises.forEach { exercise ->
            require(exercise.name.isNotBlank()) { "운동 종목명을 입력해주세요." }
            require(exercise.sets.isNotEmpty()) { "${exercise.name}의 세트를 입력해주세요." }
            exercise.sets.forEach { set ->
                require(set.weight >= 0) { "무게는 0 이상이어야 합니다." }
                require(set.reps > 0) { "횟수는 1 이상이어야 합니다." }
            }
        }
    }
}
