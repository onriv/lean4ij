package lean4ij.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;

%%

%{
    public Lean4Lexer() {
        this((java.io.Reader)null);
    }
%}

%public
%class Lean4Lexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%state DOC_COMMENT

EOL                 = \R
WHITE_SPACE         = [ \t\r\n]+

%%

<YYINITIAL> {
    {WHITE_SPACE}           { return WHITE_SPACE; }
}
