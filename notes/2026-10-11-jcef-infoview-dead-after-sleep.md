# JCEF infoview stops updating after the laptop slept (2026-10-11)

## Symptom

After the MacBook spent a few hours with the lid closed (IDE session started 15:20, idle from ~15:43 until
~22:35), the `LeanInfoviewJcef` tool window keeps showing the last goal and no longer reacts to caret moves or
edits. "Open external infoview in browser" opens a fresh tab against the same ktor server and that tab works, so
the server side (LSP, `MutableSharedFlow` pipeline, RPC) is fine; only the page living inside JCEF is dead.

## Investigation

* `idea.log` has nothing from JCEF or lean4ij for the sleep period. The only related entries are two
  `ClosedReceiveChannelException: Channel was closed` reports from `Route.kt` (15:21:14 and 22:36:43), each
  followed by a `Control-flow exceptions ... should never be logged` complaint because `Route.kt:182` passed the
  resulting `JobCancellationException` to `Logger.error`. Both are websocket clients going away, reported as
  SEVERE (so they also show up as IDE "internal error" balloons).
* Live check in the running IDE (pid 26588) with `lsof`: the JCEF processes (`cef_server` 27049 and its helpers)
  hold only their own IPC connections (ports 30002/30003); there is **no** connection from any process to the
  lean4ij ktor port (57202). The page in the tool window still renders its last React state, but its websocket is
  gone.
* `browser-infoview/src/App.tsx`: `WebSocketClient` opened the socket once in the constructor and registered only
  a `message` listener. No `close`/`error` handling, no reconnect. Once Chromium drops the connection (which it does
  on wake-up / network change inside the CEF network service) every `sendMessage` goes to a closed socket, the
  promise never settles, and the infoview shows stale content forever. A browser tab is a brand-new page with a new
  socket, hence "works in the browser".
* Server side there was no `pingPeriod` on the ktor `WebSockets` plugin, so a half-open session would also never be
  detected, and each websocket session subscribed an `EditorColorsListener` on a fresh `messageBus.connect()`
  that was never disconnected.

## Fix

* `App.tsx`: `WebSocketClient` reconnects with exponential backoff (0.5s → 10s) on `close`. Requests that were in
  flight, and requests made while disconnected, are rejected with `RpcNeedsReconnect` (`-32900`), which the
  infoview treats as "recreate the RPC session and retry" instead of an error. On every (re)connection the server
  already replays `updateTheme`, `serverRestarted` (→ `closeAllSessions()` in the infoview) and the last cursor
  location, so the page re-renders the current goal without a reload.
* `Route.kt`: client disconnects (`ClosedReceiveChannelException`, cancellation) are logged at INFO with the
  close reason instead of SEVERE + stack traces; `CancellationException` is rethrown, not logged. The message bus
  connection is disconnected in `finally`, and a failing theme push does not escape the IO scope.
* `ExternalInfoViewService.kt`: `pingPeriod = 15s`, `timeout = 30s` on the `WebSockets` plugin so a dead session
  is closed server-side too (and its event collector stops).

## How to verify

Put the machine to sleep with the tool window open (or kill the websocket from the JCEF devtools), wake up, move
the caret: the goal updates again within a second. `idea.log` shows `infoview websocket disconnected` /
`infoview websocket connected` at INFO and no SEVERE entries.
