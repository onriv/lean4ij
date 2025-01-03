<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# lean4ij Changelog

## [Unreleased]

## [0.1.10] - 2024-12-22

Depend on the latest approved nightly build of LSP4IJ: 0.9.1-20241218-185137

- fix the wrongly implemented getOptions for [LeanRunConfiguration.getOptions]
- remove a dependent log in CHANGELOG.md in build.gradle.kts
- add suffix triggering or debounce triggering strategies for "goto workspace symbols/classes" in settings
- make "open in find tool window" of "goto workspace symbols/classes" work
- goto definition for internal infoview
- add search for external infoview
- search button also for internal infoview
- support arguments in run configuration
- support lake run configuration

## [0.1.9] - 2024-12-16

Depend on the latest approved nightly build of LSP4IJ: 0.9.0-20241214-112757
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.9.0-20241214-112757@nightly]

- a frist impemlentation for run lean file
- run configuration for lean (beta)

## [0.1.9-beta.2] - 2024-12-16

Depend on the latest approved nightly build of LSP4IJ: 0.9.0-20241214-112757
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.9.0-20241214-112757@nightly]

- run configuration for lean (beta)

## [0.1.8] - 2024-12-09

Depend on the latest approved nightly build of LSP4IJ: 0.8.2-20241208-013232
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.8.2-20241208-013232@nightly]

- make `goto declaration` work by keeping LSP enable even not focusing on  an editor
- fix [\#77](https://github.com/onriv/lean4ij/issues/77)(while restarting, the focus lean file may not updateCaret)
- some code for running a lean file is encluded but not working yet

## [0.1.7] - 2024-12-02

Depend on the latest approved nightly build of LSP4IJ: 0.8.1-20241202-013252
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.8.1-20241202-013252@nightly]

- refactoring the internal infoview, and
- add a first implementation for trace message in internal infoview
- add a first implementation for goto definition for vscode-adapted infoview
- fix build window messages have no linebreak

## [0.1.6] - 2024-11-25

Depend on the latest approved nightly build of LSP4IJ: 0.8.0-20241124-230741
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.8.0-20241124-230741@nightly]

- add a first try for goto symbol/class implementation
  - for some performance tuning currently debouncing gap set to 1 second
  - a setting for the debouncing gap
- some refactor on settings
- setting for starting the language server eagerly or not
- setting for file progressing all open editors eagerly or not (requires lsp4ij 0.8.0)

## [0.1.5] - 2024-11-17

Depend on the latest approved nightly build of LSP4IJ: 0.8.0-20241115-145814
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.8.0-20241115-145814@nightly]

- add sdk and library support (if searching of file not working, try `invalidate cache` and remove `.idea` directory)
- add a soft wrap action/toolbar button for internal infoview
- rename actions for toggling infoviews (require rebind shortcuts)
- impl preferred infoview
  - add a setting for it
  - switch button of main toolbar to it
- fix external infoview trace message style
- add style for removed/inserted hyp in internal infoview
- support space/empty-line/first-column setting for the commenter

## [0.1.4] - 2024-11-11

Depend on the latest approved nightly build of LSP4IJ: 0.8.0-20241105-013235
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.8.0-20241105-013235@nightly]

- fix internal infoview document not popping up

## [0.1.3] - 2024-11-10

Depend on the latest approved nightly build of LSP4IJ: 0.8.0-20241105-013235
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.8.0-20241105-013235@nightly]

- fix some style in the external infoview
- add a button in main toolbar for toggling infoview toolwindow 
- some tuning for the basic syntax highlight
- some refactor for the setting page implementation
- remove extra css setting in the setting page for currently messing up with theme
- basic toolwindow toolbar for infoview
- switch to a new icon ([ref](https://intellij-icons.jetbrains.design/))
- fix internal infoview hyperlink click
- fix internal infoview wrong hover
- tuning default parameter for better popup document in internal infoview

## [0.1.2] - 2024-11-03

Depend on the latest approved nightly build of LSP4IJ: 0.7.1-20241101-225116
Depends on platformPlugins: [com.redhat.devtools.lsp4ij:0.7.1-20241101-225116@nightly]

- remove textmate dep
- a first highlight with customized lexer/parser
- skip inlay hints collecting if disabled in the setting
- a setting for max inlay hint wait time
- a setting for disable language server globally
- skip inlay hints if disabled the language server globally

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

[Unreleased]: https://github.com/onriv/lean4ij/compare/v0.1.10...HEAD
[0.1.10]: https://github.com/onriv/lean4ij/compare/v0.1.9...v0.1.10
[0.1.9]: https://github.com/onriv/lean4ij/compare/v0.1.9-beta.2...v0.1.9
[0.1.9-beta.2]: https://github.com/onriv/lean4ij/compare/v0.1.8...v0.1.9-beta.2
[0.1.8]: https://github.com/onriv/lean4ij/compare/v0.1.7...v0.1.8
[0.1.7]: https://github.com/onriv/lean4ij/compare/v0.1.6...v0.1.7
[0.1.6]: https://github.com/onriv/lean4ij/compare/v0.1.5...v0.1.6
[0.1.5]: https://github.com/onriv/lean4ij/compare/v0.1.4...v0.1.5
[0.1.4]: https://github.com/onriv/lean4ij/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/onriv/lean4ij/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/onriv/lean4ij/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/onriv/lean4ij/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/onriv/lean4ij/compare/v0.0.19...v0.1.0
[0.0.19]: https://github.com/onriv/lean4ij/compare/v0.0.18...v0.0.19
[0.0.18]: https://github.com/onriv/lean4ij/compare/v0.0.17...v0.0.18
[0.0.17]: https://github.com/onriv/lean4ij/commits/v0.0.17
