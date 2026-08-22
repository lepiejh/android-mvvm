---
kind: logging_system
name: Custom KLog Utility and OkHttp Logging Interceptor
category: logging_system
scope:
    - '**'
source_files:
    - mvvm/src/main/java/com/ved/framework/utils/KLog.java
    - mvvm/src/main/java/com/ved/framework/http/interceptor/logging/LoggingInterceptor.java
    - mvvm/src/main/java/com/ved/framework/http/interceptor/logging/Level.java
    - mvvm/src/main/java/com/ved/framework/http/interceptor/logging/Logger.java
    - mvvm/src/main/java/com/ved/framework/http/interceptor/logging/Printer.java
---

## What system/approach is used

The project implements a **two-layer logging system**:

1. **Application-level logging via a custom `KLog` utility** (`mvvm/src/main/java/com/ved/framework/utils/KLog.java`) — a static facade over Android's `android.util.Log` that adds structured metadata (caller class, method, line number), JSON pretty-printing, and optional asynchronous file persistence to the app's internal storage.
2. **HTTP request/response logging via a custom OkHttp `LoggingInterceptor`** (`mvvm/src/main/java/com/ved/framework/http/interceptor/logging/LoggingInterceptor.java`) with supporting classes `Level`, `Logger`, and `Printer` that format network traffic into human-readable boxes and delegate output to either OkHttp's `Platform` logger or a pluggable `Logger` interface.

There is no centralized log framework (e.g., Timber, SLF4J, Logcat-based library); both layers are hand-rolled utilities within the shared `:mvvm` library module.

## Key files and packages

- `mvvm/src/main/java/com/ved/framework/utils/KLog.java` — main application logger API (`v/d/i/w/e/a/json(...)`) plus file-log management (`initFileLog`, `setSaveToFile`, `getLogFiles`, `exportLogsToExternal`, `clearAllLogFiles`).
- `mvvm/src/main/java/com/ved/framework/http/interceptor/logging/LoggingInterceptor.java` — OkHttp interceptor that conditionally logs requests/responses based on a `Builder` configuration.
- `mvvm/src/main/java/com/ved/framework/http/interceptor/logging/Level.java` — enum of HTTP log verbosity: `NONE`, `BASIC`, `HEADERS`, `BODY`.
- `mvvm/src/main/java/com/ved/framework/http/interceptor/logging/Logger.java` — pluggable sink interface; default implementation delegates to `okhttp3.internal.platform.Platform.log`.
- `mvvm/src/main/java/com/ved/framework/http/interceptor/logging/Printer.java` — formats request/response payloads into box-drawn text blocks and splits long lines for logcat compatibility.

## Architecture and conventions

### Application logging (`KLog`)
- **Global switch**: `KLog.IS_SHOW_LOG` controls whether messages reach `android.util.Log`; `KLog.IS_SAVE_TO_FILE` controls async file writes. Both default to `false`.
- **Levels**: V, D, I, W, E, A (assert via `Log.wtf`), plus a dedicated `JSON` level that parses strings starting with `{` or `[` and prints them inside a `║ ... ║` bordered block.
- **Structured fields**: every log line embeds caller info extracted from `Thread.currentThread().getStackTrace()` at index 4 — filename, method name (capitalized first letter), and line number — formatted as `[ (ClassName:line)#MethodName ] message`.
- **File sink**: when enabled via `initFileLog(context, isSaveToFile, logDir)`, logs are written asynchronously on a single-thread `ExecutorService` to `<context.getFilesDir>/logs/app_log_YYYY-MM-dd.txt`. Each entry is prefixed `[timestamp] [LEVEL] [tag] content`. Files are archived once they exceed `MAX_LOG_FILE_SIZE = 5 MB` and only the newest `MAX_LOG_FILES = 10` are retained. Export helpers write all accumulated logs to external storage.
- **No per-call tag required**: if a tag is not supplied, the calling class name is used automatically.

### HTTP logging (`LoggingInterceptor` + `Printer`)
- **Configuration via Builder**: `isDebug`, `level` (`NONE|BASIC|HEADERS|BODY`), `requestTag`/`responseTag`, `log(int)` type, and an optional `Logger` override.
- **Conditional execution**: if `!isDebug` or `level == Level.NONE`, the chain proceeds without logging.
- **Content-type gating**: only bodies whose subtype contains `json`, `xml`, `plain`, or `html` are read and pretty-printed; other types print `Omitted request/response body`.
- **Output formatting**: `Printer` wraps each request in a `┌────── Request ──...` / `└───────────────────────────────────` box and each response similarly, including URL, method, headers (when allowed by level), form-encoded key=value pairs, status code, success flag, elapsed time in ms, and body.
- **Sink abstraction**: if a custom `Logger` is set via `Builder.logger(...)`, it receives `(type, tag, message)`; otherwise the default `Logger.DEFAULT` uses `Platform.get().log(...)`.

### Conventions observed
- All logging lives in the shared `:mvvm` library under `com.ved.framework.*`; the demo `app` module does not define its own logger.
- Log output is gated behind build-time flags (`IS_SHOW_LOG`, `isDebug`) so production builds can emit nothing.
- Network logs are separated from application logs by package and by distinct tags (`LoggingI` default vs. caller-derived tags).
- Long log lines are split at fixed widths (110 chars for normal lines, variable for others) to stay within logcat limits.
- JSON payloads are always pretty-printed with indentation before being logged or persisted.

## Conventions and constraints

- **No global log level filter per module** — `KLog` uses a single boolean `IS_SHOW_LOG`; there is no fine-grained level filtering (e.g., "only show WARN+"), though individual methods exist for each level.
- **File rotation is size-based per day** — one file per calendar date, archived after 5 MB, capped at 10 files; no rolling window or retention policy beyond this.
- **Tags are derived, not configured** — application logs derive tags from the call stack; HTTP logs use configurable `requestTag`/`responseTag` falling back to `LoggingI`.
- **Sinks are hard-coded to Android Logcat and local filesystem** — there is no remote log shipping, crash-report integration, or structured JSON-on-disk format beyond the plain `.txt` layout.
- **HTTP interceptor is opt-in** — it must be explicitly built and added to the OkHttp client via `new LoggingInterceptor.Builder()...build()`; it does not auto-register.
- **Assertions use `Log.wtf`** — the `a(...)` method maps to `android.util.Log.wtf`, treating asserts as fatal.
- **No cross-cutting log context** (e.g., user ID, session, correlation ID) is injected into log entries; only caller identity and timestamp are included.