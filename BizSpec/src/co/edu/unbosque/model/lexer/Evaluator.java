package co.edu.unbosque.model.lexer;



import java.util.*;

import co.edu.unbosque.model.ast.BizNode;

/**
 * Evaluador minimalista de BizSpec:
 * - Aplica solo la primera regla válida para cada target
 * - En consola muestra:
 *   1. La regla aplicada (solo esa)
 *   2. Los expects con PASS/FAIL
 */
public class Evaluator {

    public static String run(BizNode program) {
        StringBuilder out = new StringBuilder();
        List<Rule> rules = extractRules(program);
        List<TestCase> tests = extractTests(program);

        out.append("== BizSpec :: Run Tests ==\n");
        if (tests.isEmpty()) {
            out.append("(No hay tests)\n");
            return out.toString();
        }

        int passed = 0, failed = 0;

        for (TestCase tc : tests) {
            out.append("\nTest: ").append(tc.name).append("\n");

            Map<String, Object> env = new LinkedHashMap<>();

            // 1) cargar givens
            for (String assign : tc.givens) {
                for (String part : assign.split(",")) {
                    String s = part.trim();
                    if (s.isEmpty()) continue;
                    String[] kv = s.split("=", 2);
                    if (kv.length != 2) continue;
                    String key = kv[0].trim();
                    String rhs = kv[1].trim();
                    Object val = evalExpr(rhs, env);
                    putDeep(env, key, val);
                }
            }

            // 2) aplicar solo la primera regla válida por target
            Set<String> assignedTargets = new HashSet<>();
            for (Rule r : rules) {
                if (assignedTargets.contains(r.target)) continue;
                Object cond = evalExpr(r.condition, env);
                if (truthy(cond)) {
                    Object rhs = evalExpr(r.valueExpr, env);
                    putDeep(env, r.target, rhs);
                    assignedTargets.add(r.target);
                    out.append("  ✓ rule \"").append(r.name).append("\" aplicada → ")
                       .append(r.target).append(" = ").append(stringify(rhs)).append("\n");
                }
            }

            // 3) validar expects
            boolean allOk = true;
            for (String ex : tc.expects) {
                Object res = evalExpr(ex, env);
                boolean ok = truthy(res);
                allOk &= ok;
                out.append(ok ? "  ✓ " : "  ✗ ")
                   .append(ex)
                   .append(ok ? " → PASS\n" : " → FAIL\n");
            }

            if (allOk) { passed++; out.append("→ RESULTADO: PASS\n"); }
            else       { failed++; out.append("→ RESULTADO: FAIL\n"); }
        }

        out.append("\nResumen: ").append(passed).append(" PASS, ").append(failed).append(" FAIL\n");
        return out.toString();
    }

    // ===== AST → Reglas / Tests =====
    private static List<Rule> extractRules(BizNode program) {
        List<Rule> rs = new ArrayList<>();
        for (BizNode n : program.children) {
            if (!"Rule".equals(n.kind)) continue;
            String name = n.text;
            String when = childText(n, "When");
            BizNode set = child(n, "Set");
            String target = childText(set, "Target");
            String value  = childText(set, "Value");
            rs.add(new Rule(name, when, target, value));
        }
        return rs;
    }

    private static List<TestCase> extractTests(BizNode program) {
        List<TestCase> ts = new ArrayList<>();
        for (BizNode n : program.children) {
            if (!"Test".equals(n.kind)) continue;
            TestCase t = new TestCase(n.text);
            for (BizNode c : n.children) {
                if ("Given".equals(c.kind))  t.givens.add(c.text);
                if ("Expect".equals(c.kind)) t.expects.add(c.text);
            }
            ts.add(t);
        }
        return ts;
    }

    private static BizNode child(BizNode n, String kind) {
        if (n == null) return null;
        for (BizNode c : n.children) if (kind.equals(c.kind)) return c;
        return null;
    }
    private static String childText(BizNode n, String kind) {
        BizNode c = child(n, kind);
        return (c == null) ? "" : c.text;
    }

    // ===== Entorno jerárquico (a.b.c) =====
    @SuppressWarnings("unchecked")
    private static void putDeep(Map<String,Object> env, String dotted, Object value) {
        String[] parts = dotted.split("\\.");
        Map<String,Object> cur = env;
        for (int i=0;i<parts.length-1;i++) {
            String k = parts[i].trim();
            Object nxt = cur.get(k);
            if (!(nxt instanceof Map)) {
                nxt = new LinkedHashMap<String,Object>();
                cur.put(k, nxt);
            }
            cur = (Map<String, Object>) nxt;
        }
        cur.put(parts[parts.length-1].trim(), value);
    }

    @SuppressWarnings("unchecked")
    private static Object getDeep(Map<String,Object> env, String dotted) {
        if (!dotted.contains(".")) return env.get(dotted);
        String[] parts = dotted.split("\\.");
        Object cur = env;
        for (String p : parts) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<String,Object>)cur).get(p);
            if (cur == null) return null;
        }
        return cur;
    }

    // ===== Evaluación de expresiones con Tokenizer =====
    private static Object evalExpr(String expr, Map<String,Object> env) {
        Tokenizer tz = new Tokenizer(expr);
        List<Token> toks = tz.tokenize();
        ArrayList<Token> cleaned = new ArrayList<>();
        for (Token t : toks) if (t.type != TokenType.NEWLINE) cleaned.add(t);
        return new ExprEval(cleaned, env).parse();
    }

    private static final class ExprEval {
        private final List<Token> t;
        private final Map<String,Object> env;
        private int i = 0;

        ExprEval(List<Token> tokens, Map<String,Object> env) { this.t = tokens; this.env = env; }

        private Token tok() { return t.get(i); }
        private boolean is(TokenType tp) { return tok().type == tp; }
        private Token eat(TokenType tp, String msg) {
            if (!is(tp)) throw new RuntimeException(msg + " en " + tok().type + " '" + tok().lexeme + "'");
            return t.get(i++);
        }
        private boolean eatIf(TokenType tp) { if (is(tp)) { i++; return true; } return false; }

        Object parse() {
            Object v = or();
            eat(TokenType.EOF, "Se esperaba EOF en expresión");
            return v;
        }

        // or → and (OR and)*
        private Object or() {
            Object left = and();
            while (eatIf(TokenType.OR)) {
                Object right = and();
                left = truthy(left) || truthy(right);
            }
            return left;
        }

        // and → cmp (AND cmp)*
        private Object and() {
            Object left = cmp();
            while (eatIf(TokenType.AND)) {
                Object right = cmp();
                left = truthy(left) && truthy(right);
            }
            return left;
        }

        // cmp → add ((==|!=|>=|<=|>|<) add)*
        private Object cmp() {
            Object left = add();
            while (true) {
                if (eatIf(TokenType.EQEQ)) {
                    Object right = add();
                    left = Objects.equals(left, right);
                } else if (eatIf(TokenType.NE)) {
                    Object right = add();
                    left = !Objects.equals(left, right);
                } else if (eatIf(TokenType.GE)) {
                    Object right = add();
                    left = compare(left, right) >= 0;
                } else if (eatIf(TokenType.LE)) {
                    Object right = add();
                    left = compare(left, right) <= 0;
                } else if (eatIf(TokenType.GT)) {
                    Object right = add();
                    left = compare(left, right) > 0;
                } else if (eatIf(TokenType.LT)) {
                    Object right = add();
                    left = compare(left, right) < 0;
                } else break;
            }
            return left;
        }

        // add → mul ((+|-) mul)*
        private Object add() {
            Object left = mul();
            while (true) {
                if (eatIf(TokenType.PLUS)) {
                    Object right = mul();
                    left = num(left) + num(right);
                } else if (eatIf(TokenType.MINUS)) {
                    Object right = mul();
                    left = num(left) - num(right);
                } else break;
            }
            return left;
        }

        // mul → unary ((*|/) unary)*
        private Object mul() {
            Object left = unary();
            while (true) {
                if (eatIf(TokenType.STAR)) {
                    Object right = unary();
                    left = num(left) * num(right);
                } else if (eatIf(TokenType.SLASH)) {
                    Object right = unary();
                    left = num(left) / num(right);
                } else break;
            }
            return left;
        }

        // unary → NOT unary | MINUS unary | primary
        private Object unary() {
            if (eatIf(TokenType.NOT)) {
                return !truthy(unary());
            } else if (eatIf(TokenType.MINUS)) {
                return -num(unary());
            }
            return primary();
        }

        private Object primary() {
            if (eatIf(TokenType.NUMBER)) {
                String s = t.get(i-1).lexeme;
                return s.contains(".") ? Double.parseDouble(s) : Integer.parseInt(s);
            }
            if (eatIf(TokenType.STRING)) return t.get(i-1).lexeme;
            if (eatIf(TokenType.TRUE)) return true;
            if (eatIf(TokenType.FALSE)) return false;
            if (eatIf(TokenType.NULL) || eatIf(TokenType.UNDEFINED)) return null;
            if (eatIf(TokenType.LPAREN)) {
                Object v = or();
                eat(TokenType.RPAREN, "Falta ')'");
                return v;
            }
            if (eatIf(TokenType.IDENT)) {
                StringBuilder sb = new StringBuilder(t.get(i-1).lexeme);
                while (eatIf(TokenType.DOT)) {
                    Token id = eat(TokenType.IDENT, "Se esperaba IDENT después de '.'");
                    sb.append('.').append(id.lexeme);
                }
                return getDeep(env, sb.toString());
            }
            throw new RuntimeException("Expresión inválida cerca de " + tok().type + " '" + tok().lexeme + "'");
        }

        private static double num(Object v) {
            if (v == null) return 0.0;
            if (v instanceof Integer i) return i.doubleValue();
            if (v instanceof Long l) return l.doubleValue();
            if (v instanceof Double d) return d;
            if (v instanceof Float f) return f.doubleValue();
            if (v instanceof String s) {
                try { return Double.parseDouble(s); } catch (Exception ignored) { return 0.0; }
            }
            if (v instanceof Boolean b) return b ? 1.0 : 0.0;
            return 0.0;
        }

        @SuppressWarnings({"rawtypes","unchecked"})
        private static int compare(Object a, Object b) {
            if (a == null && b == null) return 0;
            if (a == null) return -1;
            if (b == null) return 1;
            if (a instanceof Number || b instanceof Number) {
                double da = num(a), db = num(b);
                return Double.compare(da, db);
            }
            if (a instanceof String sa && b instanceof String sb) {
                return sa.compareTo(sb);
            }
            if (a instanceof Comparable ca && b.getClass().isAssignableFrom(a.getClass())) {
                return ca.compareTo(b);
            }
            return String.valueOf(a).compareTo(String.valueOf(b));
        }
    }

    // ===== utils =====
    private static boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0.0;
        if (v instanceof String s) return !s.isEmpty();
        return true;
    }
    private static String stringify(Object v) { return v == null ? "null" : v.toString(); }

    // ===== DTOs =====
    private record Rule(String name, String condition, String target, String valueExpr) {}
    private static class TestCase {
        final String name;
        final List<String> givens = new ArrayList<>();
        final List<String> expects = new ArrayList<>();
        TestCase(String name) { this.name = name; }
    }
}
