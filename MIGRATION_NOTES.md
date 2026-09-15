# Google Sheets 버전 → Android 로컬 DB 버전

기존 시트 구조와 Android DB의 대응 관계:

## Workout_Log → workouts

- workout_id → id
- date → date
- title → title
- body_part → bodyPart
- memo → memo
- created_at → createdAt

## Workout_Set → workout_sets

- workout_id → workoutId
- exercise_order → exerciseOrder
- exercise_name → exerciseName
- set_no → setNo
- weight → weight
- reps → reps

## Goal → goals

- year_month → yearMonth
- total_goal → totalGoal
- back_goal → backGoal
- chest_goal → chestGoal
- legs_goal → legsGoal
- shoulders_goal → shouldersGoal

향후 Google Sheets 데이터를 CSV로 내보낸 뒤
Android 앱으로 가져오는 Import 기능을 만들 수 있습니다.
