package co.edu.unbosque.view;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class BizSpecFrame extends JFrame {
    private final JTextArea editor = new JTextArea();
    private final JTable tokenTable = new JTable();
    private final JTextArea console = new JTextArea();
    private final JLabel status = new JLabel("Listo");
    private File currentFile = null;

    private Runnable onTokenize, onParse, onRunTests;

    public BizSpecFrame() {
        super("BizSpec Studio");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        buildUI();
        setSystemLnF();
    }

    // ==== wiring ====
    public void onTokenize(Runnable r) { this.onTokenize = r; }
    public void onParse(Runnable r) { this.onParse = r; }
    public void onRunTests(Runnable r) { this.onRunTests = r; }

    // ==== view API ====
    public String getSource() { return editor.getText(); }
    public void setTokens(TokenTableModel model) { tokenTable.setModel(model); }
    public void setStatus(String s) { status.setText(s); }
    public void showError(String s) { status.setText("Error"); console.setText(s); }
    public void clearConsole() { console.setText(""); }
    public void setConsole(String s) { console.setText(s); }
    public void appendConsole(String s) { console.append(s+ "\n"); }
    public void loadSample(String src) { editor.setText(src); }

    // ==== UI ====
    private void setSystemLnF() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); SwingUtilities.updateComponentTreeUI(this); }
        catch (Exception ignored) {}
    }

    private void buildUI() {
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        console.setEditable(false);

        // Izquierda: editor
        JScrollPane left = new JScrollPane(editor);

        // Derecha: tabla de tokens
        JScrollPane right = new JScrollPane(tokenTable);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        main.setResizeWeight(0.6);

        // Abajo: consola
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(new JScrollPane(console), BorderLayout.CENTER);
        bottom.add(status, BorderLayout.SOUTH);

        JSplitPane root = new JSplitPane(JSplitPane.VERTICAL_SPLIT, main, bottom);
        root.setResizeWeight(0.7);
        setContentPane(root);

        setJMenuBar(buildMenu());

        // Ctrl+S para guardar
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "save");
        editor.getActionMap().put("save", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(false); }
        });
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("Archivo");
        JMenu run = new JMenu("Ejecutar");

        JMenuItem open = new JMenuItem("Abrir…");
        JMenuItem save = new JMenuItem("Guardar");
        JMenuItem saveAs = new JMenuItem("Guardar como…");
        JMenuItem tokenize = new JMenuItem("Tokenizar");
        JMenuItem parse = new JMenuItem("Parsear");
        JMenuItem runTests = new JMenuItem("Correr Tests");

        open.addActionListener(e -> open());
        save.addActionListener(e -> save(false));
        saveAs.addActionListener(e -> save(true));
        tokenize.addActionListener(e -> { if (onTokenize != null) onTokenize.run(); });
        parse.addActionListener(e -> { if (onParse != null) onParse.run(); });
        runTests.addActionListener(e -> { if (onRunTests != null) onRunTests.run(); });

        file.add(open); file.add(save); file.add(saveAs);
        run.add(tokenize); run.add(parse); run.add(runTests);

        bar.add(file); bar.add(run);
        return bar;
    }

    private void open() {
        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter("BizSpec (*.biztest, *.bizspec, *.txt)", "biztest", "bizspec", "txt"));
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = ch.getSelectedFile();
            try {
                editor.setText(Files.readString(currentFile.toPath()));
                setStatus("Abierto: " + currentFile.getName());
                clearConsole();
            } catch (IOException ex) {
                showError("No se pudo abrir: " + ex.getMessage());
            }
        }
    }

    private void save(boolean forceDialog) {
        if (currentFile == null || forceDialog) {
            JFileChooser ch = new JFileChooser();
            ch.setFileFilter(new FileNameExtensionFilter("BizSpec (*.biztest)", "biztest"));
            if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            currentFile = ch.getSelectedFile();
            if (!currentFile.getName().contains(".")) {
                currentFile = new File(currentFile.getParentFile(), currentFile.getName()+".biztest");
            }
        }
        try (FileWriter fw = new FileWriter(currentFile)) {
            fw.write(editor.getText());
            setStatus("Guardado: " + currentFile.getName());
            clearConsole();
        } catch (IOException ex) {
            showError("No se pudo guardar: " + ex.getMessage());
        }
    }
}
