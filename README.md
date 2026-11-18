# ChoreBetter

A shared household chore app for CPSC 310. The iOS frontend is written in **SwiftUI** and ports the Android prototype's screens. Tyler's Supabase backend is **not connected**; the app uses an in-memory demo store so the team can try the UI independently.

## Run

1. Open `ios/ChoreBetter.xcodeproj` in Xcode 16 or newer.
2. Select the **ChoreBetter** scheme and an iPhone or iPad simulator (iOS 17 or newer).
3. Press **Run**.

No backend credentials or third-party packages are needed. For a physical device, select your development team under **Signing & Capabilities** and use a unique bundle identifier.

## Try the UI

- Starts as **Alex**, the demo list manager. The profile button switches between Alex, Noelle, Sam, and Riley and shows the current streak.
- The week picker changes the selected day. **Mine** and **Everyone** filter assignments; managers can use **All chores** to include unassigned and unscheduled chores.
- Tap a chore's circle to complete or uncheck your own occurrence on today's date. The point balance updates immediately.
- Tap **+** to add a chore with a name, points, assignee, and daily/weekly/no recurrence. The selected date becomes its start date. **Edit** changes an existing chore while preserving recorded completion points.
- The list selector lets managers create private lists and share them with existing demo profiles. Members see only lists shared with them.
- The store button opens rewards. Managers can add rewards, and members can redeem affordable rewards after confirming the cost. Redemptions appear in the history.
- Screen-time rewards are displayed as unavailable and never deduct points.

**Demo changes reset when the app relaunches.** Profile switching is a UI demonstration, not authentication. Local access and balance checks support the demo; they are not production authorization.

The demo retains Android's provisional streak policy: completing chores on consecutive calendar days adds five bonus points once per day. Undoing a completion preserves the bonus if another completion keeps that day active; undo is rejected if the points were already spent. The team should confirm this policy against the backend before integration.

## Team responsibilities and handoff

| Owner | Responsibility | Starting point |
| --- | --- | --- |
| Xander | Own and refine the SwiftUI screens, navigation, forms, accessibility, and visual states | `ios/ChoreBetter/Views/` |
| Tyler | Own the existing Supabase backend, schema, authentication/access policies, and server-side point/reward operations | Existing Supabase project |
| Noelle | Connect the UI to Supabase: client setup, authentication, record mapping, asynchronous reads/writes, and loading/error states | `ios/ChoreBetter/Services/` |

Project structure:

- `App/ChoreBetterApp.swift`: app entry point.
- `Views/`: chore screen, week picker, chore rows, entry forms, and rewards screen.
- `Models/ChoreModels.swift`: UI-facing values, based on the Android demo rather than an assumed Supabase schema.
- `Demo/DemoStore.swift`: observable in-memory data and actions used by the views. This is the integration replacement point; it contains no networking or disk persistence.
- `Services/`: reserved for Noelle's integration. No client, credentials, migrations, or Supabase package has been added.
- `ios/Tests/`: executable checks for demo behavior, outside the app target.
- `ios/ChoreBetterUITests/`: simulator interaction tests, included in the shared Xcode scheme.

Xcode automatically includes Swift files added under `ios/ChoreBetter/`. Before connecting a screen, Noelle and Tyler should confirm table names, ID types, relationships, authentication, and row-level security. Noelle can map backend records into the UI models and replace the demo actions with async operations, while coordinating loading, empty, and error states with Xander. Point awards and redemptions must be enforced atomically by the backend. Never put a Supabase service-role or secret key in the app.

A useful first integration slice is reading one authorized chore list. Then connect completion/points, sharing, and rewards. The demo should remain useful for previews while those features are connected.

## Verification

Build the simulator app from the repository root:

```sh
xcodebuild -project ios/ChoreBetter.xcodeproj \
  -scheme ChoreBetter -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /tmp/ChoreBetterDerivedData \
  CODE_SIGNING_ALLOWED=NO build
```

Run the demo behavior checks on a Mac:

```sh
xcrun swiftc -parse-as-library \
  ios/ChoreBetter/Models/ChoreModels.swift \
  ios/ChoreBetter/Demo/DemoStore.swift \
  ios/Tests/DemoStoreChecks.swift -o /tmp/ChoreBetterDemoChecks
/tmp/ChoreBetterDemoChecks
```

These check recurrence across daylight saving, profile/list visibility, manager restrictions, form validation, completion ownership, edited completion awards, streak bonuses, redemption, and preventing negative balances when undoing.

Run the UI tests with **Product → Test** in Xcode after choosing a simulator, or replace `SIMULATOR_UDID` below with an installed simulator's ID from `xcrun simctl list devices available`:

```sh
xcodebuild -project ios/ChoreBetter.xcodeproj \
  -scheme ChoreBetter -destination 'platform=iOS Simulator,id=SIMULATOR_UDID' \
  -derivedDataPath /tmp/ChoreBetterDerivedData \
  CODE_SIGNING_ALLOWED=NO test
```

The UI tests exercise adding/editing chores, validation, list creation/sharing, member profile restrictions, completion, reward confirmation, and blocking an undo after spending points. They do not test a backend connection.

## Existing Android work and requirements

The Android prototype remains in `app/` with its Gradle files and tests. Its behavior and run instructions are documented in [Android prototype](docs/android-prototype.md).

Original school project references:

- [Functional and nonfunctional requirements](https://docs.google.com/spreadsheets/d/1Fs8F006KUaVov8ocrdjJ6chHNmMvbqHntxyWDm5Xfg4/edit)
- [User stories and acceptance criteria](https://docs.google.com/document/d/1L-lyEWVdgO51by96-FDIdeX30NVMq_yC11q90cnXPRI/edit)
- [Sprint scope](https://docs.google.com/document/d/190LrTV83XGF5ja_ecv0ws-_LX37W6Wjct4KzVfsb8Jo/edit)
