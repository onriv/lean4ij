# Performance

If there are lots of JCEF processes consuming CPU, run
```bash
taskkill /f /t /im jcef_helper.exe
```
in windows to kill all JCEF processes and run the IDE action `Reload Jcef Infoview` to reload the infoview toolwindow.

TODO add `*nix` command for this

Check [High-CPU-load-caused-by-java-chromium-embedded-framework-JCEF-helper-after-using-markdown-preview](https://youtrack.jetbrains.com/issue/IDEA-255034/High-CPU-load-caused-by-java-chromium-embedded-framework-JCEF-helper-after-using-markdown-preview) for related issue.


# Syntax Highlight

Current there maybe wrongly highlight setup for example like:

```
notation "𝟙_(" C ")" => identity_map
```

a temporal fix for it is doing some random edit and revert it after the wrong string highlight
