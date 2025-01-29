
Unicode input is supported by the automatically generated live templates by
adapting the abbreviations from [vscode-lean4](https://github.com/leanprover/vscode-lean4/blob/master/lean4-unicode-input/src/abbreviations.json)

The basic usage for this is typing `\a` and then trigger it with `<space>`. If the current position is able to autocomple this, the IDE would pop up the unicodes as autocomplete candidates:

<img width="586" alt="image" src="https://github.com/user-attachments/assets/208a2a3c-96fd-4381-8de9-2e26352f8eed" />

In other case like for typing some unicode for an identifier, the autocomplete candidates list will not popup and the unicode will be triggered after typing `<space>`. If there is only one candidate then it will be automically inserted:

![live_templates1](https://github.com/user-attachments/assets/c2e913cd-f8ad-4c95-8800-dc4f2cebeddb)

For providing few options they are split in three group:

- Editor/Live Templates/Lean4
- Editor/Live Templates/Lean4Pair
- Editor/Live Templates/Lean4Space

The group `Lean4` is basically the same behavior as vscode-lean4. And the group `Lean4Pair` is for automatically adding inserting pairs when typing the left part. The group `Lean4Space`
is for avoiding type the space again.

In the settings there are 3 options for control this. 
- add whitespace after live templates for unicode: this will enable the whitespace group and disable the default group
- autocomplete live templates for pair unicode: this will enable the pair group
- enable both spaced and non spaced live templates: and this will enable both the whitespace and the default group

<img width="371" alt="image" src="https://github.com/user-attachments/assets/343ac2c1-ec3d-4c44-a783-0a123fb2ecbd" />
