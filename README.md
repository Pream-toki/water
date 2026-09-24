# water

A water drinking tracker for Android. You log a glass of water with one tap — from
the app, from a home screen widget, or from a notification. No account, no internet
permission, no data leaves the phone.

## What it does

- Open the app and tap **Log Water**. The big number counts your drinks today.
- Log from the home screen: two widgets (small 1×1 and wide 4×2) with a **+1**
  button. The 4×2 widget also shows the time of your last drink and your last
  7 days.
- Log from a notification: the app sends silent reminders during your active
  hours. Each reminder has an **"I Drank 💧"** button that logs a drink right
  there and closes the notification. The app does not open. There is no sound
  and no vibration.
- Reminder times are not fixed. The app looks at the hours you usually drink and
  places reminders around those hours. The plan is set once per day and does not
  reshuffle after a restart.
- The history list shows your recent drinks. Swipe a row to delete it, then undo
  if you swiped by mistake.

## The week row

Seven dots show your last 7 days:

| Dot | Meaning |
| --- | --- |
| Empty ring | that day had no drinks |
| Small dot | 1–3 drinks |
| Medium dot | 4–7 drinks |
| Full dot | 8–11 drinks (about 2 litres) |
| Full dot with glow | 12 or more (about 3 litres) |
| Ring (today) | today, nothing logged yet |

Days in a row with 8 or more drinks get a short line under the dot, and the app
shows your current streak ("N-day healthy streak"). Today counts toward the
streak once you log something, and it never breaks the streak just because the
day is not over.

A one-line legend under the row explains the dots.

## Requirements

- Android 8.0 or newer
- Android Studio (Otter 2025.3.2 or newer) to build the project

## Build and run

1. Open this folder in Android Studio.
2. Wait for Gradle sync to finish. Studio downloads everything it needs.
3. Connect a phone and press Run.

From the command line:

```
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Copy it to a
phone and install it, or run `./gradlew installDebug` with the phone connected.

Unit tests (no phone needed):

```
./gradlew test
```

## If your phone is Xiaomi (MIUI / HyperOS)

1. Developer options → **Install via USB** → on. Some versions also need a Mi
   account sign-in and a SIM card in the phone.
2. Apps → Manage apps → water → **Autostart** → on. Without this, reminders
   stop working after a restart.
3. Manage apps → water → Battery saver → **No restrictions**. Otherwise the
   system delays the alarms.

## Project layout

```
app/src/main/java/com/water/app/
├── data/      Room database and saved settings
├── domain/    logging a drink, planning reminder times, day calculations
├── notify/    notifications, exact alarms, the "I Drank" action, reboot handling
├── widget/    the two home screen widgets
└── ui/        the app screen, week row, history, settings, theme
```

Built with Kotlin, Jetpack Compose (Material 3), Room, DataStore, WorkManager,
and Glance. The app does not ask for the internet permission — everything stays
on the phone.

## Testing notes

In debug builds you can fire a reminder from a computer:

```
adb shell am broadcast -n com.water.app/.notify.ReminderReceiver
```

Release builds do not allow this. Also useful to know: Glance widget rows hold
at most 10 children, so keep widget rows short (the week row here wraps each
dot and its spacing into a single child).

## Screenshots

TODO: add screenshots here (drag an image file onto this page on github.com to upload it)

## License

MIT — see [LICENSE](LICENSE).
