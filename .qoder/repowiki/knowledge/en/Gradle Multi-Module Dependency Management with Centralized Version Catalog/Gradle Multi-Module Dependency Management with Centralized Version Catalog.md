---
kind: dependency_management
name: Gradle Multi-Module Dependency Management with Centralized Version Catalog
category: dependency_management
scope:
    - '**'
source_files:
    - build.gradle
    - config.gradle
    - settings.gradle
    - gradle.properties
    - mvvm/build.gradle
    - app/build.gradle
---

## System / Approach

This Android project uses **Gradle** as its build and dependency management system across a two-module setup (`:app` demo application and `:mvvm` framework library). Dependencies are declared in Gradle `dependencies { }` blocks, not via lockfiles (no `gradle.lockfile`, no `*.lock` files exist). There is no vendoring of third-party JARs/AARs — all artifacts are resolved remotely from Maven repositories.

## Key Files

- `build.gradle` (root): Declures the Android Gradle Plugin (`com.android.tools.build:gradle:4.0.2`) and Kotlin Gradle plugin (`kotlin_version = 1.6.21`). Defines the shared repository list used by every subproject.
- `config.gradle`: The central version catalog. All third-party versions live here under `ext.versions`, `ext.support`, and `ext.dependencies`. Modules reference them via `rootProject.ext.*`.
- `settings.gradle`: Registers the two modules `:mvvm` and `:app`.
- `gradle.properties`: Enables AndroidX (`android.useAndroidX=true`) and Jetifier (`android.enableJetifier=true`) so legacy support libraries can coexist with modern code; also sets JVM heap for Gradle daemon.
- `mvvm/build.gradle`: The framework library module that consumes the centralized dependencies and publishes itself via `com.github.dcendents.android-maven` with group `com.github.lixiong`.
- `app/build.gradle`: Demo app module that currently declares most of its own dependencies directly rather than pulling from `config.gradle`; it also includes a local `libs/` flat directory via `flatDir`.

## Repositories & Sources

The root `build.gradle` configures a fixed set of Maven repositories applied to both `buildscript` and `allprojects`:

1. `http://maven.aliyun.com/nexus/content/groups/public/` (Alibaba mirror)
2. `mavenCentral()`
3. `jcenter()`
4. `google()`
5. `https://jitpack.io`
6. `http://nexus.yun-chang.cn/nexus/content/groups/public` (private Nexus instance)
7. `https://raw.githubusercontent.com/qqlixiong/mvvm-framework-maven/master` (custom hosted Maven repo)

The `:app` module additionally adds a `flatDir { dirs 'libs' }` repository so local `.jar`/`.aar` files under `app/libs/` are resolvable.

## Architecture & Conventions

### Centralized version catalog
All shared dependency coordinates are defined once in `config.gradle` under three maps:
- `android` — SDK/build tooling versions (`compileSdkVersion`, `targetSdkVersion`, `minSdkVersion`, `versionCode`, `versionName`).
- `support` — AndroidX/support library coordinates (e.g. `appcompat-v7`, `recyclerview-v7`, `constraint-layout`).
- `dependencies` — Third-party libraries grouped by feature (rxjava, network, glide, gson, material-dialogs, lifecycle, SmartRefreshLayout, etc.).

Modules consume these via `rootProject.ext.dependencies.<key>` or `rootProject.ext.support.<key>`, keeping version numbers in one place.

### Dependency scope conventions in `:mvvm`
The library module distinguishes between transitive surface and internal use:
- `api` is used for libraries that consumers of the framework need at compile time (RxJava/RxAndroid, Retrofit, OkHttp, Glide, Gson, Lifecycle extensions, Coroutines, EventBus, MMKV, SmartRefreshLayout, etc.).
- `implementation` is used for internal-only helpers (material dialogs, ImmersionBar, mmDialog, autosize, SwipePanel, coroutine runtime).
- `compileOnly` is used for optional dependencies that may be provided by the host app (support-v4, appcompat-v7, recyclerview-v7, ConstraintLayout, SwitchButton, Album).
- `annotationProcessor` is used for compile-time annotation processors (Glide compiler, Lifecycle compiler).

### Conflict resolution
A `resolutionStrategy.force` block in `mvvm/build.gradle` pins specific `lifecycle-viewmodel-ktx`, `lifecycle-livedata-core-ktx`, and `lifecycle-livedata-ktx` to `2.4.0` to resolve conflicts introduced by `espresso androidTest` transitives. A comment documents why `2.5.1` cannot be forced (module-info.class incompatibility with JDK 8 javac).

### Support-library exclusion convention
Many dependencies still transitively pull `com.android.support` artifacts. The project consistently applies `{ exclude group: 'com.android.support' }` on those declarations (rxlifecycle, rxbinding, material-dialogs, glide, bindingcollectionadapter) to avoid mixing old support libraries with AndroidX.

### Publishing
The `:mvvm` module is published as an Android Maven artifact using `com.github.dcendents.android-maven` with `group='com.github.lixiong'`. No `publishToMavenLocal` or CI publish step is visible in the checked-in scripts.

## Constraints & Rules Observed

- **Single source of truth for versions**: New third-party libraries should be added to `config.gradle` under `ext.dependencies` and referenced through `rootProject.ext.dependencies.<name>` rather than hardcoding coordinates in module build scripts.
- **AndroidX + Jetifier enforced**: `gradle.properties` forces AndroidX usage and automatic Jetifier conversion, so new dependencies should prefer AndroidX coordinates.
- **Repository whitelist**: Only the seven repositories listed in the root `build.gradle` are trusted sources; adding new Maven sources must go there to propagate to all modules.
- **No lockfile**: Dependency versions are pinned only in `config.gradle` and individual `dependencies {}` blocks; there is no Gradle dependency lockfile, so reproducible builds rely on exact version strings.
- **Transitive exclusions required for legacy support libs**: Any dependency that pulls `com.android.support` transitively must explicitly exclude that group to keep the dependency graph clean.
- **JDK 8 compatibility constraint**: The `force` pin to lifecycle `2.4.0` (not higher) is enforced because newer versions contain `module-info.class` metadata incompatible with the project's JDK 8 `javac` target.