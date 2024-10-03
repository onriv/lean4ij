
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
- [ ] setting page
