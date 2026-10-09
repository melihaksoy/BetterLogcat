# Better Logcat

A modern Logcat tool window for Android Studio, built with Compose (Jewel). It sits next to the built-in
Logcat and keeps what works there (filter syntax, device picker) while fixing what gets in the way:
multi-line messages, long JSON, copying, and a busy toolbar.

![Better Logcat](docs/images/tabs-tags.png)

## Features

**Reading logs**
- Multi-line messages (stack traces) and split messages (Ktor, OkHttp bodies over ~4000 chars) stay one entry.
- JSON is detected anywhere in a message and colored. The body is pretty printed when expanded; a collapsed
  row shows a one-line preview with **Copy JSON** and **Open** (read-only editor tab).
- Big expanded messages are virtualized and stay smooth; a sticky header collapses them from anywhere.
- Clickable stack frames, clickable links, keyword highlights with your own colors, per-level colors.
- App start and end markers from ActivityManager, so each run of your app is easy to tell apart.

**Filtering and finding**
- Studio-style search terms in the search bar, colored and editable, with autocomplete (Ctrl+Space):
  `package:mine`, `tag:`, `-tag:`, `tag=:a,b` (exact, comma list), `tag~:regex`, `level:W`, `message:`, `line:`, `process:`, `pid:`, `age:5m`,
  `is:crash`, `is:stacktrace`, `is:json`, `name:`.
- Favorites and history behind the funnel button, with match counts. The last filter is restored.
- Level toggles, a Tags panel (your app vs. other, counts, per-second sparklines, show only / hide). The panel
  writes `tag=:` / `-tag=:` terms into the search text, so tag choices are editable and saved with the filter.
- Find bar (Cmd/Ctrl+F) that moves between matches without changing the filters.

**Working with entries**
- Block selection (click, Shift, Cmd/Ctrl, drag), copy messages or whole lines, double-click any cell for
  free text selection.
- Gutter: click to bookmark; crash and network rows have icons with quick actions.
- Bookmarks panel: every bookmarked line of the project, filterable; click to jump, or open the saved copy in an
  editor. Bookmarks are saved in the IDE cache, so Clear and a restart keep them (Settings → Better Logcat).
- Click a selected row again (or press Esc) to deselect it.
- Network panel: Ktor and OkHttp requests paired with responses (status, method, path, time), Copy as cURL.
- Compare two entries in the diff viewer. R8/ProGuard retrace with a `mapping.txt`.

**Layout and devices**
- Tabs and split panes (right / down), each with its own device and filter, remembered per project.
- Screenshot and screen recording (MP4 on devices, WebM on emulators).
- View settings window with a live preview; package aliases for long package names.

![Network panel and gutter](docs/images/gutter-network.png)
![Bookmarks panel](docs/images/bookmarks.png)
![Ktor response](docs/images/ktor.png)
![View settings](docs/images/view-settings.png)

## Install

Requires Android Studio 2026.2.1 (build 262.9437) or later.

From disk: Settings | Plugins | ⚙ | Install Plugin from Disk, then pick `build/distributions/better-logcat-*.zip`.
Bind a shortcut in Settings | Keymap | "Better Logcat".

## Build

JDK 25 is needed (Android Studio 2026.2 ships Java 25 bytecode).

```bash
./gradlew test buildPlugin -PlocalPlatformPath="/path/to/oldest/Android Studio.app"
./gradlew verifyPlugin -PlocalPlatformPath="<oldest>" -PverifyPlatformPaths="<oldest>,<newest>"
```

Always compile against the oldest supported Studio: newer builds add Jewel overloads that older ones lack.

## Publish

Signing and publishing read secrets from environment variables, never from the repository:
`CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` and `PUBLISH_TOKEN`.

```bash
./gradlew signPlugin publishPlugin -PlocalPlatformPath="<oldest>"
```

See [CHANGELOG.md](CHANGELOG.md) for what changed in each version.
