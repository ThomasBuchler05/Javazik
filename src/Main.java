import controller.ControleurPrincipal;

public class Main {
    public static void main(String[] args) {
        String[] options = {"Interface graphique (Swing)", "Interface console"};
        int choix = javax.swing.JOptionPane.showOptionDialog(
                null,
                "Choisissez l'interface de lancement :",
                "JavaZik - Démarrage",
                javax.swing.JOptionPane.DEFAULT_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        // choix == -1 si l'utilisateur ferme la fenêtre => on quitte
        if (choix < 0) return;

        boolean graphique = (choix == 0);

        if (graphique) {
            // Lance l'interface graphique sur le thread Swing
            javax.swing.SwingUtilities.invokeLater(() -> {
                ControleurPrincipal controleur = new ControleurPrincipal(true);
                controleur.lancer();
            });
        } else {
            ControleurPrincipal controleur = new ControleurPrincipal(false);
            controleur.lancer();
        }
    }
}