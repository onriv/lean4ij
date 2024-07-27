package com.github.onriv.ijpluginlean.lsp.data

import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * TODO add doc
 */
class PlainTermGoalParams(textDocument: TextDocumentIdentifier, position: Position) :
    TextDocumentPositionParams(textDocument, position)