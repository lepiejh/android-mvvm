---
kind: build_system
name: Gradle Multi-Module Android Build with Centralized Config and JitPack Publishing
category: build_system
scope:
    - '**'
source_files:
    - build.gradle
    - settings.gradle
    - config.gradle
    - gradle.properties
    - app/build.gradle
    - mvvm/build.gradle
    - jitpack.yml
---

## Build System Overview

This is a Gradle-based multi-module Android project composed of two modules: `:app` (MVVM demo application) and `:mvvm` (reusable MVVM framework library). The build system uses the Android Gradle Plugin (AGP) 4.0.2 with Kotlin 1.6.21, Data Binding enabled in both modules, and Java 8 source/target compatibility.

## Key Files and Structure

- **Root `build.gradle`**: Declares AGP 4.0.2 classpath, applies `config.gradle`, configures shared Maven repositories (Aliyun Nexus mirror, Maven Central, JCenter, Google, JitPack, plus internal `nexus.yun-chang.cn` and a custom mvvm-framework maven repo), and defines a root `clean` task.
- **`settings.gradle`**: Includes `:mvvm` and `:app`; sets `rootProject.name = "android-mvvm"`.
- **`config.gradle`**: Centralized dependency and version management via `ext { ... }`. Defines `android` block (`compileSdkVersion 33`, `minSdkVersion 19`, `targetSdkVersion 33`, `versionCode 1`, `versionName "2.0.3"`), a `versions` map for shared versions, a `support` map for AndroidX/Support libraries, and a `dependencies` map covering RxJava 3, Retrofit 2.9.0, Glide 4.14.2, Material Dialogs, Lifecycle components, SmartRefreshLayout, etc. All module dependencies reference these maps via `rootProject.ext.dependencies[...]` / `rootProject.ext.support[...]`.
- **`gradle.properties`**: Sets `org.gradle.jvmargs=-Xmx1536m`, enables AndroidX (`android.useAndroidX=true`) and Jetifier (`android.enableJetifier=true`), and overrides path checks (`android.overridePathCheck=true`).
- **`app/build.gradle`**: Application module using `com.android.application`, Kotlin plugins, Data Binding, `multiDexEnabled true`, release build type with ProGuard (`proguard-rules.pro`), and Lint disabled for release builds (`checkReleaseBuilds false`, `abortOnError false`). Currently has the `:mvvm` dependency commented out.
- **`mvvm/build.gradle`**: Library module using `com.android.library` with the `com.github.dcendents.android-maven` plugin applied, publishing group `com.github.lixiong`. Uses `api` for transitive dependencies consumers need (RxJava, Retrofit, Glide, Lifecycle, Coroutines, etc.) and `compileOnly` for optional ones (ConstraintLayout, SwitchButton, Album). Applies forced resolution for lifecycle-ktx artifacts to 2.4.0 to avoid JDK 8 javac `MODULE` attribute errors with newer versions. Enables Data Binding and Java 8 compilation.
- **`jitpack.yml`**: Publishes the library to JitPack; specifies OpenJDK 11, skips source compilation (`skip: true`), runs `./gradlew clean assembleRelease` before publish.

## Architecture and Conventions

- **Centralized dependency management**: All third-party versions live in `config.gradle` under `ext.dependencies` and `ext.support`. Modules consume them through `rootProject.ext.dependencies["key"]` or `rootProject.ext.support["key"]`, ensuring consistent versions across modules.
- **Repository mirroring**: The top-level build script declares multiple repository sources, prioritizing an Aliyun Nexus mirror (`http://maven.aliyun.com/nexus/content/groups/public/`) alongside Maven Central, JCenter, Google, JitPack, and two internal/custom repos. This is intended for faster downloads in China-based environments.
- **Library vs app separation**: The `:mvvm` module is published as a Maven artifact (`group='com.github.lixiong'`) via the `android-maven` Gradle plugin and can be consumed from JitPack (per `jitpack.yml`). The `:app` module depends on it via `implementation project(path: ':mvvm')` (currently commented out).
- **Dependency visibility strategy**: The library module uses `api` for dependencies that must be exposed to consumers (networking, RxJava, Glide, Lifecycle, Coroutines) and `compileOnly` for optional UI/tooling dependencies that the consuming app should provide itself.
- **Conflict resolution**: A `configurations.all { resolutionStrategy { force ... } }` block forces specific `lifecycle-*` ktx artifacts to 2.4.0 to resolve conflicts introduced by espresso test dependencies pulling older versions.
- **Build types**: Both modules define `debug` and `release` build types; `release` disables minification (`minifyEnabled false`) but applies ProGuard rules files.
- **Multi-Dex**: Enabled in both modules (`multiDexEnabled true`) to support large dependency graphs on lower API levels.

## Conventions and Constraints

- **SDK targets are pinned centrally**: `compileSdkVersion 33`, `minSdkVersion 19`, `targetSdkVersion 33` are defined in `config.gradle` and duplicated in each module's `android {}` block — modules should not override these values independently.
- **Kotlin version is centralized**: `kotlin_version` is declared in the root `buildscript.ext` and referenced via `$kotlin_version` in module dependencies.
- **Data Binding is required**: Both modules apply `buildFeatures { dataBinding = true }`; the library module suppresses the linter warning with `//noinspection DataBindingWithoutKapt`.
- **ProGuard rules per module**: Each module ships its own `proguard-rules.pro`; release builds include the default Android optimize file plus the module-specific rules.
- **Lint is relaxed for releases**: The app module sets `checkReleaseBuilds false` and `abortOnError false`, so lint failures do not block release builds.
- **AndroidX + Jetifier enforced**: `gradle.properties` enforces AndroidX usage and automatic conversion of legacy Support libraries via Jetifier.
- **Publishing target**: The library is built and published to JitPack using OpenJDK 11; the `before_install` step runs `./gradlew clean assembleRelease`, so only release artifacts are published.
- **No CI pipeline beyond JitPack**: There is no GitHub Actions, Jenkinsfile, or other CI configuration in the repository; automated building is delegated to JitPack via `jitpack.yml`.