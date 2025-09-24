package co.edu.unbosque.view;


import javax.swing.table.AbstractTableModel;

import co.edu.unbosque.model.lexer.Token;

import java.util.List;

public class TokenTableModel extends AbstractTableModel {
    private final List<Token> toks;
    private final String[] cols = {"#", "Type", "Lexeme", "Line", "Col"};

    public TokenTableModel(List<Token> toks) { this.toks = toks; }

    @Override public int getRowCount() { return toks.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }

    @Override public Object getValueAt(int r, int c) {
        Token t = toks.get(r);
        return switch (c) {
            case 0 -> r;
            case 1 -> t.type;
            case 2 -> t.lexeme;
            case 3 -> t.line;
            case 4 -> t.col;
            default -> "";
        };
    }
}
