package co.edu.unbosque.model.lexer;

public enum TokenType {
    // Symbols
    LBRACE, RBRACE, LPAREN, RPAREN, COMMA, DOT,
    // Operators
    EQ, EQEQ, NE, GE, LE, GT, LT, PLUS, MINUS, STAR, SLASH,
    // Literals & names
    NUMBER, STRING, IDENT,
    // Keywords
    RULE, TEST, WHEN, THEN, GIVEN, EXPECT, AND, OR, NOT,
    TRUE, FALSE, NULL, UNDEFINED,
    // Structure
    NEWLINE, EOF
}
