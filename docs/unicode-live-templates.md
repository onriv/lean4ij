
Unicode input is supported by the automatically generated live templates by
adapting the abbreviations from [vscode-lean4](https://github.com/leanprover/vscode-lean4/blob/master/lean4-unicode-input/src/abbreviations.json)

The basic usage for this is typing `\a` and then trigger it with `<space>`. If the current position is able to autocomple this, the IDE would pop up the unicodes as autocomplete candidates:

<img width="586" alt="image" src="https://github.com/user-attachments/assets/208a2a3c-96fd-4381-8de9-2e26352f8eed" />

In other case like for typing some unicode for an identifier, the autocomplete candidates list will not popup and the unicode will be triggered after typing `<space>`. If there is only one candidate then it will be automically inserted:

![live_templates1](https://github.com/user-attachments/assets/c2e913cd-f8ad-4c95-8800-dc4f2cebeddb)

The templates are split in three groups (Editor/Live Templates):

- `Lean4`: the abbreviations, basically the same behavior as vscode-lean4.
- `Lean4Pair`: automatically inserting the right part of a pair when typing the left part (`\<` gives `⟨⟩`).
- `Lean4Space`: the same abbreviations as `Lean4` but with a trailing space baked into the template.

In the settings (Languages & Frameworks/Lean4) two options control this:

- *Space after unicode abbreviation*:
  - *Only when expanded with Space* (default): `\neg<Space>` inserts `¬ ` and `\neg<Tab>` (or Enter, or a click
    in the completion popup) inserts `¬`, like in nvim/vscode. This uses the `Lean4` group; the space is added by
    the plugin when the abbreviation is expanded with the Space key.
  - *Always*: every expansion adds a trailing space (the `Lean4Space` group is enabled instead of `Lean4`).
  - *Never*: no expansion adds a space (the `Lean4` group alone).
- *Autocomplete live templates for pair unicode*: enables the `Lean4Pair` group.
