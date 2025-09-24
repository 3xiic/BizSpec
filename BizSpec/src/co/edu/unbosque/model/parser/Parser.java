package co.edu.unbosque.model.parser;

import java.util.List;

import co.edu.unbosque.model.ast.BizNode;
import co.edu.unbosque.model.lexer.Token;
import co.edu.unbosque.model.lexer.TokenType;

public class Parser {
    private final List<Token> toks;
    private int i = 0;

    public Parser(List<Token> tokens) { this.toks = tokens; }

    private Token t() { return toks.get(i); }
    private TokenType tt() { return t().type; }
    private boolean is(TokenType tp) { return tt() == tp; }
    private boolean isLex(String lx) { return t().lexeme.equals(lx); }

    private RuntimeException err(String msg) {
        return new RuntimeException("[L" + t().line + " C" + t().col + "] " + msg +
                " (en " + tt() + ":'" + t().lexeme + "')");
    }
    private Token consume(TokenType tp, String msg) {
        if (tt() != tp) throw err(msg);
        return toks.get(i++);
    }
    private Token consumeLex(String lx, String msg) {
        if (!isLex(lx)) throw err(msg + " (esperado '" + lx + "')");
        return toks.get(i++);
    }
    private boolean consumeIf(TokenType tp) { if (tt() == tp) { i++; return true; } return false; }
    private boolean consumeIfLex(String lx) { if (isLex(lx)) { i++; return true; } return false; }

    public BizNode parseProgram() {
        BizNode prog = node("Program", "", t());
        while (!is(TokenType.EOF)) {
            while (consumeIf(TokenType.NEWLINE));
            if (is(TokenType.EOF)) break;
            if (isLex("rule")) prog.add(parseRule());
            else if (isLex("test")) prog.add(parseTest());
            else throw err("Se esperaba 'rule' o 'test'");
        }
        consume(TokenType.EOF, "Se esperaba EOF");
        return prog;
    }

    private BizNode parseRule() {
        Token start = consumeLex("rule", "Se esperaba 'rule'");
        Token name = consume(TokenType.STRING, "Se esperaba nombre de regla");
        consumeLex("when", "Se esperaba 'when'");
        String cond = readUntilKeyword("then");
        consumeLex("then", "Se esperaba 'then'");
        Token target = consume(TokenType.IDENT, "Se esperaba variable");
        consumeLex("=", "Se esperaba '='");
        String value = readUntilLineEnd();
        consumeIf(TokenType.NEWLINE);

        BizNode rule = node("Rule", stripQuotes(name.lexeme), start);
        rule.add(node("When", cond, start));
        BizNode set = node("Set","", start);
        set.add(node("Target", target.lexeme, target));
        set.add(node("Value", value, start));
        rule.add(set);
        return rule;
    }

    private BizNode parseTest() {
        Token start = consumeLex("test", "Se esperaba 'test'");
        Token name = consume(TokenType.STRING, "Se esperaba nombre de test");
        consumeLex("{", "Se esperaba '{'");
        while (consumeIf(TokenType.NEWLINE));
        BizNode test = node("Test", stripQuotes(name.lexeme), start);

        while (!isLex("}")) {
            if (isLex("given")) test.add(parseGiven());
            else if (isLex("expect")) test.add(parseExpect());
            else if (consumeIf(TokenType.NEWLINE));
            else throw err("Se esperaba 'given' o 'expect'");
        }
        consumeLex("}", "Se esperaba '}'");
        consumeIf(TokenType.NEWLINE);
        return test;
    }

    private BizNode parseGiven() {
        Token start = consumeLex("given", "Se esperaba 'given'");
        String assign = readUntilLineEnd();
        consumeIf(TokenType.NEWLINE);
        return node("Given", assign, start);
    }

    private BizNode parseExpect() {
        Token start = consumeLex("expect", "Se esperaba 'expect'");
        String expr = readUntilLineEnd();
        consumeIf(TokenType.NEWLINE);
        return node("Expect", expr, start);
    }

    // --- helpers texto (MVP expresiones como string) ---
    private String readUntilKeyword(String kw) {
        StringBuilder sb = new StringBuilder();
        while (!isLex(kw) && tt() != TokenType.NEWLINE && tt() != TokenType.EOF) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(t().lexeme); i++;
        }
        return sb.toString();
    }
    private String readUntilLineEnd() {
        StringBuilder sb = new StringBuilder();
        while (tt() != TokenType.NEWLINE && tt() != TokenType.EOF) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(t().lexeme); i++;
        }
        return sb.toString();
    }
    private BizNode node(String kind, String text, Token at) {
        return new BizNode(kind, text, at.line, at.col);
    }
    private static String stripQuotes(String s) {
        return (s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
                ? s.substring(1, s.length()-1) : s;
    }
}
