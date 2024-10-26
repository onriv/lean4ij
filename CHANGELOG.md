<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# lean4ij Changelog

## [Unreleased]
- register file type "lean4"
- restricting scope for live templates to lean4
- add commenter (line commenter work but block comment/uncomment maybe problematic)
- actions for toggle infoview and open infoview in browser
- fix wrongly lsp/inlay-hints for non-lean files
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

[Unreleased]: https://github.com/onriv/lean4ij/compare/v0.0.19...HEAD
[0.0.19]: https://github.com/onriv/lean4ij/compare/v0.0.18...v0.0.19
[0.0.18]: https://github.com/onriv/lean4ij/compare/v0.0.17...v0.0.18
[0.0.17]: https://github.com/onriv/lean4ij/commits/v0.0.17
