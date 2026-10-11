# Contributing

Thanks for helping. Bug reports, ideas and pull requests are welcome at
[github.com/melihaksoy/BetterLogcat](https://github.com/melihaksoy/BetterLogcat).

## Reporting a bug

Include the Android Studio version (Help › About), the plugin version, the device or emulator and its API
level, and the steps. A few log lines that show the problem help a lot (remove anything private first).
If a pane stopped with an error, its panel has **Copy error details**: paste that too.

## Building

- JDK 25 (Android Studio 2026.2 ships Java 25 bytecode).
- A local Android Studio install to compile against: always the **oldest** supported one
  (`pluginSinceBuild` in `gradle.properties`). Jewel is not binary stable inside one platform: newer builds
  add overloads, the compiler picks them, and older Studios then crash with `NoSuchMethodError`.

```bash
./gradlew clean test buildPlugin -PlocalPlatformPath="/path/to/oldest/Android Studio.app"
./gradlew verifyPlugin -PlocalPlatformPath="<oldest>" -PverifyPlatformPaths="<oldest>,<newest>"
./gradlew runIde -PlocalPlatformPath="<oldest>"     # a sandbox IDE with the plugin
```

Use `clean` after model changes: stale test classes show up as `NoSuchMethodError` in tests. The Gradle build
cache is off on purpose, for the same reason.

Without `localPlatformPath`, Gradle downloads the Android Studio version named in `gradle.properties`.

## Checking UI without an IDE

`RenderProbeTest` renders screens to PNG in a headless IDE test when `RENDER_DIR` is set:

```bash
RENDER_DIR=/some/dir ./gradlew test --tests '*RenderProbeTest*' -PlocalPlatformPath="<oldest>"
```

The screenshots in `docs/images` come from it (`RENDER_DIR=$PWD/docs/images`).

## Code layout

| Folder | What |
|---|---|
| `adb/` | adb process wrapper and parsers. Logs stream as `adb exec-out logcat -B` (binary), so multi-line messages keep exact boundaries. |
| `domain/usecase/`, `interactor/` | use case interfaces and their implementations; all return `EitherFlow`. |
| `filter/` | `LogFilter`, the compiled `LogFilterMatcher`, the ring `LogBuffer` (joins chunked messages, context lines), search terms (`QueryParser`, `QuerySuggestions`). |
| `format/` | JSON, stack frames, copy formats, log file reading, issue Markdown, native backtraces. |
| `model/` | log entries, problems and sessions, tag groups, bookmarks, highlights. |
| `ide/` | project services and stores (settings, bookmarks, problem history, tag notes), the native symbolizer. |
| `ui/logcat/` | MVI: `LogcatState`, `LogcatIntent`, `LogcatEvent`, `LogcatViewModel`, `LogcatScreen`. |
| `ui/logcat/popup/` | native IDE popups and dialogs behind `LogcatPopups`, so composables stay Swing-free and previewable. |
| `toolwindow/` | tabs, split panes (`OnePixelSplitter`), one `LogcatPane` (ViewModel + Compose panel) per pane. |

`LogcatViewModel` runs all its work on one single-thread dispatcher; keep buffer access there.

## Code rules

- Kotlin official style. Every type has a top-level `import`, no fully-qualified names inline.
- Named arguments on every call, one parameter per line in declarations and calls.
- In composables, `modifier: Modifier = Modifier` is the first parameter and the first argument.
- Previews live at the bottom of the component's file, in a `// region Previews` block.
- User-facing text goes in `src/main/resources/messages/BetterLogcatBundle.properties`.
- Icons come from `AllIconsKeys` (IDE icons), so the tool window matches the IDE theme.
- Add a test for logic you add (parsers, models, the ViewModel); UI gets a render probe when it helps.
- Add a line to `CHANGELOG.md` for user-visible changes.

## License

By contributing, you agree that your contribution is licensed under the [Apache License 2.0](LICENSE).
