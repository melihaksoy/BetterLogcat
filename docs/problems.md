# Problems

The Problems panel (toolbar button with the IDE's Problems icon) lists what went wrong, grouped, with a
count per problem. Unlike the log, it keeps its contents: Clear, a full buffer, or an IDE restart do not
remove a problem. You delete problems when you are done with them.

![Problems panel](images/problems-panel.png)

## Kinds

| Kind | Found by | Gutter icon |
|---|---|---|
| Crash | `FATAL EXCEPTION` from AndroidRuntime, an assert, or libc's `Fatal signal` (native) | red circle |
| ANR | `ANR in <app>` from ActivityManager | frozen thread |
| Exception | any line that names a throwable, like `java.io.IOException: closed` | lightning |
| StrictMode | `StrictMode policy violation` lines | stopwatch |
| Warning | level W | yellow triangle |

The same problem is one row: the same exception from the same tag, the same violation, or the same warning
text with other numbers in it ("Skipped 41 frames" and "Skipped 42 frames" are one row).

Lines are recorded when the filter shows them. A crash or ANR of your project app is recorded even when the
filter hides its line: ANR lines come from system_server, not from your app.

## Sessions

- A pane starts a session when it starts to stream a device. The title is the device name.
- When your project app starts ("Start proc" from ActivityManager), a new session begins:
  "com.example.app launched · Pixel 9".
- The live session is at the top ("Now"), sorted by kind. Earlier sessions are below, newest first; click one
  to open it.
- Sessions without problems are not saved.
- When a device log is read again (for example after a restart, with "Show the existing device log" on),
  lines that are already in a saved session of that device are not recorded twice.

## Actions

- **Click a problem:** go to its newest line. Click again for older lines. When the line left the log, its
  saved copy opens in an editor tab.
- **Right-click a problem:** Go to Line, Open in Editor, Copy Message, Copy as Issue (Markdown), Show Only or
  Hide its tag, Ignore This Problem, Delete.
- **Right-click a session:** Rename, Copy Summary, Delete Session, Delete Sessions Before This One. Hover an
  earlier session for its delete button.
- **⋮ menu:** Delete Sessions Older Than 1, 7 or 30 Days, Delete All Earlier Sessions, Show Ignored Problems
  Again.
- **Settings › Tools › Better Logcat › Problems:** how many days sessions are kept (default 30), and the crash
  notification.

## Frame drops

When Choreographer reports "Skipped N frames", the panel shows how many drops there were, the frames skipped,
the worst drop, and bars per 10 seconds over the 5 minutes up to the newest drop.

## Copy as Issue

Right-click a crash, ANR, exception or StrictMode row (in the log, in its gutter menu, or in the Problems
panel) and choose **Copy as Issue (Markdown)**. You get the problem name and text, the app's version (read
with `adb shell dumpsys package`), the device and Android version, the time, the tag and the full trace in a
code block, ready to paste into an issue.

## Native crashes

Click the gutter of a `Fatal signal` line and choose **Symbolize Native Crash**. The plugin takes the
backtrace that crash_dump logs right after (tag `DEBUG`, within 15 seconds), finds the NDK's
`llvm-symbolizer` in your SDK, and finds unstripped copies of the crashing libraries in your project's build
folders (CMake `obj`, `merged_native_libs`). The result, function names with file and line, opens in an
editor tab. Build the app (debug) first, and install the NDK from the SDK Manager.

## Crash notifications

When your app crashes or stops responding while the Better Logcat tool window is hidden, an IDE notification
appears with **Show in Better Logcat**. At most one in 5 seconds; only for lines from the last minute.

## Try it

Send fake lines with adb. They come from the shell process, so pick **All packages** in the package picker
first (`package:mine` hides them):

```bash
adb shell log -p w -t BLTest "Low memory 42"
adb shell log -p e -t BLTest "java.io.IOException: closed"
adb shell log -p e -t AndroidRuntime "FATAL EXCEPTION: main"
adb shell log -p e -t ActivityManager "ANR in com.example.app (com.example.app/.MainActivity)"
adb shell log -p d -t StrictMode "StrictMode policy violation; ~duration=48 ms: android.os.strictmode.DiskReadViolation"
adb shell log -p f -t libc "Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0"
adb shell log -p i -t Choreographer "Skipped 42 frames!  The application may be doing too much work on its main thread."
```

Then click Clear: the problems stay. Restart the IDE: the session moves to "Earlier sessions".

For real problems in your app:
- **Crash:** throw from a click handler.
- **ANR:** `Thread.sleep(10_000)` on the main thread, then tap the screen again.
- **StrictMode:** in a debug `Application.onCreate`, call
  `StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectDiskReads().penaltyLog().build())`,
  then read a file on the main thread.
- **Native crash:** dereference a null pointer in your own JNI code, so the backtrace has your library in it.
