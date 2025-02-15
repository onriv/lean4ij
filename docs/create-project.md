# Usage

To create a new Lean4 project in IntelliJ IDEA:
1. Navigate to **File > New > Project...**
2. Select **Lean4** from the list of project generators.

**Note:** While this feature has been tested in other JetBrains IDEs (such as CLion, PyCharm, and RustRover), it is currently only functional in IntelliJ IDEA.

![Create project](/.github/media/create_project.jpg)

## Project Creation Process
- The system forwards the creation request to `~/.elan/bin/lake`, with a confirmation comment displayed in the panel.
- Upon successful project creation, the `lean-toolchain` file is automatically updated to reflect the selected Lean version.
- If the specified toolchain isn't locally available, the IDE automatically executes `elan which lean` to download it.

### Handling Network Issues
If toolchain download fails due to network restrictions, configure a proxy via the proxy settings group. These settings populate the `HTTPS_PROXY` environment variable for Elan.

![Create project with proxy](/.github/media/create_project_proxy.jpg)

### Advanced Configuration
Customize project creation using options documented in `lake new -h`:

![Create project with options](/.github/media/create_project_options.jpg)

---

# Known Issues

1. **IDE Compatibility:** As noted, this feature is exclusive to IntelliJ IDEA despite testing in other JetBrains IDEs.
2. **SDK Initialization:** After project creation, SDK components or the language server might not initialize properly. If this occurs, try reopening the project.
3. **UI Blocking:** Project creation may temporarily freeze the IDE's UI. This process is typically brief but can vary based on system performance.

---

# Implementation Notes

## Toolchain Management
While Lake+Elan supports `lake +<version> new` for version-specific project creation, this method isn't utilized here due to synchronous execution blocking the UI during potentially lengthy downloads.

Instead, the IDE runs `elan which lean` post-creation to fetch missing toolchains asynchronously. This prevents UI blockage, though users may need to reopen the project after download completion for full SDK functionality.

## Design Philosophy
Version 0.2.0 emphasizes IDE-specific enhancements and Lean's programming language capabilities, diverging from 0.1.x's focus on theorem-proving aspects.

---

**Reference:** [Lake New Project with Specified Lean Version?](https://leanprover.zulipchat.com/#narrow/channel/113489-new-members/topic/lake.20new.20project.20with.20specified.20lean.20version.3F)
