package lean4ij.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager.getInstance
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import lean4ij.project.LeanProjectService
import com.intellij.ide.plugins.PluginManager
import com.intellij.java.library.JavaLibraryUtil.hasLibraryClass
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.ZipUtil
import lean4ij.module.DownloadableModel


/**
 * notify an error with [content]
 */
fun Project.notifyErr(content: String) {
    getInstance()
        // TODO custom notification group
        .getNotificationGroup("Custom Notification Group")
        .createNotification(content, NotificationType.ERROR)
        .notify(this);
}

fun Project.notify(content: String) {
    getInstance()
        // TODO custom notification group
        .getNotificationGroup("Custom Notification Group")
        .createNotification(content, NotificationType.INFORMATION)
        .notify(this);
}

fun Project.notify(content: String, type: NotificationType) {
    getInstance()
        // TODO custom notification group
        .getNotificationGroup("Custom Notification Group")
        .createNotification(content, type)
        .notify(this);
}

fun Project.notify(content: String, action: (Notification) -> Notification) {
    val notification = getInstance()
        // TODO custom notification group
        .getNotificationGroup("Custom Notification Group")
        .createNotification(content, NotificationType.INFORMATION)
    action(notification)
}

val Project.leanProjectService get(): LeanProjectService = service()

val Project.leanProjectScope get() = leanProjectService.scope

val PROJECT_MODEL_PROP_KEY = Key<GraphProperty<DownloadableModel?>>("lean_project_model")
