import json

def escape(s):
    for f, t in (
            ("&", "&amp;"),
            ("<", "&lt;"),
            ("\"", "&quot;"),):
        s = s.replace(f, t)
    return s

if __name__ == "__main__":
    # from https://github.com/leanprover/vscode-lean4/blob/master/lean4-unicode-input/src/abbreviations.json
    with open("abbreviations.json", encoding="utf-8") as f:
        abbrev = json.load(f)
    tpl="""<template name="{k}" value="{v}" shortcut="SPACE" description="{d}" toReformat="false" toShortenFQNames="true">
        <context>
          <option name="OTHER" value="true" />
        </context>
      </template>
    """
    prefix="\\"
    # dont need / for we don't eagerly trigger it...
    # TODO handle CURSOR
    tplSet = []
    for k, v in abbrev.items():
        # since we cannot expand the live template automatically, we do not need to handle this double backslash
        # though sometime we do want it automatically expanded...
        if k == "\\":
            continue
        d = v
        if "CURSOR" in v:
            v, d = v.replace("$CURSOR", "$END$"), v.replace("$CURSOR", "")
        live_tpl = tpl.format(k=prefix+escape(k),v=v, d=d)
        tplSet.append(live_tpl)

    # tplSet= "".join(tpl.format(k=prefix+escape(k),v=v) for k, v in abbrev.items() if k != "\\" and "CURSOR" not in v)
    fileContent = f"""<templateSet group="Lean4">
    {"".join(tplSet)}
    </templateSet>
    """
    with open("../src/main/resources/liveTemplates/Lean4.xml", "w", encoding="utf-8") as f:
        f.write(fileContent)