# Issue #226: optional JCEF fallback

Issue: https://github.com/onriv/lean4ij/issues/226

The reporter uses IntelliJ IDEA 2026.2.3 and lean4ij 0.2.8. Startup fails with:

```text
com.intellij.diagnostic.PluginException: com/intellij/ui/jcef/JBCefApp
Caused by: java.lang.ClassNotFoundException: com.intellij.ui.jcef.JBCefApp
```

JCEF moved to a bundled plugin in 2026.2. Current main already declares the optional
`com.intellij.modules.jcef` dependency. Startup still unconditionally obtains the
JCEF service, and selecting Swing only changes the preferred-window action.

Probe support through the plugin classloader before referencing the JCEF service.
Missing platform/runtime classes and linkage errors make JCEF unavailable; cancellation
and unexpected failures propagate. Guard toolwindow creation and action presentation
and execution, and fall back to Swing without changing the saved preference.

Keep the HTTP/WebSocket server and its URL independent of the embedded browser.
Standalone-browser access and ordinary theme generation remain available; skip JCEF
scrollbar CSS when unsupported. Showing Swing directly through ToolWindowManager also
allows its first opening before content exists.

Seven helper tests cover support, absence, linkage failures, and exception propagation.
The original development-tree run passed all ten selected helper, infoview DSL, and Gson
tests. Runtime verification on IDEA 2026.2 with JCEF disabled is still required.
