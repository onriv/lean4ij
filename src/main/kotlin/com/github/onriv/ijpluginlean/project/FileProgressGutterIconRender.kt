package com.github.onriv.ijpluginlean.project

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import javax.swing.Icon

class FileProgressGutterIconRender : GutterIconRenderer() {
    override fun equals(other: Any?): Boolean {
        return other == this
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }

    override fun getIcon(): Icon {
        return AllIcons.Actions.Refresh
    }

}