package lean4ij.util

import com.intellij.platform.util.progress.ProgressReporter
import com.intellij.platform.util.progress.reportProgress

// Call without an implicit CoroutineScope receiver so Kotlin selects the overload available in IDEA 2025.1.
internal suspend fun reportFileProgress(action: suspend (ProgressReporter) -> Unit) {
    reportProgress(action = action)
}

/**
 * a util suspend function for avoiding empty brace
 */
suspend fun ProgressReporter.step(size: Int) {
    sizedStep(size) {}
}
