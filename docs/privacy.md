# Data and privacy

Better Logcat reads your device logs through your own `adb` and keeps everything on your machine. It sends
nothing over the network and has no analytics.

## What is stored, and where

| What | Where | Scope |
|---|---|---|
| Settings, filters (favorites, history, last filter), view options, colors, keyword highlights, package aliases, tag descriptions, tag groups | `betterLogcat.xml` in the IDE's config folder (`options/`) | all projects |
| Bookmarks (the bookmarked lines' text, cut at 64 KB) | `.idea/workspace.xml` of the project | one project |
| Problem sessions (each problem's newest line, cut at 16 KB) and ignored problems | `.idea/workspace.xml` of the project | one project |
| Tabs and split panes | `.idea/workspace.xml` of the project | one project |
| The last 10,000 log lines of each device (messages cut at 64 KB), to show again after a restart | `better-logcat/last-log/` in the IDE's system folder, one gzip file per device | all projects |

`.idea/workspace.xml` is your personal project file; projects usually keep it out of version control. Check
your `.gitignore` if you commit `.idea/`, because log lines can hold personal or secret data.

## Removing stored data

- **Bookmarks:** remove them in the Bookmarks panel, or turn off "Keep bookmarks after Clear and IDE restart".
- **Problems:** delete problems or sessions in the Problems panel, or use ⋮ › Delete All Earlier Sessions.
  Sessions older than the retention setting (default 30 days) are deleted when the project opens.
- **Saved device logs:** Clear deletes the saved log of the device. Turning off "Keep the last log of each
  device after an IDE restart" deletes all of them. The files are never in the project folder.
- **Everything:** uninstall the plugin, delete `betterLogcat.xml` from the IDE config folder,
  `better-logcat/` from the IDE system folder, and the `BetterLogcat*` components from `.idea/workspace.xml`.

## Commands it runs

All through `adb`, only on your action or for the log itself:

| Command | When |
|---|---|
| `devices -l`, `shell getprop …` | listing devices and their model and Android version |
| `exec-out logcat -B`, `shell ps` | streaming the log, naming processes |
| `logcat -c` | Clear, when "Clear also clears the log on the device" is on |
| `exec-out screencap -p` | Screenshot |
| `shell screenrecord`, `emu screenrecord`, `pull`, `shell rm`, `shell settings … show_touches`, `shell wm size` | Screen record |
| `shell dumpsys package <app>` | Copy as Issue (the app version) |
| `pair`, `connect` | Pair Device over Wi-Fi |

Symbolizing a native crash runs the NDK's `llvm-symbolizer` on your own build output.
