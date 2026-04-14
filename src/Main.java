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
            // Crée la fenêtre sur l'EDT, puis lance la logique sur un thread séparé
            // pour ne pas bloquer l'EDT pendant les saisies utilisateur
            javax.swing.SwingUtilities.invokeLater(() -> {
                ControleurPrincipal controleur = new ControleurPrincipal(true);
                new Thread(controleur::lancer).start();
            });
        } else {
            ControleurPrincipal controleur = new ControleurPrincipal(false);
            controleur.lancer();
        }
    }
}