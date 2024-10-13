
# TODO

- [x] file progressing seems block UI thread in some cases 
  - solved by highlight on ranges rather than lines
- [x] skip index `.lake/build`
  - [x] this can be manually done by right-clicking the folder and marking it as exclude
  - [x] automatically exclude, check [this](https://youtrack.jetbrains.com/issue/IDEA-194725/Specify-IntelliJ-exclude-directories-in-build.gradle), or [this](https://youtrack.jetbrains.com/issue/IJPL-8363/Ability-to-have-default-Excluded-Folders-not-per-project), or [this](https://youtrack.jetbrains.com/issue/WEB-11419).
    some plugins have customized logic for it like intellij-rust or intellij-arend
- [ ] infoview toolwindow in swing
  - [x] show goals
  - [x] show term goal
  - [x] show message
  - [x] interactive message
  - [x] show all messages (all messages currently is skipped for not sure when it's trigger)
  - [x] interactive all messages
  - [x] popup
  - [ ] pop up style, fonts, clickable links, etc
  - [x] color
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
- [x] all message should be interactive (check lean-infoview/src/infoview/info.tsx)
      fixed via passing `hasWidgets` when start lsp
- [ ] jcef infoview style adjust
- [ ] unify jcef/browser/external/vscode infoview font name...
- [ ] jcef infoview popup link should be opened in external web browser
- [ ] setting page
  - [x] added a setting page
  - [ ] add some other settings
  - [ ] lsp autocompletion disable setting
  - [ ] setting for getAllMessages
- [ ] line comment and block comment
- [ ] weird, is it just me or any other reason making only word autocompletion not working? In comment, it works but in normal pos it does not. It seems it's superseded by 
  some semantic autocompletion. --- yeah it's because semantic autocompletion is too slow. Can it be done in two steps? first show alphabet autocompletion and then add more semantic 
  autocompletion 
- [ ] after bump lsp4ij to 0.7.0, make autocompletion configurable
- [ ] quick fix for field missing
- [x] color for error message
- [ ] code completion seems slow and requires manually press ctrl+space
- [ ] TODO is not highlight
– [ ] Autocomplete is slow... like in vscode. Maybe disable it or improve the lean server end
– [x] all messages logic is still wrong maybe it's flushed by new diagnostics
– [x] two cases still exists for all messages: this should already be fixed
  1. it's not shown
  2. it's outdated
– [x] some snippets to things like `\<>`
- [ ] for some snippets maybe it's better to add a space, like `\to`, now for triggering it, it requires a space. But most case it will continue with a space.
  - But not sure for the design, some absolutely don't want a auto created space
– [ ] TODO weird brackets does not complete
– [ ] maybe it's still better define some lang-like feature using parser/lexer, although it cannot be full parsed, but for the level like textmate it should be OK
– [ ] is it possible do something like pygments/ctags/gtags completion?
– [ ] option to skip library or backend files
– [ ] error seems quite delay vanish... it shows errors event it has been fixed.
- [ ] the internal infoview some case also delay (especially using ideavim one does not move caret)
– [ ] comment auto comment like /-- trigger block comment
– [ ] bock/line comment command
– [ ] impl simp? which replace the code
- [ ] settings for getAllMessages, both internal/external infoview
- [ ] maybe it's just me with my idea settings, the gap between first column and line number is a little width
- [ ] autogenerate missing fields
- [ ] internal infoview when expanding all messages it seems jumping
- [ ] check if live templates can dynamically define or not, in this way we can control if suffix space add automatically or not
- [ ] internal infoview will automatically scroll to the end, kind of disturbing

# Maybe some improvements

it is not automatically indent:
```lean
structure Submonoid₁ (M : Type) [Monoid M] where<ENTER HERE DOES NOT INTENT>
  carrior : Set M
```