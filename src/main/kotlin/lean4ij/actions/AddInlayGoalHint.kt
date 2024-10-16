package lean4ij.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import lean4ij.Lean4Settings

class AddInlayGoalHint : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.setEnabledAndVisible(e.getData(CommonDataKeys.EDITOR) != null);
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor: Editor = e.getRequiredData(CommonDataKeys.EDITOR);
        val selector = editor.selectionModel
        val selectionStart = selector.selectionStart

        val settings = service<Lean4Settings>();

        val lineCol = StringUtil.offsetToLineColumn(editor.document.text, selectionStart);
        val at = selectionStart - lineCol.column;
        val text = editor.document.text
        var endIndex = at
        while (endIndex < text.length && text[endIndex].isWhitespace() && text[endIndex] != '\n') {
            endIndex++
        }
        val whitespacePrefix = text.subSequence(at, endIndex)

        // only perform action if not already present in previous line
        if (lineCol.line != 0) {
            val prevStart = StringUtil.lineColToOffset(text, lineCol.line - 1, 0);
            val prev = text.substring(prevStart, at).trim();
            if (prev == settings.commentPrefixForGoalHint) {
                return;
            }
        }

        WriteAction.run<Throwable> {
            CommandProcessor.getInstance().executeCommand(e.project, {
                editor.document.insertString(at, "${whitespacePrefix}${settings.commentPrefixForGoalHint}\n")
            }, "Insert Goal Hint", "lean4ij.insertGoalHintCommand");
        }
    }
}
class DelInlayGoalHint : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.setEnabledAndVisible(e.getData(CommonDataKeys.EDITOR) != null);
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor: Editor = e.getRequiredData(CommonDataKeys.EDITOR);
        val selector = editor.selectionModel
        val selectionStart = selector.selectionStart
        val selectionEnd = selector.selectionEnd

        val settings = service<Lean4Settings>();

        val lastLine = StringUtil.offsetToLineColumn(editor.document.text, selectionEnd).line;
        var firstLine = StringUtil.offsetToLineColumn(editor.document.text, selectionStart).line;
        if (selectionStart == selectionEnd) {
            firstLine = maxOf(0, firstLine - 1);
        }

        WriteAction.run<Throwable> {
            for (i in lastLine downTo firstLine) {

                val text = editor.document.text
                val lineStart = StringUtil.lineColToOffset(text, i, 0);
                val lineEnd = StringUtil.lineColToOffset(text, i + 1, 0);
                val line = text.substring(lineStart, lineEnd).trim();
                if (line == settings.commentPrefixForGoalHint) {
                    CommandProcessor.getInstance().executeCommand(e.project, {
                        editor.document.deleteString(lineStart, lineEnd)
                    }, "Delete Goal Hint", "lean4ij.deleteGoalHintCommand");
                }
            }
        }
    }
}
