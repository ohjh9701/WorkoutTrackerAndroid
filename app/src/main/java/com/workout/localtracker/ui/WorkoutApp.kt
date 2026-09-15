package com.workout.localtracker.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workout.localtracker.data.ExerciseDraft
import com.workout.localtracker.data.GoalEntity
import com.workout.localtracker.data.SetDraft
import com.workout.localtracker.data.WorkoutDraft
import com.workout.localtracker.data.WorkoutWithSets
import com.workout.localtracker.data.exerciseRecords
import com.workout.localtracker.domain.ProgressStatus
import com.workout.localtracker.domain.ProgressionAnalyzer
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private enum class AppScreen(val label: String) {
    DASHBOARD("대시보드"),
    RECORD("운동기록"),
    STATS("성장통계"),
    GOAL("목표설정")
}

data class SetInputUi(
    val weight: String = "",
    val reps: String = ""
)

data class ExerciseInputUi(
    val name: String = "",
    val sets: List<SetInputUi> = List(4) { SetInputUi() }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutApp(
    viewModel: WorkoutViewModel = viewModel()
) {
    val workouts by viewModel.workouts.collectAsState()
    val goals by viewModel.goals.collectAsState()

    var screen by rememberSaveable { mutableStateOf(AppScreen.DASHBOARD) }
    var editingWorkout by remember { mutableStateOf<WorkoutWithSets?>(null) }
    var recordResetToken by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MY WORKOUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (editingWorkout != null && screen == AppScreen.RECORD) {
                                "운동 기록 수정"
                            } else {
                                screen.label
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                AppScreen.entries.forEach { item ->
                    val icon = when (item) {
                        AppScreen.DASHBOARD -> Icons.Outlined.Home
                        AppScreen.RECORD -> Icons.Outlined.AddCircle
                        AppScreen.STATS -> Icons.Outlined.ShowChart
                        AppScreen.GOAL -> Icons.Outlined.Flag
                    }

                    NavigationBarItem(
                        selected = screen == item,
                        onClick = {
                            screen = item
                            if (item != AppScreen.RECORD) {
                                editingWorkout = null
                            }
                        },
                        icon = { Icon(icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            when (screen) {
                AppScreen.DASHBOARD -> DashboardScreen(
                    workouts = workouts,
                    goals = goals,
                    onEditWorkout = {
                        editingWorkout = it
                        screen = AppScreen.RECORD
                    },
                    onDeleteWorkout = { workout ->
                        viewModel.deleteWorkout(workout) { result ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    result.fold(
                                        onSuccess = { "운동 기록을 삭제했습니다." },
                                        onFailure = { it.message ?: "삭제에 실패했습니다." }
                                    )
                                )
                            }
                        }
                    }
                )

                AppScreen.RECORD -> RecordScreen(
                    editingWorkout = editingWorkout,
                    resetToken = recordResetToken,
                    knownExerciseNames = viewModel.knownExerciseNames(),
                    lastRecordProvider = { name, date ->
                        viewModel.latestExerciseRecord(
                            exerciseName = name,
                            currentDate = date,
                            excludeWorkoutId = editingWorkout?.workout?.id
                        )
                    },
                    onCancelEdit = {
                        editingWorkout = null
                        recordResetToken++
                    },
                    onSave = { draft ->
                        viewModel.saveWorkout(
                            draft = draft,
                            editingId = editingWorkout?.workout?.id
                        ) { result ->
                            scope.launch {
                                result.onSuccess {
                                    snackbarHostState.showSnackbar(
                                        if (editingWorkout == null) {
                                            "운동 기록을 저장했습니다."
                                        } else {
                                            "운동 기록을 수정했습니다."
                                        }
                                    )
                                    editingWorkout = null
                                    recordResetToken++
                                    screen = AppScreen.DASHBOARD
                                }.onFailure {
                                    snackbarHostState.showSnackbar(
                                        it.message ?: "저장에 실패했습니다."
                                    )
                                }
                            }
                        }
                    }
                )

                AppScreen.STATS -> StatsScreen(workouts)

                AppScreen.GOAL -> GoalScreen(
                    goals = goals,
                    onSave = { goal ->
                        viewModel.saveGoal(goal) { result ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    result.fold(
                                        onSuccess = { "목표를 저장했습니다." },
                                        onFailure = { it.message ?: "목표 저장에 실패했습니다." }
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    workouts: List<WorkoutWithSets>,
    goals: List<GoalEntity>,
    onEditWorkout: (WorkoutWithSets) -> Unit,
    onDeleteWorkout: (WorkoutWithSets) -> Unit
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedWorkout by remember { mutableStateOf<WorkoutWithSets?>(null) }

    val ym = YearMonth.parse(month)
    val monthWorkouts = workouts.filter { it.workout.date.startsWith("$month-") }
    val goal = goals.firstOrNull { it.yearMonth == month } ?: GoalEntity(yearMonth = month)
    val progression = remember(workouts) { ProgressionAnalyzer.analyze(workouts) }

    val workoutDays = monthWorkouts.map { it.workout.date }.distinct().size
    val totalRate = if (goal.totalGoal > 0) {
        (workoutDays.toFloat() / goal.totalGoal.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val partCounts = mapOf(
        "등" to monthWorkouts.count { it.workout.bodyPart == "등" },
        "가슴" to monthWorkouts.count { it.workout.bodyPart == "가슴" },
        "하체" to monthWorkouts.count { it.workout.bodyPart == "하체" },
        "어깨" to monthWorkouts.count { it.workout.bodyPart == "어깨" }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MonthNavigator(
                month = ym,
                onPrevious = { month = ym.minusMonths(1).toString() },
                onNext = { month = ym.plusMonths(1).toString() }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "운동일수",
                    value = "${workoutDays}일",
                    sub = "목표 ${goal.totalGoal}일"
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "목표 달성도",
                    value = "${(totalRate * 100).roundToInt()}%",
                    progress = totalRate
                )
            }
        }

        item {
            SectionCard(title = "부위별 운동 현황") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PartStat("등", partCounts["등"] ?: 0, goal.backGoal)
                    PartStat("가슴", partCounts["가슴"] ?: 0, goal.chestGoal)
                    PartStat("하체", partCounts["하체"] ?: 0, goal.legsGoal)
                    PartStat("어깨", partCounts["어깨"] ?: 0, goal.shouldersGoal)
                }
            }
        }

        item {
            SectionCard(title = "운동 캘린더") {
                ProgressLegend()
                Spacer(Modifier.height(10.dp))
                WorkoutCalendar(
                    month = ym,
                    workouts = monthWorkouts,
                    progression = progression,
                    onWorkoutClick = { selectedWorkout = it }
                )
            }
        }

        item {
            SectionCard(title = "오늘의 운동 Tip") {
                Text(
                    text = buildLocalCoachTip(
                        workoutDays = workoutDays,
                        goal = goal,
                        partCounts = partCounts,
                        monthWorkouts = monthWorkouts
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    selectedWorkout?.let { workout ->
        WorkoutDetailDialog(
            workout = workout,
            progression = progression[workout.workout.id],
            onDismiss = { selectedWorkout = null },
            onEdit = {
                selectedWorkout = null
                onEditWorkout(workout)
            },
            onDelete = {
                selectedWorkout = null
                onDeleteWorkout(workout)
            }
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String? = null,
    progress: Float? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            if (sub != null) {
                Text(
                    sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PartStat(
    name: String,
    count: Int,
    goal: Int
) {
    Card(
        modifier = Modifier.width(92.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(name, style = MaterialTheme.typography.labelSmall)
            Text(
                "$count/$goal",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (goal > 0) "${((count.toFloat() / goal) * 100).roundToInt()}%" else "-",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun ProgressLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(Color(0xFFD94A4A), "상승")
        LegendItem(Color(0xFF8A9099), "동일·혼합")
        LegendItem(Color(0xFF3977C9), "미달")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkoutCalendar(
    month: YearMonth,
    workouts: List<WorkoutWithSets>,
    progression: Map<Long, com.workout.localtracker.domain.WorkoutProgression>,
    onWorkoutClick: (WorkoutWithSets) -> Unit
) {
    val firstOffset = month.atDay(1).dayOfWeek.value % 7
    val days = month.lengthOfMonth()
    val byDate = workouts.groupBy { it.workout.date }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val cellCount = ((firstOffset + days + 6) / 7) * 7
        repeat(cellCount / 7) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(7) { weekday ->
                    val index = week * 7 + weekday
                    val day = index - firstOffset + 1

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.78f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        if (day in 1..days) {
                            val date = month.atDay(day).toString()
                            val events = byDate[date].orEmpty()

                            Column(
                                modifier = Modifier.padding(5.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    day.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                events.take(2).forEach { event ->
                                    val status = progression[event.workout.id]?.status
                                        ?: ProgressStatus.NEW
                                    CalendarWorkoutChip(
                                        title = event.workout.title,
                                        status = status,
                                        onClick = { onWorkoutClick(event) }
                                    )
                                }

                                if (events.size > 2) {
                                    Text(
                                        "+${events.size - 2}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarWorkoutChip(
    title: String,
    status: ProgressStatus,
    onClick: () -> Unit
) {
    val color = when (status) {
        ProgressStatus.IMPROVE -> Color(0xFFD94A4A)
        ProgressStatus.DECLINE -> Color(0xFF3977C9)
        ProgressStatus.SAME, ProgressStatus.MIXED -> Color(0xFF8A9099)
        ProgressStatus.NEW -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color.copy(alpha = 0.12f),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (status != ProgressStatus.NEW) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
        }
        Text(
            title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun RecordScreen(
    editingWorkout: WorkoutWithSets?,
    resetToken: Int,
    knownExerciseNames: List<String>,
    lastRecordProvider: (String, String) -> PreviousExerciseRecord?,
    onCancelEdit: () -> Unit,
    onSave: (WorkoutDraft) -> Unit
) {
    val context = LocalContext.current

    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var title by remember { mutableStateOf("") }
    var bodyPart by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    val exercises = remember { mutableStateListOf(ExerciseInputUi()) }

    LaunchedEffect(editingWorkout?.workout?.id, resetToken) {
        if (editingWorkout == null) {
            date = LocalDate.now().toString()
            title = ""
            bodyPart = ""
            memo = ""
            exercises.clear()
            exercises.add(ExerciseInputUi())
        } else {
            date = editingWorkout.workout.date
            title = editingWorkout.workout.title
            bodyPart = editingWorkout.workout.bodyPart
            memo = editingWorkout.workout.memo
            exercises.clear()
            editingWorkout.exerciseRecords().forEach { record ->
                exercises.add(
                    ExerciseInputUi(
                        name = record.name,
                        sets = record.sets.map {
                            SetInputUi(
                                weight = numberText(it.weight),
                                reps = it.reps.toString()
                            )
                        }
                    )
                )
            }
        }
    }

    fun showDatePicker() {
        val current = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
        DatePickerDialog(
            context,
            { _, year, month, day ->
                date = LocalDate.of(year, month + 1, day).toString()
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (editingWorkout != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("기존 운동 기록 수정 중", fontWeight = FontWeight.Bold)
                        TextButton(onClick = onCancelEdit) {
                            Text("취소")
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "기본 정보") {
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("날짜") },
                    trailingIcon = {
                        IconButton(onClick = ::showDatePicker) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "날짜 선택")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = ::showDatePicker)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("제목") },
                    placeholder = { Text("예: 등 집중 훈련") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                Text("운동부위", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf("등", "가슴", "하체", "어깨").forEach { part ->
                        FilterChip(
                            selected = bodyPart == part,
                            onClick = { bodyPart = part },
                            label = { Text(part) }
                        )
                    }
                }
            }
        }

        item {
            Text(
                "운동 종목",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(exercises) { index, exercise ->
            ExerciseInputCard(
                index = index,
                exercise = exercise,
                knownExerciseNames = knownExerciseNames,
                previousRecord = lastRecordProvider(exercise.name, date),
                canDelete = exercises.size > 1,
                onChange = { updated -> exercises[index] = updated },
                onDelete = { exercises.removeAt(index) }
            )
        }

        item {
            OutlinedButton(
                onClick = { exercises.add(ExerciseInputUi()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.AddCircle, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("운동 종목 추가")
            }
        }

        item {
            SectionCard(title = "오늘의 운동 메모") {
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    minLines = 4,
                    placeholder = { Text("컨디션, 자세, 다음 운동 때 기억할 점 등") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Button(
                onClick = {
                    val parsedExercises = exercises.map { exercise ->
                        ExerciseDraft(
                            name = exercise.name,
                            sets = exercise.sets.map { set ->
                                SetDraft(
                                    weight = set.weight.toDoubleOrNull() ?: -1.0,
                                    reps = set.reps.toIntOrNull() ?: 0
                                )
                            }
                        )
                    }

                    onSave(
                        WorkoutDraft(
                            date = date,
                            title = title,
                            bodyPart = bodyPart,
                            memo = memo,
                            exercises = parsedExercises
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (editingWorkout == null) "운동 기록 저장" else "수정 내용 저장")
            }
        }
    }
}

@Composable
private fun ExerciseInputCard(
    index: Int,
    exercise: ExerciseInputUi,
    knownExerciseNames: List<String>,
    previousRecord: PreviousExerciseRecord?,
    canDelete: Boolean,
    onChange: (ExerciseInputUi) -> Unit,
    onDelete: () -> Unit
) {
    val suggestions = remember(exercise.name, knownExerciseNames) {
        if (exercise.name.isBlank()) {
            emptyList()
        } else {
            knownExerciseNames
                .filter {
                    it.contains(exercise.name, ignoreCase = true) &&
                        !it.equals(exercise.name, ignoreCase = true)
                }
                .take(4)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "운동 ${index + 1}",
                    fontWeight = FontWeight.Bold
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Close, contentDescription = "종목 삭제")
                    }
                }
            }

            OutlinedTextField(
                value = exercise.name,
                onValueChange = { onChange(exercise.copy(name = it)) },
                label = { Text("운동종목") },
                placeholder = { Text("예: 티바로우") },
                modifier = Modifier.fillMaxWidth()
            )

            if (suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = { onChange(exercise.copy(name = suggestion)) },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "세트 수",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium
                )
                IconButton(
                    onClick = {
                        if (exercise.sets.size > 1) {
                            onChange(exercise.copy(sets = exercise.sets.dropLast(1)))
                        }
                    }
                ) {
                    Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = "세트 감소")
                }
                Text(
                    "${exercise.sets.size}",
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        if (exercise.sets.size < 20) {
                            onChange(exercise.copy(sets = exercise.sets + SetInputUi()))
                        }
                    }
                ) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = "세트 추가")
                }
            }

            previousRecord?.let { previous ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "지난 기록 · ${previous.date}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            previous.exercise.sets.joinToString("  ·  ") {
                                "${numberText(it.weight)}kg×${it.reps}"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                        TextButton(
                            onClick = {
                                onChange(
                                    exercise.copy(
                                        sets = previous.exercise.sets.map {
                                            SetInputUi(
                                                weight = numberText(it.weight),
                                                reps = it.reps.toString()
                                            )
                                        }
                                    )
                                )
                            }
                        ) {
                            Text("지난 기록 적용")
                        }
                    }
                }
            }

            exercise.sets.forEachIndexed { setIndex, set ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${setIndex + 1}",
                        modifier = Modifier.width(20.dp),
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = set.weight,
                        onValueChange = { value ->
                            val updated = exercise.sets.toMutableList()
                            updated[setIndex] = set.copy(
                                weight = value.filter { ch -> ch.isDigit() || ch == '.' }
                            )
                            onChange(exercise.copy(sets = updated))
                        },
                        label = { Text("kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = set.reps,
                        onValueChange = { value ->
                            val updated = exercise.sets.toMutableList()
                            updated[setIndex] = set.copy(
                                reps = value.filter { ch -> ch.isDigit() }
                            )
                            onChange(exercise.copy(sets = updated))
                        },
                        label = { Text("회") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsScreen(
    workouts: List<WorkoutWithSets>
) {
    val exerciseNames = remember(workouts) {
        workouts
            .flatMap { it.exerciseRecords() }
            .map { it.name }
            .distinctBy { it.lowercase() }
            .sorted()
    }

    var selected by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(exerciseNames) {
        if (selected.isBlank() || selected !in exerciseNames) {
            selected = exerciseNames.firstOrNull().orEmpty()
        }
    }

    val records = remember(workouts, selected) {
        workouts
            .mapNotNull { workout ->
                workout.exerciseRecords()
                    .firstOrNull { it.name.equals(selected, ignoreCase = true) }
                    ?.let { workout to it }
            }
            .sortedByDescending { it.first.workout.date }
    }

    val bestWeight = records
        .flatMap { it.second.sets }
        .maxOfOrNull { it.weight } ?: 0.0

    val bestE1rm = records
        .maxOfOrNull { ProgressionAnalyzer.metrics(it.second).estimated1RM } ?: 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(title = "종목 선택") {
                if (exerciseNames.isEmpty()) {
                    Text("운동 기록을 먼저 추가해주세요.")
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        exerciseNames.forEach { name ->
                            FilterChip(
                                selected = selected == name,
                                onClick = { selected = name },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }
        }

        if (selected.isNotBlank()) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "최고중량 PR",
                        value = "${numberText(bestWeight)}kg"
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "최고 추정 1RM",
                        value = "${numberText(bestE1rm)}kg"
                    )
                }
            }

            item {
                Text(
                    "최근 기록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(records.take(12)) { _, item ->
                val (workout, exercise) = item
                val metrics = ProgressionAnalyzer.metrics(exercise)

                Card {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            workout.workout.date,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            workout.workout.title,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "최고 ${numberText(metrics.maxWeight)}kg · 추정 1RM ${numberText(metrics.estimated1RM)}kg",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            exercise.sets.joinToString("  ·  ") {
                                "${numberText(it.weight)}×${it.reps}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalScreen(
    goals: List<GoalEntity>,
    onSave: (GoalEntity) -> Unit
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val existing = goals.firstOrNull { it.yearMonth == month }

    var total by remember { mutableStateOf("20") }
    var back by remember { mutableStateOf("4") }
    var chest by remember { mutableStateOf("4") }
    var legs by remember { mutableStateOf("4") }
    var shoulders by remember { mutableStateOf("4") }

    LaunchedEffect(month, existing) {
        total = (existing?.totalGoal ?: 20).toString()
        back = (existing?.backGoal ?: 4).toString()
        chest = (existing?.chestGoal ?: 4).toString()
        legs = (existing?.legsGoal ?: 4).toString()
        shoulders = (existing?.shouldersGoal ?: 4).toString()
    }

    val ym = YearMonth.parse(month)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MonthNavigator(
                month = ym,
                onPrevious = { month = ym.minusMonths(1).toString() },
                onNext = { month = ym.plusMonths(1).toString() }
            )
        }

        item {
            SectionCard(title = "월 목표") {
                NumberField(
                    label = "월 운동 목표(일)",
                    value = total,
                    onValueChange = { total = it }
                )
            }
        }

        item {
            SectionCard(title = "부위별 최소 목표") {
                NumberField("등", back) { back = it }
                Spacer(Modifier.height(8.dp))
                NumberField("가슴", chest) { chest = it }
                Spacer(Modifier.height(8.dp))
                NumberField("하체", legs) { legs = it }
                Spacer(Modifier.height(8.dp))
                NumberField("어깨", shoulders) { shoulders = it }
            }
        }

        item {
            Button(
                onClick = {
                    onSave(
                        GoalEntity(
                            yearMonth = month,
                            totalGoal = total.toIntOrNull()?.coerceAtLeast(1) ?: 20,
                            backGoal = back.toIntOrNull()?.coerceAtLeast(0) ?: 4,
                            chestGoal = chest.toIntOrNull()?.coerceAtLeast(0) ?: 4,
                            legsGoal = legs.toIntOrNull()?.coerceAtLeast(0) ?: 4,
                            shouldersGoal = shoulders.toIntOrNull()?.coerceAtLeast(0) ?: 4
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("목표 저장")
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WorkoutDetailDialog(
    workout: WorkoutWithSets,
    progression: com.workout.localtracker.domain.WorkoutProgression?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(workout.workout.title, fontWeight = FontWeight.Bold)
                Text(
                    "${workout.workout.date} · ${workout.workout.bodyPart}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                workout.exerciseRecords().forEach { exercise ->
                    val p = progression?.exercises?.firstOrNull {
                        it.exerciseName.equals(exercise.name, ignoreCase = true)
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(exercise.name, fontWeight = FontWeight.Bold)
                            p?.let {
                                ProgressBadge(it.status)
                            }
                        }

                        p?.previous?.let { previous ->
                            Text(
                                "직전 ${p.previousDate}: ${numberText(previous.maxWeight)}kg / e1RM ${numberText(previous.estimated1RM)}kg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        exercise.sets.forEach {
                            Text(
                                "${it.setNo}세트  ${numberText(it.weight)}kg × ${it.reps}회",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    HorizontalDivider()
                }

                if (workout.workout.memo.isNotBlank()) {
                    Text(
                        workout.workout.memo,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("수정")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("삭제")
                }
                TextButton(onClick = onDismiss) {
                    Text("닫기")
                }
            }
        }
    )
}

@Composable
private fun ProgressBadge(status: ProgressStatus) {
    val (label, color) = when (status) {
        ProgressStatus.IMPROVE -> "▲ 상승" to Color(0xFFD94A4A)
        ProgressStatus.DECLINE -> "▼ 미달" to Color(0xFF3977C9)
        ProgressStatus.SAME -> "= 동일" to Color(0xFF8A9099)
        ProgressStatus.MIXED -> "± 혼합" to Color(0xFF8A9099)
        ProgressStatus.NEW -> "첫 기록" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun MonthNavigator(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPrevious) { Text("‹") }
        Text(
            "${month.year}년 ${month.monthValue}월",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        TextButton(onClick = onNext) { Text("›") }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun buildLocalCoachTip(
    workoutDays: Int,
    goal: GoalEntity,
    partCounts: Map<String, Int>,
    monthWorkouts: List<WorkoutWithSets>
): String {
    val rate = if (goal.totalGoal > 0) {
        ((workoutDays.toDouble() / goal.totalGoal) * 100).roundToInt()
    } else 0

    val targets = listOf(
        "등" to goal.backGoal,
        "가슴" to goal.chestGoal,
        "하체" to goal.legsGoal,
        "어깨" to goal.shouldersGoal
    )

    val biggestGap = targets
        .map { (part, target) ->
            Triple(part, target, (target - (partCounts[part] ?: 0)).coerceAtLeast(0))
        }
        .maxByOrNull { it.third }

    val base = "이번 달 ${workoutDays}/${goal.totalGoal}일 운동해서 목표 달성률은 ${rate}%입니다."

    val gapText = if (biggestGap != null && biggestGap.third > 0) {
        " ${biggestGap.first} 운동이 목표까지 ${biggestGap.third}회 남아 있어 다음 일정에 우선 배치해도 좋습니다."
    } else {
        " 부위별 최소 목표를 잘 채우고 있습니다."
    }

    val recent = monthWorkouts.sortedByDescending { it.workout.date }.take(2)
    val recovery = if (
        recent.size == 2 &&
        recent[0].workout.bodyPart == recent[1].workout.bodyPart
    ) {
        " 최근 두 기록이 모두 ${recent[0].workout.bodyPart}이므로 회복 상태를 확인하세요."
    } else ""

    return base + gapText + recovery
}

private fun numberText(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format("%.1f", value)
