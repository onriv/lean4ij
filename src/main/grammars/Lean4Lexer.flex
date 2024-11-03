package lean4ij.language;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static lean4ij.language.psi.TokenType.*;

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

%{
  private String name;
  private int commentStart;
  private int commentDepth;
  private boolean isInsideDocComment;
  private int originalState = YYINITIAL;
%}

%state BLOCK_COMMENT_INNER

EOL                 = \R
WHITE_SPACE         = [ \t\r\n]+

// LINE_COMMENT        = -- ([ ] ([^\|\r\n] .* | {EOL})? | ([^ ~!@#$%\^&*\-+=<>?/|\[\]:a-zA-Z_0-9'\u2200-\u22FF\u2A00-\u2AFF\r\n] .* | {EOL})? | -+ ([^~!@#$%\^&*\-+=<>?/|\[\]:a-zA-Z_0-9'\u2200-\u22FF\u2A00-\u2AFF\r\n] .* | {EOL})?)
LINE_COMMENT = --[^\r\n]*

// TODO this file seems occuring some encoding issue, once I got BLOCK_COMMENT_INNER not declared error
//      but after deleting it and re-typing, it works fine
BlOCK_COMMENT_START = "/-"
BLOCK_DOC_COMMENT_START = "/--"
BlOCK_COMMENT_END = "-/"

STRING              = \"{STRING_CONTENT}*\"
STRING_CONTENT      = [^\"\\\r\n] | \\[btnfr\"\'\\] | {OCT_ESCAPE} | {UNICODE_ESCAPE}
OCT_ESCAPE          = \\{OCT_DIGIT}{OCT_DIGIT}? | \\[0-3]{OCT_DIGIT}{2}
UNICODE_ESCAPE      = \\u+{HEX_DIGIT}{4}
HEX_DIGIT           = [0-9a-fA-F]
OCT_DIGIT           = [0-8]

// weird, without the \b, the scanner does not recognize the keywords
KEYWORD_COMMAND1        = prelude|import|include|export|open|mutual
KEYWORD_COMMAND_PREFIX   = local|private|protected|scoped|partial|noncomputable|unsafe
KEYWORD_MODIFIER        = renaming|hiding|where|extends|using|with|at|rec|deriving
KEYWORD_COMMAND2        = syntax|elab|elab_rules|macro_rules|macro
KEYWORD_COMMAND3        = namespace|section|end
KEYWORD_COMMAND4        = class|def|lemma|example|theorem|instance
KEYWORD_COMMAND5        = #check|#guard_msgs|#eval|#reduce
KEYWORD_SORRY = sorry
DEFAUTL_TYPE = Type|(Type \*)

 // special
left_paren          = "("
right_paren         = ")"
comma               = ","
semicolon           = ";"
left_bracket        = "["
right_bracket       = "]"
backquote           = "`"
left_brace          = "{"
right_brace         = "}"
dot = "."

// TODO any way to avoid the exclusion?
GREEK = [\u0370-\u03FF]
ALPHA_NUM = [a-zA-Z0-9_]
SUPERSCRIPT = [⁻¹²³⁴⁵⁶⁷⁸⁹⁰]
SUBSCRIPT = [₁₂₃₄₅₆₇₈₉₀]
IDENTIFIER              = ({ALPHA_NUM} | {GREEK}|{digit}|{quote}|{SUPERSCRIPT}|{SUBSCRIPT})+

NUMBER              = [0-9]+
NEGATIVE_NUMBER     = -{NUMBER}

// the following part is copied from intellij-haskell
newline             = \r|\n|\r\n
unispace            = \x05
white_char          = [\ \t\f\x0B\ \x0D ] | {unispace}    // second "space" is probably ^M, I could not find other solution then justing pasting it in to prevent bad character.
directive           = "#"{white_char}*("if"|"ifdef"|"ifndef"|"define"|"elif"|"else"|"error"|"endif"|"include"|"undef")("\\" (\r|\n|\r\n) | [^\r\n])*
include_directive   = "#"{white_char}*"include"{white_char}*\"({small}|{large}|{digit}|{dot})+\"
white_space         = {white_char}+

underscore          = "_"
small               = [a-z] | {underscore} | [\u03B1-\u03C9] | 𝑖 | 𝕧 | µ | ¬
large               = [A-Z] | [\u0391-\u03A9] | ℝ | ℂ | ℕ | ℤ | ℚ

digit               = [0-9] | [\u2070-\u2079] | [\u2080-\u2089]
decimal             = [-+]?({underscore}*{digit}+)+

hexit               = [0-9A-Fa-f]
hexadecimal         = 0[xX]({underscore}*{hexit}+)+

octit               = [0-7]
octal               = 0[oO]({underscore}*{octit}+)+

float               = [-+]?(({underscore}*[0-9]+)+(\.({underscore}*[0-9]+)+)?|\ \.({underscore}*[0-9]+)+)([eE][-+]?[0-9]+)?

gap                 = \\({white_char}|{newline})*\\
cntrl               = {large} | [@\[\\\]\^_]
charesc             = [abfnrtv\\\"\'&]
ascii               = ("^"{cntrl})|(NUL)|(SOH)|(STX)|(ETX)|(EOT)|(ENQ)|(ACK)|(BEL)|(BS)|(HT)|(LF)|(VT)|(FF)|(CR)|(SO)|(SI)|(DLE)|(DC1)|(DC2)|(DC3)|(DC4)|(NAK)|(SYN)|(ETB)|(CAN)|(EM)|(SUB)|(ESC)|(FS)|(GS)|(RS)|(US)|(SP)|(DEL)
escape              = \\({charesc}|{ascii}|({digit}+)|(o({octit}+))|(x({hexit}+)))

character_literal   = (\'([^\'\\\n]|{escape})\')
string_literal      = \"([^\"\\\n]|{escape}|{gap})*(\"|\n)

// ascSymbol except reservedop
exclamation_mark    = "!"
hash                = "#"
dollar              = "$"
percentage          = "%"
ampersand           = "&"
star                = "*"
unicode_star        = "★"
plus                = "+"
dot                 = "."
small_circle        = "∘"
slash               = "/"
lt                  = "<"
gt                  = ">"
question_mark       = "?"
caret               = "^"
dash                = "-"

// symbol and reservedop
equal               = "="
at                  = "@"
backslash           = "\\"
vertical_bar        = "|"
tilde               = "~"
colon               = ":"

colon_colon         = "::" | "∷"
left_arrow          = "<-" | "←"
right_arrow         = "->" | "→"
double_right_arrow  = "=>" | "⇒"

 // special
left_paren          = "("
right_paren         = ")"
comma               = ","
semicolon           = ";"
left_bracket        = "["
right_bracket       = "]"
backquote           = "`"
left_brace          = "{"
right_brace         = "}"
left_uni_bracket    = "⟨"
right_uni_bracket   = "⟩"
left_uni_double_bracket = "⟦"
right_uni_double_bracket = "⟧"
template_trigger = \\[^  \t\r\n]+

// this part is copied from julia-intellij
MISC_COMPARISON_SYM      =[∉∋∌⊆⊈⊂⊄⊊∝∊∍∥∦∷∺∻∽∾≁≃≄≅≆≇≈≉≊≋≌≍≎≐≑≒≓≔≕≖≗≘≙≚≛≜≝≞≟≣≦≧≨≩≪≫≬≭≮≯≰≱≲≳≴≵≶≷≸≹≺≻≼≽≾≿⊀⊁⊃⊅⊇⊉⊋⊏⊐⊑⊒⊜⊩⊬⊮⊰⊱⊲⊳⊴⊵⊶⊷⋍⋐⋑⋕⋖⋗⋘⋙⋚⋛⋜⋝⋞⋟⋠⋡⋢⋣⋤⋥⋦⋧⋨⋩⋪⋫⋬⋭⋲⋳⋴⋵⋶⋷⋸⋹⋺⋻⋼⋽⋾⋿⟈⟉⟒⦷⧀⧁⧡⧣⧤⧥⩦⩧⩪⩫⩬⩭⩮⩯⩰⩱⩲⩳⩴⩵⩶⩷⩸⩹⩺⩻⩼⩽⩾⩿⪀⪁⪂⪃⪄⪅⪆⪇⪈⪉⪊⪋⪌⪍⪎⪏⪐⪑⪒⪓⪔⪕⪖⪗⪘⪙⪚⪛⪜⪝⪞⪟⪠⪡⪢⪣⪤⪥⪦⪧⪨⪩⪪⪫⪬⪭⪮⪯⪰⪱⪲⪳⪴⪵⪶⪷⪸⪹⪺⪻⪼⪽⪾⪿⫀⫁⫂⫃⫄⫅⫆⫇⫈⫉⫊⫋⫌⫍⫎⫏⫐⫑⫒⫓⫔⫕⫖⫗⫘⫙⫷⫸⫹⫺⊢⊣⟂]
MISC_PLUS_SYM      =[⊕⊖⊞⊟++∪∨⊔±∓∔∸≂≏⊎⊽⋎⋓⧺⧻⨈⨢⨣⨤⨥⨦⨧⨨⨩⨪⨫⨬⨭⨮⨹⨺⩁⩂⩅⩊⩌⩏⩐⩒⩔⩖⩗⩛⩝⩡⩢⩣]
// temporarily removed ⋅ and ×
MISC_MULTIPLY_SYM      =[∘∩∧⊗⊘⊙⊚⊛⊠⊡⊓∗∙∤⅋≀⊼⋄⋆⋇⋉⋊⋋⋌⋏⋒⟑⦸⦼⦾⦿⧶⧷⨇⨰⨱⨲⨳⨴⨵⨶⨷⨸⨻⨼⨽⩀⩃⩄⩋⩍⩎⩑⩓⩕⩘⩚⩜⩞⩟⩠⫛⊍▷⨝⟕⟖⟗]
MISC_EXPONENT_SYM      =[↑↓⇵⟰⟱⤈⤉⤊⤋⤒⤓⥉⥌⥍⥏⥑⥔⥕⥘⥙⥜⥝⥠⥡⥣⥥⥮⥯￪￬]
MISC_ARROW_SYM      =[←→↔↚↛↞↠↢↣↦↤↮⇎⇍⇏⇐⇒⇔⇴⇶⇷⇸⇹⇺⇻⇼⇽⇾⇿⟵⟶⟷⟹⟺⟻⟼⟽⟾⟿⤀⤁⤂⤃⤄⤅⤆⤇⤌⤍⤎⤏⤐⤑⤔⤕⤖⤗⤘⤝⤞⤟⤠⥄⥅⥆⥇⥈⥊⥋⥎⥐⥒⥓⥖⥗⥚⥛⥞⥟⥢⥤⥦⥧⥨⥩⥪⥫⥬⥭⥰⧴⬱⬰⬲⬳⬴⬵⬶⬷⬸⬹⬺⬻⬼⬽⬾⬿⭀⭁⭂⭃⭄⭇⭈⭉⭊⭋⭌￩￫⇜⇝↜↝↩↪↫↬↼↽⇀⇁⇄⇆⇇⇉⇋⇌⇚⇛⇠⇢]

quote               = "'"
double_quotes       = "\""

forall              = "∀"

symbol_no_dot       = {equal} | {at} | {backslash} | {vertical_bar} | {tilde} | {exclamation_mark} | {hash} | {dollar} | {percentage} | {ampersand} | {star} |
                        {plus} | {slash} | {lt} | {gt} | {question_mark} | {caret} | {dash} | [\u2201-\u22FF]


symbol              = {symbol_no_dot} | {dot}

base_var_id         = {small} ({small} | {large} | {digit} | {quote})*
var_id              = {question_mark}? {base_var_id} | {hash} {base_var_id} | {base_var_id} {hash}
varsym_id           = (({symbol_no_dot} | {left_arrow} | {right_arrow} | {double_right_arrow}) ({symbol} | {colon})+) |
                        {symbol_no_dot} ({symbol} | {colon})*

con_id              = {large} ({small} | {large} | {digit} | {quote})* {hash}?
consym_id           = {quote}? {colon} ({symbol} | {colon})*


pragma_start        = {left_brace}{dash}{hash}
pragma_end          = {hash}{dash}{right_brace}

// Accept also * after -- because of TypeOperators
comment             = {dash}{dash}{dash}*[^\r\n\!\#\$\%\&\⋆\+\.\/\<\=\>\?\@\*][^\r\n]* | {dash}{dash}{white_char}* | "\\begin{code}"
ncomment_start      = {left_brace}{dash}
ncomment_end        = {dash}{right_brace}
haddock             = {dash}{dash}{white_char}[\^\|][^\r\n]* ({newline}+{white_char}*{comment})*
nhaddock_start      = {left_brace}{dash}{white_char}?{vertical_bar}
// the above part is copied from intellij-haskell


%%

<YYINITIAL> {

    {WHITE_SPACE}           {
          return WHITE_SPACE;
                            }
    {LINE_COMMENT}          {
          return LINE_COMMENT;
                            }
    {STRING}                {
          return STRING;
                            }
    {at} {
        return AT;
    }
    {colon}    {
        return COLON;
    }
    {MISC_COMPARISON_SYM}    {
        return MISC_COMPARISON_SYM;
    }
    {star}    {
    return STAR;
    }
    {forall}  {
    return FOR_ALL;
    }
    {NUMBER}                { return NUMBER; }
    {NEGATIVE_NUMBER}       { return NEGATIVE_NUMBER; }

    {comma} {
    return COMMA;
    }
    {equal} {
    return EQUAL;
    }
    {BlOCK_COMMENT_START}   {
                                originalState = yystate();
                                yybegin(BLOCK_COMMENT_INNER);
                                isInsideDocComment = false;
                                commentDepth = 0;
                                // Here the assignment is necessary for getting full comment
                                // getTokenStart() is a method of FlexLexer, which is the same as zzStartRead
                                // commentStart = getTokenStart();
                                commentStart = zzStartRead;
                            }
    {BLOCK_DOC_COMMENT_START}     {
                                commentStart = yytext().length();
                                originalState = yystate();
                                yybegin(BLOCK_COMMENT_INNER);
                                isInsideDocComment = true;
                                commentDepth = 0;
                                // Here the assignment is necessary for getting full comment
                                // getTokenStart() is a method of FlexLexer, which is the same as zzStartRead
                                // commentStart = getTokenStart();
                                commentStart = zzStartRead;
                            }
    {KEYWORD_COMMAND1}      {
          return KEYWORD_COMMAND1;
                            }
    {KEYWORD_COMMAND_PREFIX} {
          return KEYWORD_COMMAND_PREFIX;
                            }
    {KEYWORD_MODIFIER}      {
          return KEYWORD_MODIFIER;
                            }
    {KEYWORD_COMMAND2}      {
          return KEYWORD_COMMAND2;
                            }
    {KEYWORD_COMMAND3}      {
          return KEYWORD_COMMAND3;
                            }
    {KEYWORD_COMMAND4}      {
          return KEYWORD_COMMAND4;
                            }
    {KEYWORD_COMMAND5}      {
          return KEYWORD_COMMAND5;
                            }
    {KEYWORD_SORRY}      {
          return KEYWORD_SORRY;
                            }
    {template_trigger} {
        return TEMPLATE_TRIGGER;
    }
    {DEFAUTL_TYPE}      {
          return DEFAULT_TYPE;
                            }
    {IDENTIFIER}            {
          return IDENTIFIER;
                            }
    // left paren
    {left_paren}            {
          return LEFT_PAREN;
                            }
    // right paren
    {right_paren}           {
          return RIGHT_PAREN;
                            }
    // left bracket
    {left_bracket}          {
          return LEFT_BRACKET;
                            }
    // right bracket
    {right_bracket}         {
          return RIGHT_BRACKET;
                            }
    // braces
    {left_brace}            {
          return LEFT_BRACE;
                            }
    {right_brace}           {
          return RIGHT_BRACE;
                            }
    // unicode brackets
    {left_uni_bracket}      {
          return LEFT_UNI_BRACKET;
                            }
    {right_uni_bracket}     {
          return RIGHT_UNI_BRACKET;
                            }
    // dot
    {dot}                   {
          return DOT;
                            }

    // comparison symbols
    {MISC_COMPARISON_SYM}   {
          return MISC_COMPARISON_SYM;
                            }
    // plus symbols
    {MISC_PLUS_SYM}         {
          return MISC_PLUS_SYM;
                            }
    // multiply symbols
    {MISC_MULTIPLY_SYM}     {
          return MISC_MULTIPLY_SYM;
                            }
    // exponent symbols
    {MISC_EXPONENT_SYM}     {
          return MISC_EXPONENT_SYM;
                            }
    // arrow symbols
    {MISC_ARROW_SYM}        {
          return MISC_ARROW_SYM;
                            }

    . {
    return OTHER;
    }

}

<BLOCK_COMMENT_INNER> {


    {BlOCK_COMMENT_START} {
        commentDepth++;
    }

    {BLOCK_DOC_COMMENT_START} {
        commentDepth++;
    }

    {BlOCK_COMMENT_END} {
                            if (commentDepth > 0) {
                                commentDepth--;
                            } else {
                                // Here it's necessary to change zzStartRead, otherwise yytext() will return wrong range
                                zzStartRead = commentStart;
                                yybegin(originalState);
                                if (isInsideDocComment) {
                                    return DOC_COMMENT;
                                } else {
                                    return BLOCK_COMMENT;
                                }
                            }
                        }

    <<EOF>> {
                                // Here it's necessary to change zzStartRead, otherwise yytext() will return wrong range
                                zzStartRead = commentStart;
                                yybegin(originalState);
                                if (isInsideDocComment) {
                                    return DOC_COMMENT;
                                } else {
                                    return BLOCK_COMMENT;
                                }
    }

    [^] {}
}
