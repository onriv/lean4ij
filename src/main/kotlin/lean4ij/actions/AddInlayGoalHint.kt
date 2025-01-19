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
import lean4ij.setting.Lean4Settings

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
            if (prev == settings.state.commentPrefixForGoalHintRegex!!.pattern) {
                return;
            }
        }

        WriteAction.run<Throwable> {
            CommandProcessor.getInstance().executeCommand(e.project, {
                editor.document.insertString(at, "${whitespacePrefix}${settings.state.commentPrefixForGoalHintRegex!!.pattern}\n")
            }, "Insert Goal Hint", "lean4ij.insertGoalHintCommand");
        }
    }
}
