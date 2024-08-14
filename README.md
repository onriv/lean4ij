# Lean4ij

![Build](https://github.com/onriv/lean4ij/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

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
- [ ] file progressing seems block UI thread in some cases
- [ ] skip index `.lake/build`
- [ ] infoview toolwindow in jcef
- [ ] infoview toolwindow in swing

## Acknowledgments

The following projects give great help for developing the plugin:

- [leanprover/vscode-lean4](https://github.com/leanprover/vscode-lean4/tree/master/vscode-lean4)
- [Julian/lean.nvim](https://github.com/Julian/lean.nvim)
- [leanprover-community/lean4-mode](https://github.com/leanprover-community/lean4-mode)
- [redhat-developer/lsp4ij](https://github.com/redhat-developer/lsp4ij)
---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
