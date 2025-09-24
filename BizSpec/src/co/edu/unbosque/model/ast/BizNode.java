package co.edu.unbosque.model.ast;

import java.util.ArrayList;
import java.util.List;

public class BizNode {
    public final String kind;
    public final String text;
    public final int line, col;
    public final List<BizNode> children = new ArrayList<>();

    public BizNode(String kind, String text, int line, int col) {
        this.kind = kind;
        this.text = text;
        this.line = line;
        this.col = col;
    }

    public BizNode add(BizNode n) { children.add(n); return this; }

    @Override
    public String toString() {
        return kind + (text == null || text.isEmpty() ? "" : "(" + text + ")")
                + "@" + line + ":" + col;
    }
}
