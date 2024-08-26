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

- (Unsupported yet) ~~Using the IDE built-in plugin system:~~

  `Settings/Preferences` > `Plugins` > `Marketplace` > `Search for "lean4ij"` >
  `Install`

- Manually:

  Download the [latest release](https://github.com/onriv/lean4ij/releases/latest) and install it manually using
  `Settings/Preferences` > `Plugins` > `⚙️` > `Install plugin from disk...`

The plugin should be compatible from version 2024.1 and can not support the earlier versions for depending on textmate plugin's extension api.
## Usage

For currently there is no functionality of creating a project or setting up a project. Before open any lean project with it please first testing if the project has set up the toolchain correctly. Run any command like `elan which lake` or `lake exe cache get`, etc.

The LSP server is start as any lean file is open in the Editor. If it not behaves correctly, try firing a restart action.

Unicode is supported via live templates, for example typing `\b1<SPACE>` would result in `𝟙`. For the limitation of live templates, the `<SPACE>` keypress is always required.

Infoview is supported using [lean4-infoview,](https://github.com/leanprover/vscode-lean4/tree/master/lean4-infoview) and currently it can be started from a browser or the internal [JCEF] infoview toolwindow.

Messages and logs about the lean lsp server can be found in the language server tool window after setting the level to message or trace, check more information about this in [redhat-developer/lsp4ij](https://github.com/redhat-developer/lsp4ij).
### Actions

| action id              | action text               | meaning                     |
|------------------------|---------------------------|-----------------------------|
| OpenLeanInfoView       | Lean open info view       | open the infoview(swing)    |
| RestartLeanLsp         | Restart Lean Lsp Server   | restart the  lsp server     |
| RestartCurrentLeanFile | Restart Current Lean File | restart current file        |
| DumpCoroutine          | Dump Coroutine            | dump coroutine for debug(*) |
| ReloadJcefInfoview     | Reload Jcef Infoview      | reload the jcef infoview    |

currently DumpCoroutine hard code outputs to `D:\dumpCoroutines.txt`, `D:\dumpCoroutinesInfo.txt`  and `D:\dumpCoroutinesInfoScopre.txt`, this action should not be used usually.
<!-- Plugin description end -->

## Development

Please check [DEVELOP.md](./DEVELOP.md).

## Known Issues

The plugin is still on a very early stage, check [ISSUES.md](./ISSUES.md) for known and logged issues.

and todos
- [x] file progressing seems block UI thread in some cases 
  - solved by highlight on ranges rather than lines
- [x] skip index `.lake/build`
  - [x] this can be manually done by right-clicking the folder and marking it as exclude
  - [x] automatically exclude, check [this](https://youtrack.jetbrains.com/issue/IDEA-194725/Specify-IntelliJ-exclude-directories-in-build.gradle), or [this](https://youtrack.jetbrains.com/issue/IJPL-8363/Ability-to-have-default-Excluded-Folders-not-per-project), or [this](https://youtrack.jetbrains.com/issue/WEB-11419).
    some plugins have customized logic for it like intellij-rust or intellij-arend
- [ ] infoview toolwindow in swing
  - [x] show goals
  - [x] show term goal
  - [ ] show message
  - [x] popup
  - [ ] pop up style, fonts, clickable links, etc
  - [ ] color
  - [x] make the editor singleton  
- [ ] mathlib4 seems always failed starting the language server
  this is because elan download lake while starting lsp, not fixed yet
- [x] infoview toolwindow in jcef
- [ ] project create/setup or configuration
- [ ] distinguish source in .lake as library rather than source
- [x] avoid file progressing in search window (it should be caused by didOpen, etc.) solved by only enable lsp at focusing editor
- [ ] setting dialog
- [x] theme and color
- [x] find in files will send a didOpen request and make fileProgress, it may hurt the performance.
  currently a fix for this is disabling lsp while lost focus for the editor
- [ ] elan/lake, project create, setup etc
- [ ] run and build (debug cannot be supported, although arend has this)
- [ ] some more logs with different levels
- [ ] refactor the frontend impl (currently it's written as for feasibility test)
- [x] all messages in the external infoview failed (via caching server notification now)
- [ ] check why sometimes lsp requires multiple start
## Acknowledgments

The following projects give great help for developing the plugin:

- [leanprover/vscode-lean4](https://github.com/leanprover/vscode-lean4/tree/master/vscode-lean4)
- [leanprover-community/lean4web](https://github.com/leanprover-community/lean4web)
- [Julian/lean.nvim](https://github.com/Julian/lean.nvim)
- [leanprover-community/lean4-mode](https://github.com/leanprover-community/lean4-mode)
- [redhat-developer/lsp4ij](https://github.com/redhat-developer/lsp4ij)
---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
