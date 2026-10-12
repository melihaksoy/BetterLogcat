# Changelog

## 0.12.2
- Signed release of 0.12.1 (0.12.1 went to the Marketplace unsigned).

## 0.12.1
- With soft-wrap off, long lines scroll sideways (Shift+wheel, trackpad or the scrollbar); the column
  header scrolls with them.
- No more deprecated tool window API warnings in the Plugin Verifier.

## 0.12.0
- Problems panel: crashes, ANRs, exceptions, StrictMode violations and warnings, grouped with counts, per
  session (device stream, app launch). Problems stay after Clear and restarts until deleted: per problem,
  per session, by age, or by retention (default 30 days). Ignore a noisy problem. Frame drop chart.
- Gutter icons for crashes, ANRs, exceptions, StrictMode violations and warnings, with quick actions.
- Copy as Issue (Markdown): exception, app version from the device, device, time and trace.
- Symbolize Native Crash with the NDK's llvm-symbolizer and the project's unstripped libraries.
- IDE notification for a crash or ANR while the tool window is hidden (setting, on by default).
- `context:N` shows N dimmed lines around each match. New `is:` values: `anr`, `exception`, `strictmode`,
  `problem`.
- Export menu: Export to File, Open Log File (view a saved log in the pane), Compare With Log File (diff).
- Pair Device over Wi-Fi from the device picker.
- Setting: clear the log when the project app starts.
- Tags panel is a tree: drag tags onto each other to group them, the checkbox shows or hides, rows select
  (Cmd/Ctrl and Shift for more) for dragging several tags at once. Empty groups go away.
- Tag descriptions on hover, built-in for common tags, and your own notes.
- Highlight presets for OkHttp, Ktor and Retrofit with shared role colors.
- Toolbar buttons in groups; outline Bookmarks icon.
- Bookmarks are saved in `.idea/workspace.xml` instead of the IDE cache, so Invalidate Caches keeps them.
  Saved messages are cut at 64 KB.
- View settings show the buffer, adb and problem settings with a button to open them.
- Text selection in a message shows a caret.
- Fixed: hide "jump to latest" when nothing is below; OkHttp end lines skipped, failed calls closed.

## 0.11.0
- Bookmarks panel next to Tags and Network: filter, jump, open the saved copy, remove.
- Bookmarks are saved per project in the IDE cache and survive Clear and a restart. Setting: Keep bookmarks (on by default).
- Fixed: the empty gutter could not be clicked to bookmark a row.
- Click the selected row again to deselect it.
- JSON cut off by the logger, or broken between chunks, is now indented instead of shown raw.
- Huge single-line messages are cut into list pieces, so scrolling stays smooth.
- Chunked messages join even when other threads log a lot in between.

## 0.10.0
- Tags panel choices live in the search text as `tag=:a,b` / `-tag=:a,b` terms: editable, saved in favorites and history, restored next session.
- `=:` terms take comma lists (`tag=:OkHttp,Garage`, `package=:a,b`).
- Info and actions (size, Copy JSON, Open, Expand/Collapse) sit on their own strip under the message.
- Resizable side panels; the Network panel shows the selected call's request and response.
- Copy JSON and Open look like buttons, not links.

## 0.9.0
- Gutter with bookmarks, crash and network icons and quick actions.
- Network panel: Ktor and OkHttp calls paired, status, time, Copy as cURL.
- Crash badge on the status line and tab title; jump to the latest crash.
- App start and end markers.
- Find bar (Cmd/Ctrl+F) that does not change filters.
- Sticky header and gutter buttons to collapse big messages from anywhere.
- Copy JSON and Open in Editor on JSON rows; Compare two entries in the diff viewer.
- Tag sparklines (entries per second) in the Tags panel.
- Tabs and split panes are remembered per project; all tabs can be closed (empty state).
- `age:` filters refresh every few seconds.
- A crashed pane rebuilds itself and resets the side panel; fixed a crash in the Tags panel.

## 0.8.0
- Search terms live in the search bar, colored and editable, like Android Studio.
- Big expanded messages are virtualized for smooth scrolling.
- Right-click menu opens on release; Shift+F10 opens it; color pickers; package aliases from settings.

## 0.7.0
- Per-cell text selection, block selection outline, keyword highlights and level colors, clickable links.
- Long messages collapse; Ktor responses: request address in the collapsed row, every JSON part colored.

## 0.6.0
- Chunked messages (Ktor) joined; JSON between other text; Tags panel split into your app and other.

## 0.5.0
- Autocomplete, favorites and history, live term filtering, copy settings, package aliases.

## 0.4.0
- Studio-style search terms with chips.

## 0.3.0
- New-tab button next to the tabs; view settings window with a live preview.

## 0.2.0
- Tabs and split panes, time formats, project app default, tag panel, screenshots, screen recording, retrace.

## 0.1.0
- First version: binary logcat parsing, multi-line entries, filters, JSON, selection, copy, export.
