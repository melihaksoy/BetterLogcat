# Search terms

The search field takes free text and `key:value` terms, like Android Studio's Logcat. A term is applied
once a space follows it, so a half-typed term does not filter yet. Press **Ctrl+Space** for suggestions.

Terms are colored in the field and stay editable text. The filter is saved with favorites and history,
and the last filter comes back next time.

## How terms combine

| Form | Meaning |
|---|---|
| `key:value` | contains `value` (case does not matter) |
| `key=:value` | equals `value`; takes a comma list: `tag=:OkHttp,GarageApp` |
| `key~:regex` | matches the regular expression |
| `-key:value` | excludes what the term matches (works with `=:` and `~:` too) |
| `"two words"` | quote a value that has spaces: `tag:"Ktor Client"` |

Terms with the **same key** combine with OR: `tag:OkHttp tag:Retrofit` shows both.
Terms with **different keys** combine with AND: `tag:OkHttp level:W` shows OkHttp warnings and errors.
Free text left over matches the message or the tag. The **.\*** button makes it a regular expression,
**Cc** makes it case sensitive.

## Keys

| Key | Matches | Example |
|---|---|---|
| `package:` | the app (process) that logged the line | `package:mine`, `package=:com.example.app` |
| `package:mine` | your project's application ids | `package:mine` |
| `tag:` | the log tag | `-tag=:art,adbd` |
| `message:` | the message text | `message~:timeout\|refused` |
| `line:` | the whole line: process, level, tag and message | `line:"E OkHttp"` |
| `level:` | this level or higher (`V D I W E A`, or the full name) | `level:W` |
| `process:` | the process name | `process:remote` |
| `pid:` | the process id | `pid:14302` |
| `age:` | lines newer than this: `s`, `m`, `h`, `d` | `age:5m` |
| `is:` | a kind of line, see below | `is:crash` |
| `context:` | show N lines before and after each match, dimmed (does not filter) | `context:3` |
| `name:` | names a saved filter (does not filter) | `name:"Login flow"` |

## `is:` values

| Value | Lines |
|---|---|
| `is:crash` | Java/Kotlin crashes (`FATAL EXCEPTION`), asserts, native crashes (`Fatal signal`) |
| `is:anr` | "ANR in …" lines from ActivityManager |
| `is:exception` | lines that name a throwable, like `java.io.IOException: closed` |
| `is:strictmode` | StrictMode policy violations |
| `is:problem` | any of the above, and warnings |
| `is:stacktrace` | lines with stack frames |
| `is:json` | lines with JSON |

## Examples

```
package:mine level:W                      warnings and errors of your app
package:mine -tag=:Choreographer          your app, without frame drop noise
tag:OkHttp message:"HTTP FAILED" context:5 failed calls with 5 lines around each
is:crash age:10m                          crashes in the last 10 minutes
is:exception -is:crash                    exceptions that did not crash the app
```

The Tags panel writes `tag=:` and `-tag=:` terms for you, so its choices are part of the filter text.

![Suggestions](images/suggest-keys.png)
