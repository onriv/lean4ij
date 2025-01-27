package lean4ij.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import lean4ij.language.Lean4Definition
import lean4ij.language.psi.TokenType

class MainRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.parent !is Lean4Definition) return null
        if (element.node.elementType == TokenType.IDENTIFIER && element.node.text == "main") {
            if (element.nextSibling == null && element.prevSibling.elementType == TokenType.WHITE_SPACE) {
                // TODO
                val icon = AllIcons.RunConfigurations.TestState.Run
                return Info(icon, {
                        runReadAction {
                            "Run current main method"
                        }
                    },
                    *ExecutorAction.getActions(1)
                )
            }
        }
        return null
    }
}