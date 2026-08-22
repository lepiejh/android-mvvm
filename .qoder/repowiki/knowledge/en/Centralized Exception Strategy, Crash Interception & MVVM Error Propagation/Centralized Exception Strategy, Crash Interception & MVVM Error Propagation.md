---
kind: error_handling
name: Centralized Exception Strategy, Crash Interception & MVVM Error Propagation
category: error_handling
scope:
    - '**'
source_files:
    - mvvm/src/main/java/com/ved/framework/http/ErrorCode.java
    - mvvm/src/main/java/com/ved/framework/http/ResponseThrowable.java
    - mvvm/src/main/java/com/ved/framework/http/ExceptionHandle.java
    - mvvm/src/main/java/com/ved/framework/http/strategy/IExceptionStrategy.java
    - mvvm/src/main/java/com/ved/framework/http/strategy/AbstractExceptionStrategy.java
    - mvvm/src/main/java/com/ved/framework/http/strategy/ExceptionHandlerRegistry.java
    - mvvm/src/main/java/com/ved/framework/net/ARequest.java
    - mvvm/src/main/java/com/ved/framework/net/ISeatError.java
    - mvvm/src/main/java/com/ved/framework/net/ResultException.java
    - mvvm/src/main/java/com/ved/framework/base/BaseRepository.java
    - mvvm/src/main/java/com/ved/framework/base/BaseViewModel.kt
    - mvvm/src/main/java/com/ved/framework/crash/CaocConfig.java
    - mvvm/src/main/java/com/ved/framework/crash/DefaultErrorActivity.java
    - mvvm/src/main/java/com/ved/framework/crash/CaocInitProvider.java
---

## Overview

The framework defines a layered error-handling system that spans three scopes: (1) **unhandled crash interception** at the process level, (2) **network/HTTP exception classification** via a strategy registry, and (3) **MVVM-layer error propagation** from repositories through `ARequest` to view-level error seats. Errors are never thrown as raw exceptions into UI code; they are wrapped in typed carriers (`ResponseThrowable`, `ResultException`) and routed through callbacks or LiveData.

## 1. Unhandled-crash interception (process-level)

- `crash/CaocConfig` configures CustomActivityOnCrash behavior: background mode, whether to show details/restart button, activity tracking, min time between crashes, drawable, custom error/restart activities, and an event listener for analytics.
- `crash/CaocInitProvider` initializes the crash interceptor during app startup.
- `crash/CustomActivityOnCrash` is the core library entry point (vendored).
- `crash/DefaultErrorActivity` extends `BaseActivity<AbBinding, BaseViewModel>` and renders a user-facing error screen with restart/close buttons, an optional "more info" dialog showing full stack traces, and clipboard copy of error details.

This layer catches any uncaught `Throwable` that escapes the app's normal flow and presents a controlled recovery UI instead of letting Android's native crash dialog terminate the process silently.

## 2. Network / HTTP exception handling (strategy pattern)

### Core types
- `http/ErrorCode` — constants defining the framework's canonical error codes: `UNKNOWN=1000`, `PARSE_ERROR=1001`, `NETWORD_ERROR=1002`, `HTTP_ERROR=1003`, `SSL_ERROR=1005`, `TIMEOUT_ERROR=1006`.
- `http/ResponseThrowable` — a checked `Exception` carrying an `int code` and `String message`; produced by every strategy.
- `http/ExceptionHandle` — static facade exposing `handleException(Throwable)` and re-exporting `ERROR.*` constants for backward compatibility.

### Strategy registry
- `http/strategy/IExceptionStrategy` — interface with `matches(Throwable)` and `handle(Throwable)` returning `ResponseThrowable`.
- `http/strategy/AbstractExceptionStrategy` — template method that constructs `ResponseThrowable` from subclass-provided `matchType`, `code`, `message`.
- Concrete strategies: `HttpExceptionStrategy`, `ParseExceptionStrategy`, `ConnectExceptionStrategy`, `SslExceptionStrategy`, `TimeoutExceptionStrategy`, `UnknownExceptionStrategy`.
- `ExceptionHandlerRegistry` — singleton list of strategies initialized in a static block with a fixed order: HTTP → Parse → Connect → SSL → Timeout → Unknown (the catch-all must be last). Provides `register(...)` / `registerAll(...)` so consumers can add custom strategies while preserving the open/closed principle.

When `ExceptionHandle.handleException(e)` is called, it logs via `KLog.e("--NET--", ...)` and delegates to `ExceptionHandlerRegistry.handle(e)`, which iterates strategies in registration order and returns the first match; if none match, it falls back to `UnknownExceptionStrategy`.

## 3. MVVM-layer error propagation

### Repository & ViewModel
- `base/BaseRepository` wraps RxJava subscriptions in a `CompositeDisposable` and clears them in `onCleared()` to prevent leaks.
- `base/BaseViewModel.kt` manages coroutine jobs in a `ConcurrentHashMap<String, Job>` keyed by job name. The private `launchManagedTask(key, onError, onCancel, task)` wraps each task in try/catch:
  - `CancellationException` is routed to `onCancel`.
  - Any other `Exception` is marshaled back to `Dispatchers.Main` and passed to `onError`.
  - Jobs are removed from the map in `finally`.
  - `fetchWithCancel` and `delayedAction` expose this template to callers.
- `BaseViewModel.onCleared()` calls `model?.onCleared()`, clears disposables, cancels all jobs, and cancels `viewModelScope`, catching any exception to avoid teardown failures.

### Request pipeline (`net/ARequest`)
`ARequest<T, K>` is the central builder for network calls. Its `build()` method chains:
1. Network availability check — if offline, invokes `iResponse.onError(...)` and `seatSuccess.onNoNetworkView()`.
2. Optional loading state via `viewModel.showDialog()`.
3. Executes the Retrofit call inside a try/catch; errors are caught and dispatched to `dispatchError`.
4. Uses `RxUtils.schedulersTransformer()` and `compose(RxUtils.bindToLifecycle(...))` so requests are bound to the view lifecycle and cancelled on destroy.
5. `onErrorResumeNext` logs the throwable and calls `parseError` before re-emitting the error.
6. `doOnDispose` evicts OkHttp idle connections to avoid reuse of half-dead connections after cancellation.
7. Subscriptions are added to `viewModel.accept(disposable)` so they are disposed when the ViewModel clears.

### Error dispatch in `ARequest`
- `isCanceledException(ResponseThrowable)` treats OkHttp `IOException("Canceled")` and `SocketException("Socket closed")` as expected cancellations — these skip all error UI/callbacks and only dismiss loading.
- `parseError` dismisses loading, skips canceled exceptions, then:
  - Calls `seatError.onErrorView()` if a view state holder is present.
  - Calls `iResponse.onError(error, false)` if provided.
  - If the cause is `net/ResultException`, routes through `handleResultException` to propagate both the business error code and message to `seatError.onErrorHandler(code)` and `iResponse.onError`.
  - Otherwise logs `throwable.message` and calls `seatError.onEmptyView(message)`.
- `exceptionHandling(viewModel, error, code)` is an abstract hook that subclasses implement to integrate with global error reporting (e.g., Sentry, analytics).

### View-level error contracts
- `net/ISeatError` — callback contract for view layers: `onErrorView()`, `onErrorHandler(int)`, `onEmptyView()`, `onEmptyView(String)`.
- `net/ISeatSuccess` — paired success/state callbacks (e.g., `onStateView`, `onNoNetworkView`).
- `net/IResponse<K>` — generic response envelope used by `ARequest` to deliver `onSuccess(response)` and `onError(message, boolean)`.

## 4. Conventions & constraints observed

| Area | Convention / Constraint | Evidence |
|---|---|---|
| **Crash interception** | All unhandled crashes are intercepted by CustomActivityOnCrash and presented via `DefaultErrorActivity` rather than crashing to the system dialog. | `CaocConfig`, `CaocInitProvider`, `CustomActivityOnCrash`, `DefaultErrorActivity` form a complete crash-capture chain. |
| **Network errors** | Every network error is converted to `ResponseThrowable` with a canonical integer code from `ErrorCode`. Raw exceptions are never propagated past the request layer. | `AbstractExceptionStrategy.handle` always wraps in `ResponseThrowable`; `ARequest.subscribe` consumes `ResponseThrowable`. |
| **Strategy ordering** | Generic/fallback strategies must be registered last; specific strategies (HTTP, Parse, Connect, SSL, Timeout) are registered before `UnknownExceptionStrategy`. | `ExceptionHandlerRegistry` static block comment: "兜底策略必须最后注册" and explicit registration order. |
| **Extensibility** | New exception types are added by implementing `IExceptionStrategy` and registering via `ExceptionHandlerRegistry.register(...)`, without modifying existing code. | Registry exposes `register`/`registerAll`; comments cite open/closed principle. |
| **ViewModel async safety** | Coroutines launched via `launchManagedTask` always catch exceptions and marshal them to the main thread before invoking `onError`; cancellations are handled separately. | `BaseViewModel.kt` `launchManagedTask` try/catch blocks around `task()`. |
| **Subscription lifecycle** | All RxJava subscriptions are added to a `CompositeDisposable` managed by `BaseRepository`/`BaseViewModel` and cleared in `onCleared()` to prevent memory leaks. | `BaseRepository.addSubscribe` + `onCleared`; `BaseViewModel` implements `ISubscription` and clears disposables. |
| **Cancellation semantics** | User-initiated cancellations (`IOException("Canceled")`, `SocketException("Socket closed")`) are treated as non-errors: loading is dismissed but no error UI or callbacks fire. | `ARequest.isCanceledException` and early return in `parseError`. |
| **Business vs transport errors** | Business errors from the server are wrapped as `ResultException` (containing `errMsg`, `errCode`) and routed through `handleResultException` to reach `seatError.onErrorHandler(code)`. | `ARequest.handleResultException` branches on `cause instanceof ResultException`. |
| **UI-thread guarantee** | Error callbacks are dispatched onto the UI thread via `UiThreadDispatcher.runOnUiThread(viewModel, ...)` so view updates are safe. | `ARequest.dispatchError` uses `UiThreadDispatcher`. |

## Key files

- `mvvm/src/main/java/com/ved/framework/http/ErrorCode.java` — canonical error-code constants
- `mvvm/src/main/java/com/ved/framework/http/ResponseThrowable.java` — unified network exception carrier
- `mvvm/src/main/java/com/ved/framework/http/ExceptionHandle.java` — facade delegating to strategy registry
- `mvvm/src/main/java/com/ved/framework/http/strategy/IExceptionStrategy.java` — strategy interface
- `mvvm/src/main/java/com/ved/framework/http/strategy/AbstractExceptionStrategy.java` — template base class
- `mvvm/src/main/java/com/ved/framework/http/strategy/ExceptionHandlerRegistry.java` — ordered strategy chain
- `mvvm/src/main/java/com/ved/framework/net/ARequest.java` — request builder, error dispatch, cancellation handling
- `mvvm/src/main/java/com/ved/framework/net/ISeatError.java` — view-level error callback contract
- `mvvm/src/main/java/com/ved/framework/net/ResultException.java` — business-error wrapper
- `mvvm/src/main/java/com/ved/framework/base/BaseRepository.java` — subscription container
- `mvvm/src/main/java/com/ved/framework/base/BaseViewModel.kt` — coroutine job manager, error marshaling to main thread
- `mvvm/src/main/java/com/ved/framework/crash/CaocConfig.java` — crash-interceptor configuration
- `mvvm/src/main/java/com/ved/framework/crash/DefaultErrorActivity.java` — user-facing crash UI
- `mvvm/src/main/java/com/ved/framework/crash/CaocInitProvider.java` — crash interceptor bootstrap
