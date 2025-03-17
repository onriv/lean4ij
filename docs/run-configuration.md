# Run Configurations

To enhance support for Elan, Lake, and Lean file execution, three dedicated run configurations have been implemented:

1. **Elan Command** - Execute Elan toolchain commands
2. **Lake Command** - Run Lake build system operations
3. **Lean File** - Execute Lean source files directly

![Run Configuration Interface](/.github/media/run_configuration.jpg)

## Automated Command Integration
Common workflows are streamlined with automatic configuration of:
- Essential Elan commands (`elan which lean`)
- Core Lake operations (`lake build`, `lake update`)
- Mathlib dependency management (`lake exe cache get` for Mathlib-dependent projects)

## Main Method Execution
A specialized gutter icon appears next to `main` methods for quick execution:

![Main Method Gutter Icon](/.github/media/run_configuration_main_gutter.jpg)

**Workflow:**
1. Click the gutter icon
2. Execution uses: `lake env lean --run <file_name>`
3. Customize arguments via run configuration settings

---

# Known Issues

### Debug Configuration Limitations
**Current Behavior:**
- Debug modal (typically bound to <kbd>F5</kbd> in Visual Studio keymap) may display errors when editing configurations  
  ![Debug Configuration Error](/.github/media/run_configuration_debug_error.jpg)

**Workarounds:**
1. Apply configuration changes first, then trigger execution through the debug modal
2. Preferred alternative: Use the dedicated run menu  
   **<Run> > <Run...>** in the application menu

**Note:** Full debug functionality is currently unavailable. The development team is investigating solutions.