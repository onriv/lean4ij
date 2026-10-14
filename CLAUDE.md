# lean4ij — notes for coding agents

IntelliJ plugin for Lean 4 (LSP via LSP4IJ, Swing "internal" infoview, and a web "external" infoview served by
an embedded ktor server and shown in a JCEF tool window). Upstream: https://github.com/onriv/lean4ij.

## Layout
- `src/main/kotlin/lean4ij/` — plugin sources (`infoview/`, `infoview/external/` = JCEF + ktor, `lsp/`, `project/`, `actions/`, `language/`).
- `src/main/grammars/` — JFlex/Grammar-Kit sources, generated into `src/gen/java` (git-ignored, regenerated on every compile).
- `browser-infoview/` — React frontend of the external infoview; `buildPlugin` builds it with a downloaded Node 18
  and `processResources` copies `browser-infoview/dist` into the jar.
- `src/main/resources/META-INF/plugin.xml` — all registrations; `lean4ij-jcef.xml` is the (empty) optional config file
  for the `com.intellij.modules.jcef` dependency.
- `notes/` — dated investigation notes (log analyses, migration findings). Add a new file per investigation.
- `DEVELOP.md`, `docs/` — upstream developer docs.

## Building
- JDK 25 toolchain is required to *run* the build (2026.x+ platform jars are class file version 69); produced
  bytecode targets Java 21 (`jvmTarget`/`release` in build.gradle.kts) so 2024.2+ IDEs can still load the plugin.
  Kotlin 2.4.x is required because 2026.x+ IDEs ship Kotlin 2.4 metadata; `kotlin.stdlib.default.dependency=false`.
- `platformBundledModules` in gradle.properties lists platform modules that are not on the compile classpath by
  default anymore (execution, buildView, externalSystem, JCEF). Add there when a `com.intellij.*` import is unresolved;
  find the owning module with the IDE's `modules/module-descriptors.jar` or by scanning `lib/*.jar`.
- `instrumentCode` is disabled (no .form files; the instrumenter artifact does not exist for nightly builds).
- `./gradlew buildPlugin` → `build/distributions/lean4ij-<version>.zip` (downloads `platformVersion` from gradle.properties).
- Against an installed IDE (needed for nightlies, which are not in the artifact repository):
  `./gradlew buildPlugin -PplatformLocalPath="/path/to/IDE.app/Contents"`.
- `./gradlew runIde` to launch a sandbox IDE; `./gradlew check` for tests.
- LSP4IJ dependency: `build.gradle.kts` fetches the latest *nightly* LSP4IJ from the Marketplace unless
  `../lsp4ij/build/idea-sandbox/plugins/LSP4IJ` exists.

## Platform compatibility gotchas
- JCEF: since 2026.x `com.intellij.ui.jcef.*` lives in the bundled plugin `com.intellij.modules.jcef`; the optional
  `<depends>` in plugin.xml is what makes those classes visible. Never remove it.
- `sinceBuild = 242`, no `untilBuild`. Keep code compatible with 2024.2 APIs or guard new APIs.
- Editor markup: LSP ranges from the Lean server can be empty/inverted after clamping; validate offsets before
  calling `addRangeHighlighter`.
- lsp4j differs by IDE/LSP4IJ version (0.2x vs 1.0.0). Access changed members through `lean4ij.util.Lsp4jCompat`
  (reflection) instead of calling them directly; `Diagnostic.getMessage()` is the known case.
- `Editor.getVirtualFile()` is `@Nullable` on new platforms; always use a safe call.

## Conventions
- Commit messages and code comments in English.
- When fixing a crash seen in an IDE log, record the stack trace + root cause in `notes/` and link the upstream issue if any.
- Every "fix this" request ends with a commit and a rebuilt plugin zip, unless told otherwise. If the change
  consists of several independent fixes, make one commit per logical fix (notes/CLAUDE.md updates go together
  with the fix they describe, or in a separate commit when standalone). Each commit that changes user-visible
  behaviour also adds a short entry to the `[Unreleased]` block of `CHANGELOG.md` (what changed for the user,
  not the implementation details; `### Added` / `### Fixed` / `### Build` subsections). Then run the `buildPlugin`
  command above (against the installed IDE when targeting a nightly) and report the path to
  `build/distributions/*.zip`.
