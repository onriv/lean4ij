package lean4ij.util

import com.intellij.platform.util.progress.ProgressReporter

/**
 * a util suspend function for avoiding empty brace
 */
suspend fun ProgressReporter.step(size: Int) {
    sizedStep(size) {}
}
