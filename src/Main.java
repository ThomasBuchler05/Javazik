import controller.ControleurPrincipal;

/**
 * Point d'entrée de l'application JavaZik.
 *
 * <p>Présente une boîte de dialogue de démarrage permettant à l'utilisateur
 * de choisir entre l'interface graphique Swing et l'interface console.
 * Selon le choix, un {@link ControleurPrincipal} est instancié dans le mode
 * approprié, puis lancé.</p>
 *
 * <ul>
 *   <li>Interface graphique : construction sur l'EDT, logique métier sur un
 *       thread séparé pour ne pas bloquer l'interface.</li>
 *   <li>Interface console : exécution directe sur le thread principal.</li>
 * </ul>
 */
public class Main {

    /**
     * Méthode principale de l'application.
     *
     * @param args arguments de la ligne de commande (non utilisés)
     */
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