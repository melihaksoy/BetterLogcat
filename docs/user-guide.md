# User guide

Open the **Better Logcat** tool window. It picks the first online device and your project's app
(`package:mine`). Bind a shortcut in Settings | Keymap | "Better Logcat".

![Better Logcat](images/tabs-tags.png)

## Toolbar

From left to right:

| Group | Buttons |
|---|---|
| Source | device picker (also **Pair Device over Wi-Fi…**), package picker |
| Log | Find, Pause/Resume, Clear, Soft-wrap, Clear on app start |
| Panels | Tags, Network, Bookmarks, Problems |
| Messages | Expand all, Collapse all |
| Capture | Screenshot, Screen record, Export menu |
| Settings | View settings |
| Panes | Split right, Split down, Close pane |

## Reading the log

- A multi-line message (a stack trace) is one entry. Messages that loggers cut into ~4000-char pieces (Ktor,
  OkHttp bodies) are joined back.
- Click the chevron, or press Enter/Space, to expand or collapse a message. Right/Left arrows expand or
  collapse the selection.
- JSON is colored. Expanded, it is pretty printed. Collapsed, the row shows **Copy JSON** and **Open**
  (a read-only editor tab with folding and find).
- In expanded JSON, click the line that opens a block (`▾`) to fold it to `▸ { … 12 keys }`. Deep in a big
  body, a path bar at the top shows the blocks around you; click one to fold it.
- An HTTP row shows the method next to the address, a Ktor response shows the call time, and the status
  code is colored by class (2xx, 3xx, 4xx, 5xx).
- After an IDE restart, each device's last log shows again, up to a "previous IDE session" divider. Live
  lines continue below it.
- Stack frames and links are clickable. Load an R8/ProGuard `mapping.txt` (row menu › Retrace, or View
  settings) to retrace obfuscated traces.
- App start and end lines from ActivityManager show as markers between runs.

![JSON and selection](images/selection-json.png)

## Filtering

- Type terms in the search field: see [Search terms](search-terms.md).
- The level buttons (V D I W E A) on the right turn levels on and off.
- The funnel button holds favorites (star a filter) and history, with how many lines each one matches now.
- `context:3` shows 3 dimmed lines before and after each match.
- **Find** (Cmd/Ctrl+F) moves between matches (Enter, Shift+Enter) without hiding other lines.

![Saved filters](images/saved-filters.png)

## Selecting and copying

- Click, Shift+click, Cmd/Ctrl+click, or drag to select rows. Arrows, Page Up/Down, Home/End move; add Shift to
  extend. Esc clears the selection.
- Cmd/Ctrl+C copies messages only or whole lines (View settings › "Copy copies the message only").
- Double-click a cell to select part of its text, with a caret. Esc leaves text selection.
- Right-click (or Shift+F10) for the row menu: copy modes, Copy JSON, Open in Editor, Copy as cURL, Compare the
  Two Entries, Bookmark, Show only / Hide tag, Show only package, package alias, Copy as Issue.

## Gutter

The narrow column at the left of each row:

| Icon | Meaning | Click |
|---|---|---|
| red circle | crash | menu: bookmark, Copy Crash, Copy as Issue, Show Problems Panel, Symbolize (native) |
| frozen thread | ANR | menu |
| lightning | exception | menu |
| stopwatch | StrictMode violation | menu |
| globe | network call | menu: go to request/response, Copy as cURL, Show Network Panel |
| yellow bookmark | bookmarked line | remove the bookmark |
| yellow triangle | warning | menu |
| (empty) | other lines | add a bookmark (a faint bookmark shows on hover) |

## Tags panel

![Tags panel](images/tag-tree.png)

- Tags sort by count, busiest first, with a per-second sparkline. With a project app known, they split into
  **Your app** and **Other**.
- The **checkbox** shows or hides a tag. The panel writes `tag=:` / `-tag=:` terms into the search text.
  A `tag=:` list keeps the app start and end lines; hide them with `-tag=:ActivityManager`.
- Hover a row for **only** (show only this tag).
- Click a row to select it; Cmd/Ctrl+click and Shift+click select more.
- **Groups:** drag selected tags onto another tag to make a new group ("Group 1", "Group 2" …), onto a group to
  move them there, or onto a section header or the empty space to take them out. A group's checkbox shows or
  hides all its tags. A group with no tags left goes away. Right-click a group to rename or delete it.
- Right-click a tag to write your own description of it (shown on hover), or to move it to a group.

## Network panel

Ktor and OkHttp requests paired with their responses: method, path, status and time. Click a call for the
request and response details. Copy as cURL from the row or the gutter menu.

![Network panel](images/network-detail.png)

## Bookmarks panel

Every bookmarked line of the project, in log order. Click to jump. A line that left the log (Clear, a full
buffer, a restart) is still saved: click it to open the saved copy. Bookmarks are saved in
`.idea/workspace.xml`; messages over 64 KB are cut.

![Bookmarks](images/bookmarks.png)

## Problems panel

Crashes, ANRs, exceptions, StrictMode violations and warnings per session, kept until you delete them.
See [Problems](problems.md).

## Files

The **Export** button opens a menu:
- **Export to File…** writes the shown lines to a `.txt` file.
- **Open Log File…** shows a file that Export wrote, in this pane, instead of the device. A bar shows the file
  name and **Back to Live Log**.
- **Compare With Log File…** opens the IDE diff viewer: the shown log next to the file.

## Devices

- The device picker lists connected devices and emulators.
- **Pair Device over Wi-Fi…**: on the device, open Developer options › Wireless debugging › Pair device with
  pairing code. Enter the pairing address and the code; enter the device address too if adb does not connect
  on its own.
- **Screenshot** and **Screen record** (MP4 on devices, WebM on emulators) with options and a save folder.

## Tabs and panes

Each tab and split pane has its own device and filter. Split right or down from the toolbar; close a pane with
its close button. Tabs and splits are remembered per project.

Start a different project app from the IDE, for example after a build variant switch, and the old app can keep
running next to the new one. Tabs with `package:mine` then change to the package of the old app
(`package:com.example.alpha.debug`), and a new tab opens with the same filter for the new app
(`package:com.example.beta.debug`). Each tab keeps its own log and tag list. If a tab for the new app is open
already, it gets the focus instead. Turn this off in Settings › Tools › Better Logcat.

## Settings

**View settings** (gear button): columns, time format, colors, keyword highlights, presets (OkHttp, Ktor,
Retrofit …), package aliases, retrace, with a live preview. Changes apply at once.

**Settings › Tools › Better Logcat** (also from View settings › "Open Buffer and adb Settings…"):

| Setting | Default |
|---|---|
| adb path (empty: found automatically) | empty |
| Keep at most this many entries | 100,000 |
| Show the existing device log when a device connects | on |
| Clear also clears the log on the device | on |
| Keep the last log of each device after an IDE restart | on |
| Open a new tab when you start a different project app | on |
| Keep bookmarks after Clear and IDE restart | on |
| Notify me of a crash or ANR while the tool window is hidden | on |
| Keep problem sessions for this many days | 30 |
| Hidden base URLs (add, edit, remove) | none |

## Keyboard

| Keys | Action |
|---|---|
| Cmd/Ctrl+F | Find |
| Ctrl+Space | search suggestions |
| Cmd/Ctrl+C, Cmd/Ctrl+A | copy, select all |
| ↑ ↓ Page Up/Down Home End (+Shift) | move (extend) the selection |
| → ← | expand, collapse the selection |
| Enter, Space | expand or collapse the selected row |
| Shift+F10, menu key | row menu |
| Esc | clear the selection, leave text selection |
