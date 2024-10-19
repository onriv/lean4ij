package lean4ij.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service

// Reference: https://github.com/chylex/IntelliJ-Inspection-Lens/blob/main/src/main/kotlin/com/chylex/intellij/inspectionlens/InspectionLensPluginDisposableService.kt
@Service
class MyProjectDisposableService : Disposable {
    override fun dispose() {}
}
