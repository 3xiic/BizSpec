package co.edu.unbosque.model.lexer;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private final String src;
    private final int n;
    private int i = 0, line = 1, col = 1;
    private final List<Token> out = new ArrayList<>();

    public Tokenizer(String source) {
        this.src = source == null ? "" : source;
        this.n = this.src.length();
    }

    public List<Token> tokenize() {
        while (!eof()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r') { advance(); continue; }
            if (c == '\n') { add(TokenType.NEWLINE, "\n"); advanceLine(); continue; }
            if (c == '#') { skipComment(); continue; }

            switch (c) {
                case '{': addAndAdvance(TokenType.LBRACE, "{"); break;
                case '}': addAndAdvance(TokenType.RBRACE, "}"); break;
                case '(': addAndAdvance(TokenType.LPAREN, "("); break;
                case ')': addAndAdvance(TokenType.RPAREN, ")"); break;
                case ',': addAndAdvance(TokenType.COMMA, ","); break;
                case '.': addAndAdvance(TokenType.DOT, "."); break;
                case '+': addAndAdvance(TokenType.PLUS, "+"); break;
                case '-': addAndAdvance(TokenType.MINUS, "-"); break;
                case '*': addAndAdvance(TokenType.STAR, "*"); break;
                case '/':
                    if (peek2() == '/') { skipLine(); }
                    else addAndAdvance(TokenType.SLASH, "/");
                    break;
                case '=':
                    if (peek2() == '=') { advance(); advance(); add(TokenType.EQEQ, "=="); }
                    else { advance(); add(TokenType.EQ, "="); }
                    break;
                case '!':
                    if (peek2() == '=') { advance(); advance(); add(TokenType.NE, "!="); }
                    else throw error("Se esperaba '=' después de '!'");
                    break;
                case '>':
                    if (peek2() == '=') { advance(); advance(); add(TokenType.GE, ">="); }
                    else { advance(); add(TokenType.GT, ">"); }
                    break;
                case '<':
                    if (peek2() == '=') { advance(); advance(); add(TokenType.LE, "<="); }
                    else { advance(); add(TokenType.LT, "<"); }
                    break;
                case '"':
                    lexString();
                    break;
                default:
                    if (isDigit(c)) lexNumber();
                    else if (isIdentStart(c)) lexIdentOrKeyword();
                    else throw error("Carácter no reconocido: '" + c + "'");
            }
        }
        out.add(new Token(TokenType.EOF, "", line, col));
        return out;
    }

    private void lexString() {
        int startCol = col;
        StringBuilder sb = new StringBuilder();
        advance(); // consume "
        while (!eof()) {
            char c = peek();
            if (c == '"') { advance(); add(TokenType.STRING, sb.toString(), startCol); return; }
            if (c == '\n') throw errorAt(line, startCol, "String sin cerrar");
            if (c == '\\') {
                advance();
                if (eof()) throw error("Escape al final");
                char e = peek();
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    default: sb.append(e); break;
                }
                advance();
            } else { sb.append(c); advance(); }
        }
        throw errorAt(line, startCol, "String sin cerrar");
    }

    private void lexNumber() {
        int startCol = col;
        StringBuilder sb = new StringBuilder();
        while (!eof() && isDigit(peek())) { sb.append(peek()); advance(); }
        if (!eof() && peek() == '.' && isDigit(peekAhead(1))) {
            sb.append('.'); advance();
            while (!eof() && isDigit(peek())) { sb.append(peek()); advance(); }
        }
        add(TokenType.NUMBER, sb.toString(), startCol);
    }

    private void lexIdentOrKeyword() {
        int startCol = col;
        StringBuilder sb = new StringBuilder();
        sb.append(peek()); advance();
        while (!eof() && isIdentPart(peek())) { sb.append(peek()); advance(); }
        String lex = sb.toString();
        String lower = lex.toLowerCase();
        switch (lower) {
            case "rule": add(TokenType.RULE, lex, startCol); return;
            case "test": add(TokenType.TEST, lex, startCol); return;
            case "when": add(TokenType.WHEN, lex, startCol); return;
            case "then": add(TokenType.THEN, lex, startCol); return;
            case "given": add(TokenType.GIVEN, lex, startCol); return;
            case "expect": add(TokenType.EXPECT, lex, startCol); return;
            case "and": add(TokenType.AND, lex, startCol); return;
            case "or": add(TokenType.OR, lex, startCol); return;
            case "not": add(TokenType.NOT, lex, startCol); return;
            case "true": add(TokenType.TRUE, lex, startCol); return;
            case "false": add(TokenType.FALSE, lex, startCol); return;
            case "null": add(TokenType.NULL, lex, startCol); return;
            case "undefined": add(TokenType.UNDEFINED, lex, startCol); return;
            default:
                add(TokenType.IDENT, lex, startCol);
        }
    }

    private void skipComment() { while (!eof() && peek() != '\n') advance(); }
    private void skipLine() { while (!eof() && peek() != '\n') advance(); }

    private boolean eof() { return i >= n; }
    private char peek() { return src.charAt(i); }
    private char peek2() { return (i + 1 < n) ? src.charAt(i + 1) : '\0'; }
    private char peekAhead(int k) { return (i + k < n) ? src.charAt(i + k) : '\0'; }

    private void advance() { i++; col++; }
    private void advanceLine() { i++; line++; col = 1; }

    private void add(TokenType t, String lexeme) { out.add(new Token(t, lexeme, line, col)); }
    private void add(TokenType t, String lexeme, int startCol) { out.add(new Token(t, lexeme, line, startCol)); }
    private void addAndAdvance(TokenType t, String lexeme) { add(t, lexeme); advance(); }

    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isIdentStart(char c) { return Character.isLetter(c) || c == '_'; }
    private boolean isIdentPart(char c) { return Character.isLetterOrDigit(c) || c == '_'; }

    private RuntimeException error(String msg) { return new RuntimeException("[L" + line + " C" + col + "] " + msg); }
    private RuntimeException errorAt(int l, int c, String msg) { return new RuntimeException("[L" + l + " C" + c + "] " + msg); }
}
