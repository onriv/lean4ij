# "no build for externalInfoViewService" fatal error on start-up (2026-10-10)

## Symptom

Right after the plugin starts, the IDE shows a fatal error:

```
java.lang.Throwable: no build for externalInfoViewService
    at com.intellij.openapi.diagnostic.Logger.error(Logger.java:377)
    at lean4ij.project.BuildWindowService$1$1.emit(BuildWindowService.kt:78)
    at lean4ij.project.BuildWindowService$1$1.emit(BuildWindowService.kt:55)
    at kotlinx.coroutines.flow.SharedFlowImpl.collect$suspendImpl(SharedFlow.kt:397)
    ...
```

## Root cause

`BuildWindowService` pushed `BuildStart` / `BuildMessage` / `BuildEnd` events through a
`MutableSharedFlow<BuildEvent>()` with no replay and no buffer, and every `startBuild` / `addBuildEvent` /
`endBuild` call launched its own coroutine on `LeanProjectService.scope` to `emit`.

Two consequences:

1. A `MutableSharedFlow` with no subscribers drops emissions silently. The collector is started from the
   service `init` via `launch`, while `ExternalInfoViewService.startServer()` calls `startBuild("externalInfoViewService")`
   as soon as the service is created, so the `BuildStart` could be emitted before the collector subscribed and was lost.
2. Three independently launched coroutines have no ordering guarantee, so `BuildEnd` could be delivered before
   `BuildStart` or `BuildMessage`.

Either way the collector saw a `BuildEnd` for a file it had never started and reported it through
`Logger.error`, which the IDE presents as a fatal error.

## Fix

- Replaced the shared flow with `Channel<BuildEvent>(Channel.UNLIMITED)`: events are posted with `trySend`
  directly from the caller's thread (preserving the caller's order) and are buffered until the collector consumes them.
- The collector is a single `for (s in events)` loop, so the `Mutex` is no longer needed.
- "no build for <file>" is now a `warn`: a `BuildEnd` without a start can still legitimately come from the
  language server and must not raise a fatal-error dialog.
