# Lean4ij

![Build](https://github.com/onriv/lean4ij/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

![image](https://github.com/user-attachments/assets/fde43071-29e9-4f62-a8ea-d18e433aa780)

<!-- Plugin description -->
A [Lean4](https://lean-lang.org/) plugin for the Intellij Platform.

# Installation

This plugin uses [Lsp4ij](https://github.com/redhat-developer/lsp4ij) for connecting to the Lean4 lsp server. Please install this first.

- (Unsupported yet) ~~Using the IDE built-in plugin system:~~

  `Settings/Preferences` > `Plugins` > `Marketplace` > `Search for "lean4ij"` >
  `Install`

- Manually:

  Download the [latest release](https://github.com/onriv/lean4ij/releases/latest) and install it manually using
  `Settings/Preferences` > `Plugins` > `⚙️` > `Install plugin from disk...`

The plugin should be compatible from version 2024.1 and can not support the earlier versions for depending on textmate plugin's extension api.
## Usage

After opening a lean4 project, the language server toolwindow would automatically start the language server.

Unicode is supported via live templates, for example typing `\b1<SPACE>` would result in `𝟙`. For the limitation of live templates, the `<SPACE>` keypress is always required.

Infoview is supported using [lean4-infoview](https://github.com/leanprover/vscode-lean4/tree/master/lean4-infoview) and currently it requires opening from a web browser

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
- [x] infoview toolwindow in jcef
- [ ] mathlib4 seems always failed starting the language server
- [ ] infoview toolwindow in swing
- [ ] project create/setup or configuration
- [ ] distinguish source in .lake as library rather than source
- [ ] avoid file progressing in search window (it should be caused by didOpen, etc.)
- [ ] setting dialog
- [ ] theme and color
- [x] find in files will send a didOpen request and make fileProgress, it may hurt the performance.
  currently a fix for this is disabling lsp while lost focus for the editor
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
