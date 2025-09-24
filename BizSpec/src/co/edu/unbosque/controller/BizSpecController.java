package co.edu.unbosque.controller;


import java.util.List;

import co.edu.unbosque.model.ast.BizNode;
import co.edu.unbosque.model.lexer.Evaluator;
import co.edu.unbosque.model.lexer.Token;
import co.edu.unbosque.model.lexer.Tokenizer;
import co.edu.unbosque.model.parser.Parser;
import co.edu.unbosque.view.BizSpecFrame;
import co.edu.unbosque.view.TokenTableModel;

public class BizSpecController {

    private final BizSpecFrame view;

    public BizSpecController(BizSpecFrame view) {
        this.view = view;
    }

    public void init() {
        view.onTokenize(this::tokenize);
        view.onParse(this::parse);
        view.onRunTests(this::runTests);
        view.loadSample(sample());
    }

    private void tokenize() {
        try {
            Tokenizer tz = new Tokenizer(view.getSource());
            List<Token> toks = tz.tokenize();
            view.setTokens(new TokenTableModel(toks));
            view.setStatus("Tokens: " + toks.size());
            view.clearConsole();
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    private void parse() {
        try {
            Tokenizer tz = new Tokenizer(view.getSource());
            List<Token> toks = tz.tokenize();
            Parser p = new Parser(toks);
            BizNode ast = p.parseProgram();

            view.setTokens(new TokenTableModel(toks));
            view.setConsole(ast.toString());
            view.setStatus("Parse OK");
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    private void runTests() {
        try {
            Tokenizer tz = new Tokenizer(view.getSource());
            List<Token> toks = tz.tokenize();
            Parser p = new Parser(toks);
            BizNode ast = p.parseProgram();

            String output = Evaluator.run(ast);
            view.setTokens(new TokenTableModel(toks));
            view.setConsole(output);
            view.setStatus("Tests ejecutados");
        } catch (Exception ex) {
            view.showError(ex.getMessage());
        }
    }

    private String sample() {
        return String.join("\n",
            "# Reglas de ejemplo",
            "rule \"Envío gratis\" when carrito.total >= 100000 then envio = 0",
            "rule \"Envío base\"   when envio == null or envio == undefined then envio = 9900",
            "rule \"Envío internacional\" when destino == \"USA\" then envio = 50000",
            "rule \"Envío promocional\" when cupon == \"ENVIO10\" then envio = 1000",
            "",
            "# Tests",
            "test \"Orden 123\" {",
            "  given carrito.total=120000",
            "  expect envio == 0",
            "}"
        );
    }
}
