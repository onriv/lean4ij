import json

def escape(s):
    for f, t in (
            ("&", "&amp;"),
            ("<", "&lt;"),
            ("\"", "&quot;"),):
        s = s.replace(f, t)
    return s

# from https://github.com/leanprover/vscode-lean4/blob/master/lean4-unicode-input/src/abbreviations.json
with open("abbreviations.json", encoding="utf-8") as f:
    abbrev = json.load(f)
tpl="""<template name="{k}" value="{v}" shortcut="SPACE" description="{v}" toReformat="false" toShortenFQNames="true">
    <context>
      <option name="OTHER" value="true" />
    </context>
  </template>
"""
prefix="\\"
# dont need / for we don't eagerly trigger it...
# TODO handle CURSOR
tplSet= "".join(tpl.format(k=prefix+escape(k),v=v) for k, v in abbrev.items() if k != "\\" and "CURSOR" not in v)
fileContent = f"""<templateSet group="Lean4">
{tplSet}
</templateSet>
"""
with open("Lean4.xml", "w", encoding="utf-8") as f:
    f.write(fileContent)