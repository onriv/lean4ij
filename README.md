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

This plugin uses [LSP4IJ](https://github.com/redhat-developer/lsp4ij) for connecting to the Lean4 lsp server. Please install that first.

- Using the IDE built-in plugin system: `Settings/Preferences` > `Plugins` > `Marketplace` > `Search for "lean4ij"` >
  `Install`

- Manually: Download the [latest release](https://github.com/onriv/lean4ij/releases/latest) and install it manually using
  `Settings/Preferences` > `Plugins` > `⚙️` > `Install plugin from disk...` For nightly builds go to [Actions/build](https://github.com/onriv/lean4ij/actions/workflows/build.yml) and download from the buttom of eachsuccess run.

The plugin should be compatible from version 2024.1 and can not support the earlier versions for depending on textmate plugin's extension api.
## Usage

For currently there is no functionality of creating a project or setting up a project. Before open any lean project with it please first testing if the project has set up the toolchain correctly. Run any command like `elan which lake` or `lake exe cache get`, etc.

The LSP server is start as any lean file is open in the Editor. If it not behaves correctly, try firing a restart action.

Unicode is supported via live templates, for example typing `\b1<SPACE>` would result in `𝟙`. For the limitation of live templates, the `<SPACE>` keypress is always required.

Infoview is supported using [lean4-infoview,](https://github.com/leanprover/vscode-lean4/tree/master/lean4-infoview) and currently it can be started from a browser or the internal [JCEF] infoview toolwindow. If it not behaves correctly, try firing a restart action too. There is also an infoview implemented in swing that's native
in Jetbrains platform, it contains some basic functionally and for popup it requires a click.

Messages and logs about the lean lsp server can be found in the language server tool window after setting the level to message or trace, check more information about this in [redhat-developer/lsp4ij](https://github.com/redhat-developer/lsp4ij).
### Actions

| action id              | action text                       | meaning                     |
|------------------------|-----------------------------------|-----------------------------|
| OpenLeanInfoView       | Lean4 : Lean open info view       | open the infoview(swing)    |
| RestartLeanLsp         | Lean4 : Restart Lean Lsp Server   | restart the  lsp server     |
| RestartCurrentLeanFile | Lean4 : Restart Current Lean File | restart current file        |
| RestartJcefInfoview    | Lean4 : Restart Jcef Infoview     | restart the jcef infoview   |

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
 
## Acknowledgments

The following projects give great help for developing the plugin:

- [leanprover/vscode-lean4](https://github.com/leanprover/vscode-lean4)
- [leanprover-community/lean4web](https://github.com/leanprover-community/lean4web)
- [Julian/lean.nvim](https://github.com/Julian/lean.nvim)
- [leanprover-community/lean4-mode](https://github.com/leanprover-community/lean4-mode)
- [redhat-developer/lsp4ij](https://github.com/redhat-developer/lsp4ij)

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
