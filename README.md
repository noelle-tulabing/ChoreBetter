# ChoreBetter

Native Android frontend for a shared household chore list. The task screen follows the supplied Figma wireframe: profile, point balance and store at the top; a week picker; simple chore rows; and an add button.

## Run

Open this directory in Android Studio, select an emulator, and run `app`. Or, with an Android emulator running:

```sh
./gradlew :app:installDebug
~/Library/Android/sdk/platform-tools/adb shell am start -n com.example.ui_practice/.MainActivity
```

The Gradle wrapper uses Java 25 and downloads its toolchain. Set `sdk.dir` in the ignored `local.properties` to your Android SDK installation. The project compiles against SDK 37.

## Try the frontend

- Starts as **Alex**, a manager. Use the profile icon to switch between four local demo household members.
- Select a date and check off your own chores. Everyone can see completion; only the assignee can complete a chore, on its scheduled day.
- Tap **+** to add a chore with an assignee, point value, and daily/weekly recurrence. The selected calendar date is the first occurrence.
- Open the list selector to create an empty list or share it with an existing demo member. A new list is private to its creator.
- **Mine** filters assigned chores. Managers can use **All chores** to see unscheduled and unassigned entries, which are excluded from the calendar.
- Open the store to redeem rewards. Managers can add rewards. Redemption asks for confirmation, deducts points, and adds a history entry.
- Lists, sharing, completions, points, streaks and redemptions persist across app restarts on this device.

## Scope and decisions

This is a frontend demo, not an authenticated multi-device service. Switching profiles simulates roles; it is not a security boundary. Shared-list access rules are enforced in the local state model and tested, but production authorization must also be enforced by the server. No Supabase project is modified or connected.

Daily chores recur every day and weekly chores every seven days from the selected start date. Completion is recorded per occurrence and cannot award points twice. One completion in a day increments the streak; continuing on the next calendar day adds **5 bonus points once that day**. Missing a day resets the streak. This policy fills a gap in the requirements, which specify a bonus without defining its amount or calculation.

Screen-time rewards can be defined but **cannot be redeemed** until a real screen-time provider is connected. They never deduct points or claim to unlock a device. FR-10 and NFR-04 remain integration requirements; they are not implemented by this frontend. Account registration, network sync, push notifications, automatic fair assignment, and edit/delete flows are outside this increment. Chores are assigned manually when created.

The supplied wireframe is the visual reference; the Figma connector could not access the original file.

## Requirements and tests

Sources:
- [Functional and nonfunctional requirements](https://docs.google.com/spreadsheets/d/1Fs8F006KUaVov8ocrdjJ6chHNmMvbqHntxyWDm5Xfg4/edit)
- [User stories and acceptance criteria](https://docs.google.com/document/d/1L-lyEWVdgO51by96-FDIdeX30NVMq_yC11q90cnXPRI/edit)
- [Sprint scope](https://docs.google.com/document/d/190LrTV83XGF5ja_ecv0ws-_LX37W6Wjct4KzVfsb8Jo/edit)

| Requirement | Automated coverage |
| --- | --- |
| FR-01, FR-02 | Empty lists; add chores with frequency, points and assignee; invalid input |
| FR-03, FR-06 | Share a list with an existing member; reject unknown members; switch profiles in UI |
| NFR-01 | Reject reads and redemptions for unshared lists; private lists hidden in UI (local model only) |
| FR-04, FR-07, NFR-02 | Completion and wallet update in the same synchronous state change; no duplicate awards; visible across profiles |
| FR-05 | Continue/reset streaks; bonus once per day |
| US-04, NFR-03 | Daily/weekly occurrences, start dates, excluded unscheduled/unassigned chores |
| FR-08 | Manager-created rewards; member restrictions |
| FR-09, NFR-05 | Exact point deduction, redemption history, insufficient-point rejection, UI confirmation |
| FR-10, NFR-04 | Integration deferred; test ensures unavailable screen time cannot charge points |
| Persistence | Gson round trip for sharing, wallet, completion and redemption |

NFR-02 gives no numeric latency target. Tests verify points update with completion rather than making an unsupported latency claim. NFR-04's multi-day external-service reliability cannot be validated without the service.

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest  # requires a running emulator
./gradlew :app:lintDebug
```

Domain acceptance tests: `ChoreRequirementsTest.kt`. Emulator interaction tests: `ChoreFrontendTest.kt`.
