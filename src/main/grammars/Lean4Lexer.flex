package lean4ij.language;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
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
KEYWORD_COMMAND4        = class|def|lemma|example|theorem|instance|structure|set_option|vairable|infixr
KEYWORD_COMMAND5        = #check|#guard_msgs|#eval|#reduce|#synth|#help
KEYWORD_COMMAND6        = match|have|with|by|in|fun
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
colon_equal         = ":="
at_leftbracket      = "@["
attribute            = "attribute"

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
// MISC_MULTIPLY_SYM      =[∘∩∧⊗⊘⊙⊚⊛⊠⊡⊓∗∙∤⅋≀⊼⋄⋆⋇⋉⋊⋋⋌⋏⋒⟑⦸⦼⦾⦿⧶⧷⨇⨰⨱⨲⨳⨴⨵⨶⨷⨸⨻⨼⨽⩀⩃⩄⩋⩍⩎⩑⩓⩕⩘⩚⩜⩞⩟⩠⫛⊍▷⨝⟕⟖⟗]
// the following is converted from the above using
// def f(s): print("".join(['\\u'+hex(ord(i))[2:] for i in s[1:-1]]))
MISC_MULTIPLY_SYM      =[\u2218\u2229\u2227\u2297\u2298\u2299\u229a\u229b\u22a0\u22a1\u2293\u2217\u2219\u2224\u214b\u2240\u22bc\u22c4\u22c6\u22c7\u22c9\u22ca\u22cb\u22cc\u22cf\u22d2\u27d1\u29b8\u29bc\u29be\u29bf\u29f6\u29f7\u2a07\u2a30\u2a31\u2a32\u2a33\u2a34\u2a35\u2a36\u2a37\u2a38\u2a3b\u2a3c\u2a3d\u2a40\u2a43\u2a44\u2a4b\u2a4d\u2a4e\u2a51\u2a53\u2a55\u2a58\u2a5a\u2a5c\u2a5e\u2a5f\u2a60\u2adb\u228d\u25b7\u2a1d\u27d5\u27d6\u27d7]
MISC_EXPONENT_SYM      =[↑↓⇵⟰⟱⤈⤉⤊⤋⤒⤓⥉⥌⥍⥏⥑⥔⥕⥘⥙⥜⥝⥠⥡⥣⥥⥮⥯￪￬]
// MISC_ARROW_SYM      =[←→↔↚↛↞↠↢↣↦↤↮⇎⇍⇏⇐⇒⇔⇴⇶⇷⇸⇹⇺⇻⇼⇽⇾⇿⟵⟶⟷⟹⟺⟻⟼⟽⟾⟿⤀⤁⤂⤃⤄⤅⤆⤇⤌⤍⤎⤏⤐⤑⤔⤕⤖⤗⤘⤝⤞⤟⤠⥄⥅⥆⥇⥈⥊⥋⥎⥐⥒⥓⥖⥗⥚⥛⥞⥟⥢⥤⥦⥧⥨⥩⥪⥫⥬⥭⥰⧴⬱⬰⬲⬳⬴⬵⬶⬷⬸⬹⬺⬻⬼⬽⬾⬿⭀⭁⭂⭃⭄⭇⭈⭉⭊⭋⭌￩￫⇜⇝↜↝↩↪↫↬↼↽⇀⇁⇄⇆⇇⇉⇋⇌⇚⇛⇠⇢]
// the following is converted from the above using
// def f(s): print("".join(['\\u'+hex(ord(i))[2:] for i in s[1:-1]]))
MISC_ARROW_SYM      =[\u2190\u2192\u2194\u219a\u219b\u219e\u21a0\u21a2\u21a3\u21a6\u21a4\u21ae\u21ce\u21cd\u21cf\u21d0\u21d2\u21d4\u21f4\u21f6\u21f7\u21f8\u21f9\u21fa\u21fb\u21fc\u21fd\u21fe\u21ff\u27f5\u27f6\u27f7\u27f9\u27fa\u27fb\u27fc\u27fd\u27fe\u27ff\u2900\u2901\u2902\u2903\u2904\u2905\u2906\u2907\u290c\u290d\u290e\u290f\u2910\u2911\u2914\u2915\u2916\u2917\u2918\u291d\u291e\u291f\u2920\u2944\u2945\u2946\u2947\u2948\u294a\u294b\u294e\u2950\u2952\u2953\u2956\u2957\u295a\u295b\u295e\u295f\u2962\u2964\u2966\u2967\u2968\u2969\u296a\u296b\u296c\u296d\u2970\u29f4\u2b31\u2b30\u2b32\u2b33\u2b34\u2b35\u2b36\u2b37\u2b38\u2b39\u2b3a\u2b3b\u2b3c\u2b3d\u2b3e\u2b3f\u2b40\u2b41\u2b42\u2b43\u2b44\u2b47\u2b48\u2b49\u2b4a\u2b4b\u2b4c\uffe9\uffeb\u21dc\u21dd\u219c\u219d\u21a9\u21aa\u21ab\u21ac\u21bc\u21bd\u21c0\u21c1\u21c4\u21c6\u21c7\u21c9\u21cb\u21cc\u21da\u21db\u21e0\u21e2]

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
    {underscore} {
        return PLACEHOLDER;
    }
    {at_leftbracket}        {
        return ATTRIBUTE_START;
    }
    {attribute}    {
        return ATTRIBUTE;
    }
    {colon}    {
        return COLON;
    }
    {colon_equal} {
        return ASSIGN;
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
    {KEYWORD_COMMAND6}      {
          return KEYWORD_COMMAND6;
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
