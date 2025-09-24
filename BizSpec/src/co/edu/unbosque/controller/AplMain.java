package co.edu.unbosque.controller;

import co.edu.unbosque.view.BizSpecFrame;

public class AplMain {
	public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            BizSpecFrame view = new BizSpecFrame();
            new BizSpecController(view).init();   
            view.setVisible(true);
        });
    }
}

