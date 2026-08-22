---
kind: configuration_system
name: Gradle-Centric Build & Runtime Configuration with Centralized Dependency Versions and In-App Configure Singleton
category: configuration_system
scope:
    - '**'
source_files:
    - config.gradle
    - build.gradle
    - gradle.properties
    - mvvm/build.gradle
    - app/build.gradle
    - mvvm/src/main/java/com/ved/framework/utils/Configure.java
    - mvvm/src/main/java/com/ved/framework/net/RetrofitClient.java
    - mvvm/src/main/java/com/ved/framework/utils/bland/code/UtilsActivityLifecycleImpl.java
    - mvvm/src/main/java/com/ved/framework/utils/Constant.java
    - mvvm/src/main/java/com/ved/framework/config/MyAppGlideModule.java
    - mvvm/src/main/AndroidManifest.xml
    - app/src/main/AndroidManifest.xml
---

## What system/approach is used

The project uses a **Gradle multi-module build** (app + mvvm library) with centralized dependency/version management via a top-level `config.gradle` Groovy script, plus Android resource files (`res/values/*`) for compile-time string/color/style constants. At runtime the framework exposes a small in-process configuration singleton (`Configure`) that holds network base URLs and JSON response key names, which are consumed by the networking layer.

There is no external config file format (no `.env`, `.yaml`, `.toml`, or properties files loaded at runtime). All build-time configuration lives in Gradle; all app-level runtime configuration is set programmatically before first use.

## Key files and packages

- **Build / dependency configuration**
  - `build.gradle` (root): applies `config.gradle`, declares Kotlin version, maven repositories (Aliyun, Maven Central, JCenter, Google, JitPack, internal Nexus, custom GitHub maven), and classpath plugins.
  - `config.gradle`: defines `ext { android = {...}, versions = {...}, support = {...}, dependencies = {...} }` — single source of truth for SDK versions, third-party artifacts, and their coordinates.
  - `gradle.properties`: project-wide Gradle flags (`org.gradle.jvmargs`, `android.useAndroidX=true`, `android.enableJetifier=true`, `android.overridePathCheck=true`).
  - `settings.gradle`: module declarations (not shown but implied).
  - `app/build.gradle`: application module build config (compileSdk/targetSdk/minSdk, DataBinding enabled, buildTypes, dependencies). Repeats some values instead of referencing `config.gradle`.
  - `mvvm/build.gradle`: library module build config (DataBinding, debug/release buildTypes, Java 8 compatibility, dependency resolution strategy forcing specific lifecycle-ktx versions to avoid conflicts).

- **Runtime configuration**
  - `mvvm/src/main/java/com/ved/framework/utils/Configure.java`: process-wide singleton holding a list of API base URLs keyed by integer code, plus configurable JSON response field names (`codeKey`, `msgKey`, `dataKey`). Provides `setUrl(code, ...urls)` and `setResponseKeys(codeKey, msgKey, dataKey)`.
  - `mvvm/src/main/java/com/ved/framework/net/RetrofitClient.java`: consumes `Configure.getUrl()` to set Retrofit base URL per code and reads `Configure.getCodeKey()` / `Configure.getMsgKey()` when parsing responses in an interceptor.
  - `mvvm/src/main/java/com/ved/framework/utils/bland/code/UtilsActivityLifecycleImpl.java`: auto-populates `Configure` from Activity metadata at startup (reads `url` and response key metadata and calls `Configure.setUrl(...)` / `Configure.setResponseKeys(...)`).
  - `mvvm/src/main/java/com/ved/framework/utils/Constant.java`: compile-time constants reused by networking and UI (e.g. `DEFAULT_TIMEOUT = 30`, `CACHE_TIMEOUT = 10 * 1024 * 1024`).
  - `mvvm/src/main/java/com/ved/framework/config/MyAppGlideModule.java`: Glide's `@GlideModule` marker — a minimal extension point for image loading configuration.
  - `mvvm/src/main/AndroidManifest.xml`: declares framework components, meta-data for notch/scoped storage, cleartext traffic, FileProvider, and a background `:error_activity` process.
  - `app/src/main/AndroidManifest.xml`: minimal app manifest; no `<application>` subclass declared here.
  - Resource strings: `app/src/main/res/values/strings.xml` (only `app_name`); `mvvm/src/main/res/values/strings.xml` (crash/error messages, file picker labels).

## Architecture and conventions

1. **Centralized dependency & version management**: `config.gradle` is the single place where Android SDK levels, third-party library versions, and artifact coordinates are defined. The `mvvm` library module references them via `rootProject.ext.dependencies[...]` and `rootProject.ext.support[...]`. The `app` module currently duplicates many settings directly in its own `build.gradle` rather than reusing `config.gradle`, which is a deviation from the intended convention.

2. **Multi-module separation of concerns**: The `mvvm` module is a reusable framework library (published via `com.github.dcendents.android-maven` plugin to a custom maven repo). It ships its own manifest with framework components, permissions, and providers. The `app` module is a thin demo application that depends on the library.

3. **Runtime configuration via a process-global singleton**: `Configure` is a static-holder class (private constructor, throws `UnsupportedOperationException` on instantiation). Base URLs are stored as a `List<String>` indexed by an integer code; the default response wrapper keys are `code/msg/data` but can be overridden once at startup via `setResponseKeys(...)`. This is consumed by `RetrofitClient` (base URL selection, response parsing) and by any other component that needs the configured keys.

4. **Automatic initialization from manifest metadata**: `UtilsActivityLifecycleImpl` reads Activity metadata entries for URLs and response keys and calls `Configure.setUrl` / `Configure.setResponseKeys` during activity lifecycle callbacks, so callers do not need to manually configure before making requests.

5. **Network client built around OkHttp + Retrofit**: `RetrofitClient` builds a shared `OkHttpClient` (singleton via DCL) with cache directory under `cacheDir/ved_cache`, persistent cookies via `PersistentCookieStore`, SSL socket factory, logging interceptor, timeouts taken from `Constant.DEFAULT_TIMEOUT`, connection pool (8 idle, 15s keep-alive), and no proxy. Per-call clients are created via an inner `HttpClientBuilder` that adds request-specific headers, interceptors, and optional overrides.

6. **Resource-based configuration**: Android resources (`res/values/colors.xml`, `dimens.xml`, `strings.xml`, `styles.xml`, `attrs.xml`, `arrays.xml`, `integers.xml`) hold UI/theme constants. The library manifest uses `meta-data` entries for device features (notch support, max aspect ratio, scoped storage flag).

7. **Build-type configuration**: Both modules define `debug` and `release` build types with ProGuard rules applied. Debug enables logging; release disables minification in this project. The library also forces specific `lifecycle-*-ktx` versions to resolve transitive dependency conflicts.

## Conventions and constraints

- **Dependency versions must be added to `config.gradle`**, not duplicated in module `build.gradle` files. The `mvvm` module follows this convention by using `rootProject.ext.dependencies[...]`; the `app` module currently does not, which is inconsistent.
- **Base URLs and response wrapper keys must be set before any network call**. `Configure.getUrl()` throws `NullPointerException` if no URL has been registered, and `RetrofitClient.create(...)` will fail if the list is empty.
- **JSON response key names should be configured once via `Configure.setResponseKeys(...)`** when the backend uses non-default keys (e.g. `status/message/result`). The README documents this pattern and warns that it only affects the framework's fallback parsing and interceptor behavior, not custom `IEntityResponse` implementations.
- **Constants belong in `Constant.java`** (e.g. `DEFAULT_TIMEOUT`, `CACHE_TIMEOUT`, `CLICK_INTERVAL`) and are referenced across networking, caching, and UI code rather than being hard-coded inline.
- **Framework components are declared in the library manifest** (`mvvm/src/main/AndroidManifest.xml`), including activities, services, providers, and meta-data. The app manifest stays minimal and relies on manifest merging.
- **Glide configuration is done through a `@GlideModule`** (`MyAppGlideModule`) rather than global builder calls; extending it is the documented extension point.
- **Gradle properties in `gradle.properties` are project-wide defaults** (JVM args, AndroidX migration flags, path override) and are not meant to be changed per-build without understanding their side effects.