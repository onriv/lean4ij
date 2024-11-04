# Lean4ij

![Build](https://github.com/onriv/lean4ij/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/25104.svg)](https://plugins.jetbrains.com/plugin/25104)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/25104.svg)](https://plugins.jetbrains.com/plugin/25104)

![image](https://github.com/user-attachments/assets/4cca8ddd-f336-4f8c-b5f4-b16b9e725675)
(The screenshot is taken from [mathematics_in_lean](https://github.com/leanprover-community/mathematics_in_lean))
and some video:

https://github.com/user-attachments/assets/25757345-0249-4999-adc7-7dadf94c8b0e

<!-- Plugin description -->
A [Lean4](https://lean-lang.org/) plugin for the Intellij Platform.

# Installation

This plugin uses [LSP4IJ](https://github.com/redhat-developer/lsp4ij) for connecting to the Lean4 language server. Please install that first.

- Using the IDE built-in plugin system: `Settings/Preferences` > `Plugins` > `Marketplace` > `Search for "lean4ij"` >
  `Install`

- Manually: Download the [latest release](https://github.com/onriv/lean4ij/releases/latest) and install it manually using
  `Settings/Preferences` > `Plugins` > `⚙️` > `Install plugin from disk...` For nightly builds go to [Actions/build](https://github.com/onriv/lean4ij/actions/workflows/build.yml), click the latest success run and scroll to the buttom.

The plugin should be compatible from version 2024.1 and can not support the earlier versions for depending some experimental api)

## Usage

For currently there is no functionality of creating a project or setting up a project. Before open any lean project with it please first testing if the project has set up the toolchain correctly. Run any command like `elan which lake` or `lake exe cache get`, or `lake build` etc.

The LSP server is start as any lean file is open in the Editor and the editor gets focus. If it not behaves correctly, try firing a restart action.

Unicode is supported via live templates, for example typing `\b1<SPACE>` would result in `𝟙`. For the limitation of live templates, the `<SPACE>` keypress is always required.

Infoview is supported using [lean4-infoview,](https://github.com/leanprover/vscode-lean4/tree/master/lean4-infoview) and currently it can be started from a browser or the internal [JCEF]() infoview toolwindow. If it not behaves correctly, try firing a restart action too. There is also an infoview implemented in swing that's native
in Jetbrains platform, it contains some basic functionally and for popup it requires a click.

Messages and logs about the lean lsp server can be found in the language server tool window after setting the level to message or trace, check more information about this in [redhat-developer/lsp4ij](https://github.com/redhat-developer/lsp4ij).
### Actions

Currently, the following actions are defined, mostly without default shortcut. Add one for them in `Keymap` (like `Control Shift Enter` for toggle infoview)

| action id                            | action text                                                 | default shortcut |
|--------------------------------------|-------------------------------------------------------------|------------------|
| OpenLeanInfoViewInternal             | Lean4 Actions: Toggle Infoview (internal)                   |                  |  
| OpenLeanInfoViewJcef                 | Lean4 Actions: Toggle Infoview (jcef)                       |                  |  
| IncreaseZoomLevelForLeanInfoViewJcef | Lean4 Actions: Increase zoom level for lean infoview (jcef) |                  |  
| DecreaseZoomLevelForLeanInfoViewJcef | Lean4 Actions: Decrease zoom level for lean infoview (jcef) |                  |  
| ResetZoomLevelForLeanInfoViewJcef    | Lean4 Actions: Reset zoom level for lean infoview (jcef)    |                  |  
| OpenExternalInfoviewInBrowser        | Lean4 Actions: Open infoview in browser                     |                  |  
| RestartLeanLsp                       | Lean4 Actions: Restart Lean Lsp Server                      |                  |  
| RestartCurrentLeanFile               | Lean4 Actions: Restart Current Lean File                    |                  |  
| RestartJcefInfoview                  | Lean4 Actions: Restart Jcef Infoview                        |                  |  
| AddInlayGoalHint                     | Lean4 Actions: Add Inlay Goal Hint                          | Control I        |  
| DelInlayGoalHint                     | Lean4 Actions: Delete Inlay Goal Hint                       | Control Shift I  |  

## Settings

Since version 0.0.17 there are some settings available:
- General setting is under `Settings/Preferences` > `Leanguages & Frameworks` > `Lean4`. Available settings are:
  - (TODO) Enable Lsp Completion: Currently not support, waiting lsp4ij's new release. This is for currently discovering that sometimes lsp completion is slow. But it's enable by default.
- Enable the native infoview, and timeout for popping the doc
- Enable the external infoview
- Extra css for external infoview. The most relevant I found is changing font-size

The inlay hints related settings are under `Settings/Preferences` > `Inlay Hints` > `textmate`:
- `Show inlay hint for omit type`
- `Show value for placeholder _`

Some color settings are under `Settings/Preferences` > `Editor` > `Color Scheme` >  `Lean Infoview`. It contains color settings for both the external and internal infoview.

<!-- Plugin description end -->

## Development

Please check [DEVELOP.md](./DEVELOP.md).

## Known Issues

The plugin is still on an early stage, check [ISSUES.md](./ISSUES.md) for known and logged issues, and [TODO.md](./TODO.md)

## Troubleshooting
- Currently, the plugin seems capable to open the same project with vscode in the same time (Although it may consume twice the cpu and memory resources). Try open the project simultaneously in VSC and JB-IDE while troubleshooting.
- Currently, some log is printed in the build window for the progressing file and the url to the external/jcef infoview, if something does not work normally, some log there may help.
- There are also detailed logs for the lsp server supported by LSP4IJ via the "language servers" tool window after setting the debug/trace level to verbose.
- Some logs are also sent in the standard log file like `idea.log`. For different systems the path of it's the following paths, it can also be opened via `Help/Show log in ...` in the menu.
  - (Linux) `$HOME.cache/JetBrains/<Product>/log/idea.log`
  - (Windows) `$HOME\AppData\Local\JetBrains\<Product>\log\idea.log`
  - (Macos) `~/Library/Caches/<Product>/log/idea.log`
- If the IDE is freezing, try check also the `threadDumps-freeze-***` files under the log folder.

For showing debug/trace log, add `lean4ij:all` in `MENU > Help > Diagnostic Tools > Debug Log Settings` and restart, see [How-to-enable-debug-logging-in-IntelliJ-IDEA](https://youtrack.jetbrains.com/articles/SUPPORT-A-43/How-to-enable-debug-logging-in-IntelliJ-IDEA) for more docs.
 
## Acknowledgments

The following projects give great help for developing the plugin:

- [leanprover/vscode-lean4](https://github.com/leanprover/vscode-lean4)
- [leanprover-community/lean4web](https://github.com/leanprover-community/lean4web)
- [Julian/lean.nvim](https://github.com/Julian/lean.nvim)
- [leanprover-community/lean4-mode](https://github.com/leanprover-community/lean4-mode)
- [redhat-developer/lsp4ij](https://github.com/redhat-developer/lsp4ij)

and many source codes with references to

- [intellij-arend](https://github.com/JetBrains/intellij-arend)
- [intellij-haskell](https://github.com/rikvdkleij/intellij-haskell.git)
- [julia-intellij](https://github.com/JuliaEditorSupport/julia-intellij)
- [intellij-quarkus](https://github.com/redhat-developer/intellij-quarkus/)
- [intellij-rust](https://github.com/intellij-rust/intellij-rust.git)
- [intellij-sdk-code-samples](https://github.com/JetBrains/intellij-sdk-code-samples)

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
