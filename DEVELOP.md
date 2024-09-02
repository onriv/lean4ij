

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

# Ref

- Some threads in zulip during the developments:
  - [Would lsp server quit anomaly?](https://leanprover.zulipchat.com/#narrow/stream/270676-lean4/topic/Would.20lsp.20server.20quit.20anomaly.3F)
  - [`elan which lake` downloads lean if it not exist](https://leanprover.zulipchat.com/#narrow/stream/270676-lean4/topic/.60elan.20which.20lake.60.20downloads.20lean.20if.20it.20not.20exist) This is why currently the way to find the toolchain is manually done
  - [integrating infoview app](https://leanprover.zulipchat.com/#narrow/stream/270676-lean4/topic/integrating.20infoview.20app)
  - [Lean 4 outside of VS Code](https://leanprover.zulipchat.com/#narrow/stream/113488-general/topic/Lean.204.20outside.20of.20VS.20Code)
  - [lsp fileProgress and gerInteractiveGoals](https://leanprover.zulipchat.com/#narrow/stream/270676-lean4/topic/lsp.20fileProgress.20and.20gerInteractiveGoals)
  - [lsp getInteractiveGoals and subexprPos?](https://leanprover.zulipchat.com/#narrow/stream/113489-new-members/topic/lsp.20getInteractiveGoals.20and.20subexprPos.3F)
  - [Full BNF syntax?](https://leanprover.zulipchat.com/#narrow/stream/113489-new-members/topic/Full.20BNF.20syntax.3F)
  - [using lean4-infoview alone inside an external browser](https://leanprover.zulipchat.com/#narrow/stream/113488-general/topic/using.20lean4-infoview.20alone.20inside.20an.20external.20browser)
  - [✔ Lean LSP extensions](https://leanprover.zulipchat.com/#narrow/stream/113488-general/topic/.E2.9C.94.20Lean.20LSP.20extensions)

