

# LSP
using [Lsp4ij](https://github.com/redhat-developer/lsp4ij)

for some document on lean4 lsp server, check 
- [src/Lean/Server](https://github.com/leanprover/lean4/tree/master/src/Lean/Server)

TODO

## Highlight

TODO

## Unicode input

TODO

## InfoView

TODO

## Developing in Intellij Idea
(TODO this seems not work)Proxy issue (this should only happen in some specific region)
If the runPlugin task requires some proxy, do
```
ORG_GRADLE_PROJECT_systemProp.https.proxyHost=<ip>
ORG_GRADLE_PROJECT_systemProp.https.proxyPort=<port>
ORG_GRADLE_PROJECT_systemProp.https.nonProxyHosts=*.nonproxyrepos.com|localhost
```
in system environment. Replace the `<ip>` and `<port>` to real value.
ref: https://docs.gradle.org/current/userguide/project_properties.html

For first (and while require updating the frontend, run a `gradle buildBrowserInfoview` before run `runIDE`)