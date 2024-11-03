<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# lean4ij Changelog

## [Unreleased]
- remove textmate dep, backport to version 2023.2
- a first highlight with customized lexer/parser
- skip inlay hints collecting if disabled in the setting
- a setting for max inlay hint wait time
- a setting for disable language server globally

## [0.1.1] - 2024-10-30

Depend on the latest approved nightly build of LSP4IJ: 0.7.1-20241027-013436

- setting for disable progress bar on the left of editor while file progressing
- main reason for the release: disable LEAN_SERVER_LOG (and a setting for enable it) Please read [issue#45](https://github.com/onriv/lean4ij/issues/45) for removing existing logs
- disable placeholder inlay hints by default
- fix some errors and exceptions
- @enigmurl fix some bad cases in inlay hints

## [0.1.0] - 2024-10-27

Depend on the latest approved nightly build of LSP4IJ: 0.7.1-20241027-013436
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.7.1-20241027-013436@nightly, org.jetbrains.plugins.textmate]

- register file type "lean4", dummy lexer/parser (marked as 0.1.0 for this change)
- temp icons (with the author's poor design skill) for toolwindow and file 
- restricting scope for live templates to lean4
- add commenter
- actions for toggle infoview and open infoview in browser
- @enigmurl fixes wrongly lsp/inlay-hints for non-lean files
- respect `#check` etc. inlay-hints to current scheme
- actions for increase/decrease/reset zoom level for jcef infoview

## [0.0.19] - 2024-10-21

Depend on the latest approved nightly build of LSP4IJ: 0.7.1-20241017-013236
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.7.1-20241017-013236@nightly, org.jetbrains.plugins.textmate]

- update some dependencies
- change imports of infoview-app to [dynamic loading](https://github.com/leanprover/vscode-lean4/tree/master/lean4-infoview#loading-the-infoview), hence
- adapt external infoview [widget](https://lean-lang.org/lean4/doc/examples/widgets.lean.html)
- adapt apply edit like `simp?`
- add support for goal hints in regular (i.e. non tactic) mode
- add diagnostic hints
- allow for collapsing of all inlay hints

## [0.0.18] - 2024-10-16

Depend on the latest approved nightly build of LSP4IJ: 0.7.1-20241016-013210
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.7.1-20241016-013210@nightly, org.jetbrains.plugins.textmate]

- fix caret of internal infoview at first line first col while refreshing
- tuning size for internal infoview popup window
- remove click in internal infoview, some optimization for popup up doc
- goal hints with @enigmurl's great efforts
- bump with LSP4IJ to 0.7.0
- A setting for enable/disable lsp completion (for faster simple word completion)

## [0.0.17] - 2024-10-06

Depend on the latest approved nightly build of LSP4IJ: 0.7.0-20241006-013203
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.7.0-20241006-013203@nightly, org.jetbrains.plugins.textmate]

- More content on the internal infoview
- Fix [issue#15](https://github.com/onriv/lean4ij/issues/15)
- Some inlay hints with [@enigmurl](https://github.com/enigmurl)'s great efforts!
- Some more snippets involving the cursor
- A setting page

[Unreleased]: https://github.com/onriv/lean4ij/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/onriv/lean4ij/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/onriv/lean4ij/compare/v0.0.19...v0.1.0
[0.0.19]: https://github.com/onriv/lean4ij/compare/v0.0.18...v0.0.19
[0.0.18]: https://github.com/onriv/lean4ij/compare/v0.0.17...v0.0.18
[0.0.17]: https://github.com/onriv/lean4ij/commits/v0.0.17
