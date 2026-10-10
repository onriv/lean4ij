# JCEF infoview goes blank after an edit and never recovers (2026-10-10)

## Symptom

Cursor after `_` in `theorem t3 : p → ¬p → q := _` (last line of `MyTest/Basic.lean`), the JCEF infoview shows
the term goal. Deleting the `_` makes the JCEF infoview empty, and it stays empty: putting `_` back or moving the
caret anywhere else shows nothing. The internal (Swing) infoview is not affected. `idea.log` has **no** error from
lean4ij for the whole session.

## Investigation

* Lean itself behaves (checked with a stdio LSP script against `lake serve` in `~/LeanProjects/my_test`,
  `v4.34.1`): `getInteractiveTermGoal` answers before/after the edit, no error codes, no hangs.
* The plugin's websocket server in the running IDE also answers (checked with a hand-written websocket client
  connected to the ktor port): `createRpcSession`, `getInteractiveTermGoal`, `getWidgets` all return, and
  server notifications keep flowing. So neither the RPC path nor the `MutableSharedFlow` pipeline was stuck.
* The payload of the forwarded `textDocument/publishDiagnostics` is what breaks the frontend:

  ```json
  {"range": {...}, "severity": "Error", "source": "Lean 4",
   "message": {"left": "don't know how to synthesize placeholder\ncontext:\n..."}}
  ```

  `Route.kt` serialized every event with a plain `Gson()`. With lsp4j 1.0 (2026.x IDEs / LSP4IJ 0.21+)
  `Diagnostic.message` is an `Either<String, MarkupContent>` and the reflective serializer emits it as
  `{"left": ...}`; enums come out by name (`"Error"` instead of `1`). On 2024.x/2025.x (lsp4j 0.2x) `message`
  was a plain `String`, which is why this never showed up before the migration.

## Why the edit triggers it

`InfoAux` in `@leanprover/infoview` requests `getInteractiveDiagnostics` for the cursor line and, when that
returns an empty list, falls back to the LSP diagnostics at the cursor (`lspDiagToInteractive`:
`{...d, message: {text: d.message}}`). Right after deleting `_` Lean returns `[]` for the interactive
diagnostics (the parse error "unexpected end of input" only appears after re-elaboration), while
`publishDiagnostics` already has a diagnostic at the cursor. The fallback kicks in, `text` is the
`{"left": ...}` object, React throws "Objects are not valid as a React child", and because the infoview has no
error boundary the whole React root unmounts. Later events still arrive over the websocket, but nothing renders
them any more, hence "never recovers" until the page is reloaded (Restart JCEF infoview / IDE restart).

With `_` present the interactive diagnostics are non-empty, so the fallback (and the crash) never happened.

## Fix

* `Route.kt`: serialize everything sent over the websocket with lsp4j's own Gson
  (`MessageJsonHandler(emptyMap()).gson`), which carries the `Either`/enum/tuple adapters and produces wire-format
  JSON (`"message": "..."`, `"severity": 1`). `MessageJsonHandler(Map)` + `getGson()` exist in both lsp4j 0.2x and
  1.0 (`getDefaultGsonBuilder()` does not: static in 0.2x, instance method in 1.0).
* `Route.kt` + `App.tsx`: every request from the browser is now answered. On failure the response carries
  `error: {code, message}` and the frontend rejects the promise instead of leaving it pending forever (previously a
  failing `sendClientRequest` was only logged and the infoview stayed on "Updating..." for that position). Lean
  error codes are passed through so the infoview's own retry logic (`ContentModified`, `RpcNeedsReconnect`)
  works.

## Follow-ups (not done)

* `LeanLanguageServerLifecycleListener` forwards *client* notifications too (`textDocument/didChange`,
  `$/lean/rpc/keepAlive`, `$/cancelRequest`...) as `serverNotification` events. The infoview ignores them, but
  they inflate `ExternalInfoViewService.notificationMessages`, which is replayed to every new connection and
  grows without bound.
* `rpcCallWithRetry` still maps "elaboration interrupted" and similar errors to `null` instead of letting the
  infoview retry.
