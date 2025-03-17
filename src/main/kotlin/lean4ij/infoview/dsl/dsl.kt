package lean4ij.infoview.dsl

import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FoldingListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import lean4ij.infoview.Lean4TextAttributesKeys
import lean4ij.infoview.InfoViewWindowFactory
import lean4ij.infoview.LeanInfoviewContext
import lean4ij.lsp.data.ContextInfo
import lean4ij.lsp.data.FoldingData
import lean4ij.lsp.data.InteractiveHypothesisBundle
import lean4ij.lsp.data.SubexprInfo
import lean4ij.lsp.data.TaggedText
import java.lang.ProcessHandle.Info

fun info(click: LeanInfoviewContext.(InfoObjectModel) -> Unit, init: InfoObjectBuilder.()->Unit) : InfoObjectModel {
    val infoObjectBuilder = InfoObjectBuilder().apply {
        init()
    }
    return infoObjectBuilder.build().also {
        it.clickAction = click
    }
}

fun info(init: InfoObjectBuilder.()->Unit) : InfoObjectModel {
    val infoObjectBuilder = InfoObjectBuilder().apply {
        init()
    }
    return infoObjectBuilder.build()
}

class InfoObjectBuilder {

    private val data: MutableList<InfoObjectModel> = mutableListOf()
    private val attr: MutableList<TextAttributesKey> = mutableListOf()
    private var contextInfo: ContextInfo? = null

    fun build(): InfoObjectModel {
        return InfoObjectModel(children = data, attr = attr).also {
            for (child in it.children) {
                child.parent = it
            }
            it.contextInfo = contextInfo
        }
    }

    fun add(model: InfoObjectModel) {
        data.add(model)
    }

    fun fold(init: InfoObjectBuilder.() -> Unit) {
        fold(true, init)
    }

    fun fold(expanded: Boolean, init: InfoObjectBuilder.() -> Unit) {
        fold(expanded, null, init)
    }

    fun fold(expanded: Boolean, listener: ((FoldRegion)->Unit)?, init: InfoObjectBuilder.() -> Unit) {
        fold(expanded, false, listener, init)
    }

    fun fold(expanded: Boolean, isAllMessages: Boolean=false, listener: ((FoldRegion)->Unit)?, init: InfoObjectBuilder.() -> Unit) {
        val model = InfoObjectBuilder().run {
            init()
            build()
        }
        model.fold = FoldingData(0,0, "", expanded, isAllMessages=isAllMessages, listener=listener)
        data.add(model)
    }

    fun h1(text: String) {
        data.add(InfoObjectModel(text+"\n", attr = Lean4TextAttributesKeys.SwingInfoviewExpectedType.key))
    }

    fun h2(text: String) {
        // TODO it's vague adding the line break this way, since h1 has some folding behavior too
        data.add(InfoObjectModel(text+"\n", attr = Lean4TextAttributesKeys.SwingInfoviewExpectedType.key))
    }

    fun h3(text: String) {
        // TODO it's vague adding the line break this way, since h2 has some folding behavior too
        data.add(InfoObjectModel(text+"\n", attr = Lean4TextAttributesKeys.SwingInfoviewCasePos.key))
    }

    fun p(text: String, attr: MutableList<TextAttributesKey> = mutableListOf()) {
        data.add(InfoObjectModel(text, attr=attr))
    }

    fun p(text: String, click: (LeanInfoviewContext, InfoObjectModel)->Unit)  {
        data.add(InfoObjectModel(text, clickAction = click))
    }

    fun p(text: String, attr: MutableList<TextAttributesKey>, click: (LeanInfoviewContext, InfoObjectModel) -> Unit) {
        data.add(InfoObjectModel(text, attr, clickAction = click))
    }

    fun p(text: String) {
        data.add(InfoObjectModel(text))
    }

    fun br() {
        data.add(InfoObjectModel("\n"))
    }

    fun p(text: String, attr: TextAttributesKey) {
        data.add(InfoObjectModel(text, attr=attr))
    }

    fun p(text: String, attr: Lean4TextAttributesKeys) {
        data.add(InfoObjectModel(text, attr=attr.key))
    }

    operator fun String.unaryPlus() {
        data.add(InfoObjectModel(this))
    }

    fun createGoalObjectModel(hyps: Array<InteractiveHypothesisBundle>, type: TaggedText<SubexprInfo>) {
        for (hyp in hyps) {
            val names = hyp.names.joinToString(prefix = "", separator = " ", postfix = "")
            // TODO check if it's possible for using multiple text attributes
            val attr : MutableList<Lean4TextAttributesKeys> = mutableListOf()
            when {
                hyp.isRemoved == true -> attr.add(Lean4TextAttributesKeys.RemovedText)
                hyp.isInserted == true -> attr.add(Lean4TextAttributesKeys.InsertedText)
            }
            if (names.contains("✝")) {
                attr.add(Lean4TextAttributesKeys.GoalInaccessible)
            } else {
                attr.add(Lean4TextAttributesKeys.GoalHyp)
            }
            p(names, attr.map { it.key }.toMutableList())
            p(" : ")
            add(hyp.type.toInfoObjectModel())
            // TODO is it suitable doing line break this way?
            br()
        }
        p("⊢ ", Lean4TextAttributesKeys.SwingInfoviewGoalSymbol)
        add(type.toInfoObjectModel())
    }

    fun addAttr(attr: MutableList<TextAttributesKey>) {
        this.attr.addAll(attr)
    }

    fun setContextInfo(contextInfo: ContextInfo?) {
        this.contextInfo = contextInfo
    }

    fun onClick() {
    }
}


/**
 * TODO all fields are quite lenient access scope
 */
class InfoObjectModel(
    var text: String = "",
    val attr: MutableList<TextAttributesKey> = mutableListOf(),
    // TODO maybe do not allow children to be null
    val children: MutableList<InfoObjectModel> = mutableListOf(),
    // TODO make a class for this
    var fold: FoldingData? = null,
    var clickAction: (LeanInfoviewContext.(InfoObjectModel) -> Unit)? = null
) {
    // it's mainly for using in lazy trace
    var parent : InfoObjectModel? = null

    constructor(text: String, attr: TextAttributesKey) : this(text, attr = mutableListOf(attr))

    /**
     * avoid using [toStringAndEditorActions] for this method
     * since [toStringAndEditorActions] changes start/end etc
     */
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(text)
        for (elem in children) {
            sb.append(elem)
        }
        return sb.toString()
    }

    fun output(editorEx: EditorEx) {
        val sb = StringBuilder()
        val attrList: MutableList<(EditorEx) -> Unit> = mutableListOf()
        val foldsList: MutableList<FoldingData> = mutableListOf()
        toStringAndEditorActions(sb, attrList, foldsList)
        editorEx.document.setText(sb.toString())
        for (action in attrList) {
            action(editorEx)
        }
        editorEx.foldingModel.runBatchFoldingOperation {
            editorEx.foldingModel.clearFoldRegions()
            for (folding in foldsList) {
                val foldRegion = editorEx.foldingModel.addFoldRegion(folding.startOffset, folding.endOffset, folding.placeholderText)
                foldRegion?.apply {
                    isExpanded = if (folding.isAllMessages) {
                        InfoViewWindowFactory.expandAllMessage
                    } else {
                        folding.expanded
                    }
                }
                editorEx.foldingModel.addListener(object : FoldingListener {
                    override fun onFoldRegionStateChange(region: FoldRegion) {
                        if (region == foldRegion) {
                            folding.listener?.invoke(region)
                        }
                    }
                }) {
                    // TODO should some disposal add here?
                }
            }
        }
    }

    private var fullText : String? = null
    // TODO only pub for test
    var start : Int? = null
    // TODO only pub for test
    var end : Int? = null
    var contextInfo : ContextInfo? = null

    /**
     * TODO define a concrete type for the return triple
     */
    fun toStringAndEditorActions(sb: StringBuilder, attrs: MutableList<(EditorEx)->Unit>, folds: MutableList<FoldingData>) {
        start = sb.length
        sb.append(text)
        for (elem in children) {
            elem.toStringAndEditorActions(sb, attrs, folds)
        }
        end = sb.length
        fullText = sb.substring(start!!, end!!)
        for (key in attr) {
            attrs.add { editorEx ->
                EditorColorsManager.getInstance().globalScheme.getAttributes(key)?.let {
                    editorEx.markupModel.addRangeHighlighter(start!!, end!!, HighlighterLayer.SYNTAX, it, HighlighterTargetArea.EXACT_RANGE)
                }
            }
        }
        fold?.let {
            val firstLine = fullText!!.split("\n")[0]
            // default collapsed text to the first line
            // TODO after cleaning code, use it directly
            // TODO here it absolutely should not copy the data,  very buggy when adding new fields
            folds.add(FoldingData(start!!, end!!, firstLine, it.expanded, isAllMessages = it.isAllMessages, listener = it.listener))
        }
    }

    private fun isShownInToolWindow() : Boolean  {
        return fullText != null && start != null && end != null
    }

    fun getCodeContext(offset: Int): Triple<ContextInfo, Int, Int>? {
        // the method should be called with the model is already output
        if (!isShownInToolWindow()) {
            return null
        }
        if (offset !in start!!..<end!!) {
            return null
        }
        for (child in children) {
            child.getCodeContext(offset)?.let {
                return it
            }
        }
        return contextInfo?.let {
            return Triple(it, start!!, end!!)
        }

    }

    fun getChild(offset: Int): InfoObjectModel? {
        // the method should be called with the model is already output
        if (!isShownInToolWindow()) {
            return null
        }
        if (offset !in start!!..<end!!) {
            return null
        }
        for (child in children) {
            child.getChild(offset)?.let {
                return it
            }
        }
        return this
    }

    fun click(context: LeanInfoviewContext) {
        clickAction?.let {
            context.it(this)
        }
    }
}