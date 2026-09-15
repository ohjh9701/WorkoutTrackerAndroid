# 코딩툴 없이 APK 만들기 — GitHub Actions

이 프로젝트는 Android Studio 없이 GitHub의 서버에서 APK를 자동 빌드하도록 설정되어 있습니다.

## 가장 쉬운 방법

ChatGPT에서 GitHub 플러그인을 연결한 경우,
이 프로젝트를 GitHub 저장소에 올리고 Actions 빌드를 실행하는 작업을 ChatGPT와 이어서 진행할 수 있습니다.

## GitHub 웹사이트에서 직접 하는 방법

1. GitHub에 로그인합니다.
2. 새 Private repository를 만듭니다.
   - 예: `workout-tracker-android`
3. 이 ZIP을 Windows 기본 압축 해제로 풉니다.
4. GitHub 저장소에서 `Add file` → `Upload files`를 선택합니다.
5. `WorkoutTrackerAndroid_GitHubBuild` 폴더 안의 파일/폴더 전체를 업로드합니다.
   - `.github` 폴더도 반드시 포함되어야 합니다.
6. Commit changes를 누릅니다.
7. 저장소 상단의 **Actions** 탭으로 이동합니다.
8. 왼쪽에서 **Build Workout Tracker APK**를 선택합니다.
9. **Run workflow** → **Run workflow**를 누릅니다.
10. 빌드가 성공하면 해당 실행 화면 아래 **Artifacts**에
    `WorkoutTracker-debug-apk`가 나타납니다.
11. 다운로드해서 압축을 풀면 `app-debug.apk`가 있습니다.
12. APK를 Android 휴대폰으로 보내 설치합니다.

## 휴대폰 설치

Android에서 처음 직접 설치하는 APK라면 브라우저/파일 앱에 대해
`알 수 없는 앱 설치 허용`을 한 번 켜야 할 수 있습니다.

## 참고

- 이 APK는 개인 테스트용 Debug APK입니다.
- Google Play 배포용 APK/AAB는 별도 서명(keystore) 설정이 필요합니다.
- 운동 데이터는 휴대폰 내부 Room/SQLite DB에 저장됩니다.
- 앱을 삭제하면 로컬 운동 데이터도 삭제될 수 있으므로 추후 백업/복원 기능 추가를 권장합니다.
