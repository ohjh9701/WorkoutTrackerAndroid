# Workout Tracker Android — Local DB Edition

기존 Google Sheets + Apps Script 웹앱을 **완전한 Android 네이티브 앱**으로 옮긴 프로젝트입니다.

## 핵심 구조

- UI: Jetpack Compose
- 로컬 DB: Room (SQLite)
- 서버: 없음
- Google Sheets: 필요 없음
- 인터넷 연결: 기본 기능에는 필요 없음

운동 기록은 앱 내부의 `workout_tracker.db` 파일에 저장됩니다.

## 포함 기능

- 월 운동일수 / 목표 달성률
- 등 / 가슴 / 하체 / 어깨 횟수
- 월간 운동 달력
- 직전 동일 종목 대비 점진적 과부하 표시
  - 빨강: 상승
  - 회색: 동일 또는 혼합
  - 파랑: 직전 기록 미달
- 운동 기록 추가
- 운동 기록 수정 / 삭제
- 세트 수 +/- → 세트별 무게/횟수 입력칸 자동 생성
- 기존 운동 종목명 추천
- 동일 종목의 지난 기록 자동 조회
- `지난 기록 적용`
- 성장통계
  - 종목별 최고중량 PR
  - 추정 1RM
  - 최근 기록
- 월 목표 설정
- 인터넷 없이 동작하는 기본 운동 Tip

## Android Studio에서 실행

권장 환경:

- Android Studio Quail 4 (2026.1.4) 이상
- JDK 17
- Android SDK 37 설치

1. ZIP 압축을 풉니다.
2. Android Studio → **Open**
3. `WorkoutTrackerAndroid` 폴더를 선택합니다.
4. Gradle Sync가 완료될 때까지 기다립니다.
5. 스마트폰에서 개발자 옵션 → USB 디버깅을 켭니다.
6. 스마트폰 연결 후 ▶ Run을 누릅니다.

## APK 만들기

Android Studio 메뉴:

**Build → Generate App Bundles or APKs → Generate APKs**

개인 설치 테스트는 Debug APK로도 충분합니다.

일반적으로 생성 위치:

`app/build/outputs/apk/debug/app-debug.apk`

정식 서명 APK가 필요하면:

**Build → Generate Signed App Bundle or APK**

에서 keystore를 생성한 뒤 Release APK를 빌드합니다.

## 데이터는 어디에 있나?

Room이 Android 앱 전용 내부 저장소에 SQLite DB를 만듭니다.

사용자가 일반 파일 탐색기로 DB 파일을 직접 수정할 필요가 없습니다.

중요:

- 앱 업데이트: 보통 데이터 유지
- 앱 삭제: 기기에서 로컬 데이터가 삭제될 수 있음
- 기기 분실/초기화: 백업이 없으면 데이터 손실 가능

따라서 다음 버전에서는 **JSON 백업/복원** 또는 **Google Drive 백업** 기능 추가를 권장합니다.

## GPT 기능에 대하여

OpenAI API Key를 APK 내부에 직접 넣는 것은 권장하지 않습니다.
APK는 사용자가 분석할 수 있으므로 API Key가 노출될 수 있습니다.

현재 프로젝트는 오프라인 규칙 기반 Tip을 사용합니다.

GPT 코치를 추가하려면 권장 구조는:

Android APK → 본인 백엔드/Cloud Function → OpenAI API

입니다.

## 기존 Google Sheet 데이터

이 프로젝트는 새 로컬 DB에서 시작합니다.
기존 Google Sheet의 운동 기록을 이 앱으로 옮기려면
CSV/JSON 가져오기 기능을 별도로 붙일 수 있습니다.


## 첫 실행 시 Gradle 관련 안내

프로젝트는 **Gradle 9.6.0 / AGP 9.4.0** 기준으로 구성했습니다.

이 전달본에는 실행용 `gradle-wrapper.jar` 바이너리를 포함하지 않았습니다.
Android Studio에서 프로젝트를 열었을 때 Gradle 설정을 묻는 경우:

- Gradle version: **9.6.0**
- Gradle JDK: **17**

을 선택한 뒤 Sync 하면 됩니다.

Android Studio에서 새 프로젝트를 하나 만든 뒤 이 프로젝트의 `app` 폴더와
루트 Gradle 설정을 복사하는 방식으로도 사용할 수 있습니다.


## Android Studio 없이 APK 만들기

이 배포본에는 `.github/workflows/build-apk.yml`이 포함되어 있습니다.
GitHub Actions에서 **Build Workout Tracker APK** 워크플로를 실행하면
GitHub 서버가 Debug APK를 자동 생성합니다.

자세한 순서는 `BROWSER_ONLY_BUILD.md`를 참고하세요.
