package view;

import model.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Vue graphique Swing pour JavaZik.
 *
 * Architecture :
 *   - Fenêtre principale en mode "maximisée" (plein écran windowed)
 *   - Sidebar gauche (260 px) — contenu contextuel selon l'état de session
 *     (déconnecté / visiteur / client / admin)
 *   - Zone de contenu droite avec CardLayout — un JPanel par écran
 *
 * Synchronisation EDT / thread contrôleur :
 *   Les méthodes comme {@link #afficherMenuPrincipal()} sont appelées depuis
 *   le thread du contrôleur et doivent bloquer jusqu'à ce qu'un clic ait lieu
 *   sur l'EDT. On utilise pour cela la classe interne {@link InputRequest}
 *   basée sur une {@link SynchronousQueue} : le thread contrôleur attend sur
 *   {@code await()}, l'EDT livre la valeur via {@code submit(valeur)}.
 */
public final class VueGraphique extends VueConsole {

    // ==================== ÉTATS DE SESSION ====================

    public enum SessionState { DECONNECTE, VISITEUR, CLIENT, ADMIN }

    private SessionState sessionState = SessionState.DECONNECTE;
    private String nomUtilisateur = null; // null si non connecté

    // ==================== COMPOSANTS SWING ====================

    private JFrame fenetre;
    private JPanel sidebarPanel;
    private JLabel labelUser;
    private JPanel contentPanel;
    private CardLayout contentLayout;

    /** Liste des boutons sidebar courants, pour gérer l'état "actif". */
    private final List<SidebarButton> boutonsSidebar = new ArrayList<>();
    /** Carte actuellement affichée. */
    private String cardCourante = "accueil";

    /**
     * Référence vers la saisie utilisateur en cours. Utilisée par le
     * WindowListener pour injecter une valeur de "fermeture" si l'utilisateur
     * clique la croix pendant qu'un {@code await()} est en cours.
     */
    private final AtomicReference<InputRequest<?>> requeteCourante = new AtomicReference<>();

    // ==================== CONSTRUCTEUR ====================

    public VueGraphique() {
        // Construction sur l'EDT pour éviter tout problème de thread-safety Swing
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                buildUI();
            } else {
                SwingUtilities.invokeAndWait(this::buildUI);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur construction UI", e);
        }
    }

    private final void buildUI() {
        fenetre = new JFrame("JAVAZIK");
        // On gère la fermeture manuellement pour injecter une valeur "quitter"
        // au contrôleur s'il est en train d'attendre une saisie.
        fenetre.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        fenetre.setMinimumSize(new Dimension(1000, 650));
        fenetre.setLayout(new BorderLayout());
        fenetre.setExtendedState(JFrame.MAXIMIZED_BOTH);

        fenetre.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });

        // Sidebar
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(Styles.TEAL);
        sidebarPanel.setPreferredSize(new Dimension(Styles.SIDEBAR_WIDTH, 0));
        sidebarPanel.setBorder(new EmptyBorder(Styles.PADDING_LG, 0, Styles.PADDING_LG, 0));
        fenetre.add(sidebarPanel, BorderLayout.WEST);

        // Zone de contenu avec CardLayout
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setBackground(Styles.BG_MAIN);
        fenetre.add(contentPanel, BorderLayout.CENTER);

        registerCards();
        rebuildSidebar();

        fenetre.setLocationRelativeTo(null);
        fenetre.setVisible(true);
    }

    /**
     * Appelé quand l'utilisateur clique la croix de la fenêtre.
     * Si une saisie est en cours côté contrôleur, on lui envoie une valeur
     * d'annulation ; sinon, on ferme directement.
     */
    private void handleWindowClosing() {
        InputRequest<?> r = requeteCourante.get();
        if (r != null) {
            // Envoi d'une valeur spéciale : le contrôleur la traitera comme
            // "retour menu principal" puis recevra 5 (quitter) au prochain
            // afficherMenuPrincipal().
            r.cancelWithDefault();
        }
        // Dispose + exit proprement
        fenetre.dispose();
        System.exit(0);
    }

    // ==================== ENREGISTREMENT DES CARTES ====================

    private final void registerCards() {
        contentPanel.add(buildAccueilCard(),         "accueil");
        contentPanel.add(placeholder("Catalogue",    "Parcours des morceaux, albums, artistes et groupes."), "catalogue");
        contentPanel.add(placeholder("Écouter",      "Lecture d'un morceau au choix."),                      "ecoute");

        contentPanel.add(placeholder("Connexion administrateur", "Saisissez vos identifiants admin."),      "connexionAdmin");
        contentPanel.add(placeholder("Connexion client",         "Saisissez vos identifiants abonné."),     "connexionClient");
        contentPanel.add(placeholder("Créer un compte client",   "Inscription en tant qu'abonné."),         "inscription");

        contentPanel.add(placeholder("Mes playlists",  "Créer, renommer, supprimer, ajouter des morceaux."),"playlists");
        contentPanel.add(placeholder("Mon historique", "Consultation des morceaux récemment écoutés."),     "historique");

        contentPanel.add(placeholder("Gérer le catalogue", "Ajouter ou supprimer morceaux, albums, artistes, groupes."), "gestionCatalogue");
        contentPanel.add(placeholder("Gérer les comptes", "Suspendre, réactiver ou supprimer un compte abonné."),        "gestionComptes");
        contentPanel.add(placeholder("Statistiques",      "Vue d'ensemble du catalogue et de l'activité."),              "statistiques");
    }

    // ==================== CONSTRUCTION DE LA SIDEBAR (contextuelle) ====================

    /**
     * Reconstruit entièrement la sidebar en fonction de sessionState.
     * Appelé au démarrage et à chaque changement d'état.
     * IMPORTANT : doit être appelé sur l'EDT.
     */
    private final void rebuildSidebar() {
        sidebarPanel.removeAll();
        boutonsSidebar.clear();

        sidebarPanel.add(buildLogoHeader());
        sidebarPanel.add(Box.createVerticalStrut(Styles.PADDING_MD));
        sidebarPanel.add(buildUserHeader());
        sidebarPanel.add(Box.createVerticalStrut(Styles.PADDING_LG));

        sidebarPanel.add(buildSeparator());
        sidebarPanel.add(Box.createVerticalStrut(Styles.PADDING_MD));

        switch (sessionState) {
            case DECONNECTE:
                addSidebarButton("Accueil",                  "accueil");
                addSidebarButton("Connexion administrateur", "connexionAdmin");
                addSidebarButton("Connexion client",         "connexionClient");
                addSidebarButton("Créer un compte",          "inscription");
                addSidebarButton("Continuer en visiteur",    "visiteurAction"); // action, pas une carte
                break;
            case VISITEUR:
                addSidebarButton("Accueil",    "accueil");
                addSidebarButton("Catalogue",  "catalogue");
                addSidebarButton("Écouter",    "ecoute");
                break;
            case CLIENT:
                addSidebarButton("Accueil",       "accueil");
                addSidebarButton("Catalogue",     "catalogue");
                addSidebarButton("Mes playlists", "playlists");
                addSidebarButton("Écouter",       "ecoute");
                addSidebarButton("Historique",    "historique");
                break;
            case ADMIN:
                addSidebarButton("Accueil",             "accueil");
                addSidebarButton("Catalogue",           "catalogue");
                addSidebarButton("Gérer le catalogue",  "gestionCatalogue");
                addSidebarButton("Gérer les comptes",   "gestionComptes");
                addSidebarButton("Statistiques",        "statistiques");
                break;
        }

        sidebarPanel.add(Box.createVerticalGlue());

        sidebarPanel.add(buildSeparator());
        sidebarPanel.add(Box.createVerticalStrut(Styles.PADDING_MD));

        if (sessionState == SessionState.DECONNECTE) {
            sidebarPanel.add(buildSidebarActionButton("Quitter", "quitter"));
        } else {
            sidebarPanel.add(buildSidebarActionButton("Se déconnecter", "deconnexion"));
        }

        highlightActiveButton();
        refreshSidebarButtonsState();

        sidebarPanel.revalidate();
        sidebarPanel.repaint();
    }

    private JPanel buildLogoHeader() {
        JPanel p = transparentPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT, Styles.PADDING_LG, 0));
        JLabel logo = new JLabel("JAVAZIK  \u266A");
        logo.setFont(Styles.FONT_LOGO);
        logo.setForeground(Styles.TEXT_ON_TEAL);
        p.add(logo);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return p;
    }

    private JPanel buildUserHeader() {
        JPanel p = transparentPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(0, Styles.PADDING_LG, 0, Styles.PADDING_LG));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        labelUser = new JLabel(labelForSession());
        labelUser.setFont(Styles.FONT_SMALL);
        labelUser.setForeground(new Color(255, 255, 255, 200));
        labelUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(labelUser);

        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return p;
    }

    private String labelForSession() {
        switch (sessionState) {
            case DECONNECTE: return "Non connecté";
            case VISITEUR:   return "Mode visiteur";
            case CLIENT:     return "Connecté : " + (nomUtilisateur != null ? nomUtilisateur : "abonné");
            case ADMIN:      return "Administrateur : " + (nomUtilisateur != null ? nomUtilisateur : "admin");
            default:         return "";
        }
    }

    private JPanel buildSeparator() {
        JPanel sep = new JPanel();
        sep.setBackground(new Color(255, 255, 255, 60));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setPreferredSize(new Dimension(Styles.SIDEBAR_WIDTH, 1));
        return sep;
    }

    /**
     * Ajoute un bouton sidebar. Le {@code actionKey} est une chaîne qui
     * identifie le bouton (soit une carte de CardLayout, soit une action
     * spéciale comme "quitter", "deconnexion", "visiteurAction").
     */
    private final void addSidebarButton(String label, String actionKey) {
        SidebarButton b = new SidebarButton(label, actionKey);
        b.addActionListener(e -> onSidebarClick(actionKey));
        boutonsSidebar.add(b);
        sidebarPanel.add(b);
        sidebarPanel.add(Box.createVerticalStrut(2));
    }

    private JComponent buildSidebarActionButton(String label, String actionKey) {
        SidebarButton b = new SidebarButton(label, actionKey);
        b.markAsFooter();
        b.addActionListener(e -> onSidebarClick(actionKey));
        boutonsSidebar.add(b);
        return b;
    }

    private void highlightActiveButton() {
        for (SidebarButton b : boutonsSidebar) {
            b.setActive(cardCourante.equals(b.getActionKey()));
        }
    }

    // ==================== ROUTAGE DES CLICS SIDEBAR ====================

    /**
     * Dispatch central pour tous les clics sidebar.
     * Deux chemins possibles selon qu'une saisie est en cours ou non.
     */
    private void onSidebarClick(String actionKey) {
        InputRequest<?> r = requeteCourante.get();
        if (r != null && r.acceptsSidebarKey(actionKey)) {
            r.submitForKey(actionKey);
            return;
        }
        if (isCardKey(actionKey)) {
            showCard(actionKey);
        }
        // Si ce n'est pas une carte et qu'aucune saisie n'attend : no-op.
    }

    private boolean isCardKey(String key) {
        // Les clés qui ne sont PAS des cartes (purement actionnelles).
        return !(key.equals("quitter")
              || key.equals("deconnexion")
              || key.equals("visiteurAction"));
    }

    /** Affiche la carte demandée (navigation pure, sans effet contrôleur). */
    public void showCard(String name) {
        cardCourante = name;
        contentLayout.show(contentPanel, name);
        highlightActiveButton();
    }

    // ==================== GESTION DES ÉTATS DE SESSION ====================

    /**
     * Changement d'état de session + reconstruction sidebar.
     * Thread-safe : s'exécute sur l'EDT même si appelée d'ailleurs.
     */
    public void setSessionState(SessionState newState, String utilisateur) {
        runOnEdt(() -> {
            this.sessionState = newState;
            this.nomUtilisateur = utilisateur;
            cardCourante = "accueil";
            rebuildSidebar();
            contentLayout.show(contentPanel, "accueil");
        });
    }

    // ==================== CARTES (placeholders) ====================

    private JPanel buildAccueilCard() {
        JPanel card = new JPanel();
        card.setBackground(Styles.BG_MAIN);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(80, Styles.PADDING_LG * 2, Styles.PADDING_LG, Styles.PADDING_LG * 2));

        JLabel titre = Styles.titleLabel("Bienvenue sur JAVAZIK");
        titre.setFont(Styles.FONT_TITLE.deriveFont(36f));
        titre.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sousTitre = Styles.mutedLabel("Votre catalogue musical, vos playlists, votre historique d'écoute.");
        sousTitre.setFont(Styles.FONT_BODY.deriveFont(16f));
        sousTitre.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel cta = Styles.bodyLabel("Choisissez une action dans le menu de gauche pour commencer.");
        cta.setForeground(Styles.TEXT_MUTED);
        cta.setBorder(new EmptyBorder(Styles.PADDING_LG, 0, 0, 0));
        cta.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titre);
        card.add(Box.createVerticalStrut(Styles.PADDING_SM));
        card.add(sousTitre);
        card.add(cta);
        card.add(Box.createVerticalGlue());

        return card;
    }

    private JPanel placeholder(String titre, String description) {
        JPanel card = new JPanel();
        card.setBackground(Styles.BG_MAIN);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(Styles.PADDING_LG * 2, Styles.PADDING_LG * 2,
                                       Styles.PADDING_LG, Styles.PADDING_LG * 2));

        JLabel t = Styles.titleLabel(titre);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel d = Styles.mutedLabel(description);
        d.setBorder(new EmptyBorder(Styles.PADDING_SM, 0, 0, 0));
        d.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel todo = new JLabel("Écran en cours de construction.");
        todo.setFont(Styles.FONT_SMALL);
        todo.setForeground(Styles.TEXT_MUTED);
        todo.setBorder(new EmptyBorder(Styles.PADDING_LG, 0, 0, 0));
        todo.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(t);
        card.add(d);
        card.add(todo);
        card.add(Box.createVerticalGlue());

        return card;
    }

    // ==================== UTILITAIRES INTERNES ====================

    private JPanel transparentPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    /**
     * Exécute un Runnable sur l'EDT. Si on y est déjà, exécute directement.
     * Utilisé pour tout ce qui touche à l'UI depuis le thread contrôleur.
     */
    private void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    /**
     * Ancien système de désactivation des boutons non armés par une requête
     * en cours — retiré car causait races et mauvaise UX. Les boutons non
     * armés restent cliquables et font juste de la navigation pure (changement
     * de carte), sans soumettre de valeur au contrôleur.
     *
     * Gardé comme no-op pour ne pas casser les appels existants.
     */
    private void refreshSidebarButtonsState() {
        // no-op intentionnel
    }

    // ==================== DIALOGS DE SAISIE (modaux, bloquants) ====================

    /**
     * Affiche un dialog de saisie texte et bloque le thread appelant jusqu'à
     * ce que l'utilisateur valide ou annule. Retourne la chaîne saisie, ou ""
     * si annulation/fermeture.
     *
     * À appeler depuis le thread contrôleur. La logique UI s'exécute sur l'EDT.
     */
    private String promptText(String titre, String message) {
        final String[] resultat = {""};
        try {
            Runnable r = () -> {
                Object rep = JOptionPane.showInputDialog(
                        fenetre, message, titre,
                        JOptionPane.PLAIN_MESSAGE);
                resultat[0] = (rep == null) ? "" : rep.toString();
            };
            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
            } else {
                SwingUtilities.invokeAndWait(r);
            }
        } catch (Exception ignored) {}
        return resultat[0];
    }

    /**
     * Affiche un dialog de saisie mot de passe (caractères masqués) et bloque
     * jusqu'à validation. Retourne la chaîne saisie, ou "" si annulation.
     */
    private String promptPassword(String titre, String message) {
        final String[] resultat = {""};
        try {
            Runnable r = () -> {
                JPasswordField pf = new JPasswordField(20);
                pf.setFont(Styles.FONT_BODY);
                JPanel panel = new JPanel(new BorderLayout(0, 8));
                panel.add(new JLabel(message), BorderLayout.NORTH);
                panel.add(pf, BorderLayout.CENTER);
                // Focus automatique sur le champ de saisie quand le dialog s'affiche
                pf.addAncestorListener(new javax.swing.event.AncestorListener() {
                    @Override public void ancestorAdded(javax.swing.event.AncestorEvent e) { pf.requestFocusInWindow(); }
                    @Override public void ancestorMoved(javax.swing.event.AncestorEvent e) {}
                    @Override public void ancestorRemoved(javax.swing.event.AncestorEvent e) {}
                });
                int reponse = JOptionPane.showConfirmDialog(
                        fenetre, panel, titre,
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (reponse == JOptionPane.OK_OPTION) {
                    resultat[0] = new String(pf.getPassword());
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
            } else {
                SwingUtilities.invokeAndWait(r);
            }
        } catch (Exception ignored) {}
        return resultat[0];
    }

    /** Affiche un message d'information (non-bloquant). */
    private void showInfo(String titre, String message, int messageType) {
        runOnEdt(() -> JOptionPane.showMessageDialog(fenetre, message, titre, messageType));
    }

    /** Affiche un message d'information (icône info). */
    private void showInfo(String message) {
        showInfo("Information", message, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Affiche un message d'erreur (icône erreur). */
    private void showError(String message) {
        showInfo("Erreur", message, JOptionPane.ERROR_MESSAGE);
    }

    // ==================== FRAMEWORK DE SAISIE UTILISATEUR ====================

    /**
     * Requête de saisie : représente une attente de valeur depuis l'UI.
     *
     * Le thread appelant (contrôleur) crée une InputRequest et appelle
     * {@code await()} pour bloquer. L'EDT appelle {@code submit(val)}
     * quand l'utilisateur a fourni sa réponse.
     *
     * Pour les menus sidebar, la requête connait la correspondance
     * "clé sidebar → valeur à retourner" (map key → T).
     */
    private static class InputRequest<T> {
        private final SynchronousQueue<Object> queue = new SynchronousQueue<>();
        private final java.util.Map<String, T> mapping;
        private final T defaultValue;
        /** Sentinel non-null pour représenter une "annulation" dans la queue. */
        private static final Object CANCEL = new Object();

        /** Pour les menus : keyArmée → valeur à retourner, + valeur par défaut si cancel. */
        InputRequest(java.util.Map<String, T> mapping, T defaultValue) {
            this.mapping = mapping;
            this.defaultValue = defaultValue;
        }

        /** Vrai si cette clé sidebar est armée pour répondre à la saisie. */
        boolean acceptsSidebarKey(String key) {
            return key != null && mapping.containsKey(key);
        }

        /** Appelé depuis l'EDT quand l'utilisateur clique sur un bouton armé. */
        void submitForKey(String key) {
            T val = mapping.get(key);
            if (val != null) {
                try { queue.put(val); } catch (InterruptedException ignored) {}
            }
        }

        /** Appelé depuis l'EDT si la fenêtre se ferme pendant l'attente. */
        void cancelWithDefault() {
            try { queue.put(CANCEL); } catch (InterruptedException ignored) {}
        }

        /**
         * Bloque le thread appelant jusqu'à réception d'une valeur.
         * Retourne defaultValue en cas d'annulation ou d'interruption.
         */
        @SuppressWarnings("unchecked")
        T await() {
            try {
                Object o = queue.take();
                if (o == CANCEL) return defaultValue;
                return (T) o;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return defaultValue;
            }
        }
    }

    /**
     * Construit une requête de menu sidebar et bloque jusqu'au choix.
     * Doit être appelée depuis le thread contrôleur, jamais l'EDT.
     */
    private <T> T awaitSidebarChoice(java.util.Map<String, T> mapping, T defaultValue) {
        InputRequest<T> req = new InputRequest<>(mapping, defaultValue);
        requeteCourante.set(req);
        runOnEdt(this::refreshSidebarButtonsState);
        try {
            return req.await();
        } finally {
            requeteCourante.set(null);
            runOnEdt(this::refreshSidebarButtonsState);
        }
    }

    // ==================== CLASSE INTERNE : BOUTON SIDEBAR ====================

    private static class SidebarButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final String actionKey;
        private boolean active = false;

        SidebarButton(String text, String actionKey) {
            super(text);
            this.actionKey = actionKey;
            setFont(Styles.FONT_SIDEBAR);
            setForeground(Styles.TEXT_ON_TEAL);
            setBackground(Styles.TEAL);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(true);
            setOpaque(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, Styles.PADDING_LG, 10, Styles.PADDING_LG));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (isEnabled() && !active) setBackground(Styles.TEAL_DARK);
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    if (isEnabled() && !active) setBackground(Styles.TEAL);
                }
            });
        }

        String getActionKey() { return actionKey; }

        void setActive(boolean a) {
            this.active = a;
            if (isEnabled()) {
                setBackground(a ? Styles.TEAL_LIGHT : Styles.TEAL);
            }
            setFont(a ? Styles.FONT_SIDEBAR.deriveFont(Font.BOLD) : Styles.FONT_SIDEBAR);
        }

        @Override public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (!enabled) {
                // Bouton désactivé : style estompé, curseur normal
                setBackground(Styles.TEAL);
                setForeground(new Color(255, 255, 255, 110));
                setCursor(Cursor.getDefaultCursor());
            } else {
                setForeground(Styles.TEXT_ON_TEAL);
                setBackground(active ? Styles.TEAL_LIGHT : Styles.TEAL);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }

        void markAsFooter() { /* réservé pour polish */ }
    }

    // ==================== SURCHARGES VueConsole ====================

    @Override public void afficherBienvenue() { /* affiché via accueil card */ }

    /**
     * Menu principal : réinitialise l'état DECONNECTE, affiche la carte
     * accueil, arme les 5 boutons sidebar correspondants puis bloque
     * jusqu'au clic utilisateur.
     *
     * La préparation de l'UI est faite en {@code invokeAndWait} pour
     * garantir que tout est en place AVANT qu'on n'arme la requête
     * et qu'on ne bloque le thread contrôleur. Ça évite toute race
     * avec un clic qui arriverait trop tôt.
     */
    @Override public int afficherMenuPrincipal() {
        // Étape 1 : préparer l'UI de manière SYNCHRONE sur l'EDT
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                resetToAccueilDeconnecte();
            } else {
                SwingUtilities.invokeAndWait(this::resetToAccueilDeconnecte);
            }
        } catch (Exception ignored) {}

        // Étape 2 : construire la requête et bloquer
        java.util.Map<String, Integer> mapping = new java.util.HashMap<>();
        mapping.put("connexionAdmin",  1);
        mapping.put("connexionClient", 2);
        mapping.put("inscription",     3);
        mapping.put("visiteurAction",  4);
        mapping.put("quitter",         5);

        return awaitSidebarChoice(mapping, 5);
    }

    /** Réinitialise la sidebar et affiche la carte accueil. À appeler sur l'EDT. */
    private void resetToAccueilDeconnecte() {
        sessionState = SessionState.DECONNECTE;
        nomUtilisateur = null;
        cardCourante = "accueil";
        rebuildSidebar();
        contentLayout.show(contentPanel, "accueil");
    }

    // Les autres menus sont encore stubés et retournent "retour menu principal".
    // Ils seront câblés aux étapes 5/6/7.
    @Override public int afficherMenuAdmin()      { return 13; }
    @Override public int afficherMenuClient()     { return 5;  }
    @Override public int afficherMenuVisiteur()   { return 3;  }
    @Override public int afficherMenuCatalogue()  { return 7;  }

    @Override public String demanderRecherche()   { return ""; }
    @Override public String demanderMail()         { return promptText("Adresse e-mail", "Saisissez votre e-mail :"); }
    @Override public String demanderMdp()          { return promptPassword("Mot de passe", "Saisissez votre mot de passe :"); }
    @Override public String demanderNom()          { return promptText("Inscription (1/4)", "Nom :"); }
    @Override public String demanderPrenom()       { return promptText("Inscription (2/4)", "Prénom :"); }
    @Override public String demanderEmail()        { return promptText("Inscription (3/4)", "Adresse e-mail :"); }
    @Override public String demanderMotDePasse()   { return promptPassword("Inscription (4/4)", "Choisissez un mot de passe :"); }

    @Override public void afficherResultatsRecherche(Catalogue.ResultatRecherche r) { }
    @Override public void afficherDetailsMorceau(Morceau m)                         { }
    @Override public void afficherDetailsAlbum(Album a)                             { }
    @Override public void afficherDetailsArtiste(Artiste a)                         { }
    @Override public void afficherDetailsGroupe(Groupe g)                           { }
    @Override public void afficherListeMorceaux(List<Morceau> morceaux)             { }
    @Override public void afficherListeAlbums(List<Album> albums)                   { }
    @Override public void afficherListeArtistes(List<Artiste> artistes)             { }
    @Override public void afficherListeGroupes(List<Groupe> groupes)                { }

    @Override public int afficherMenuNavigation() { return 5; }
    @Override public int demanderIdElement()      { return -1; }

    @Override public void afficherMessage(String message) {
        // Le contrôleur appelle afficherMessage("Merci d'avoir utilise Javazik...")
        // comme dernier acte avant de se terminer : on ferme la fenêtre.
        if (message != null && message.startsWith("Merci d'avoir utilise")) {
            runOnEdt(() -> {
                fenetre.dispose();
                System.exit(0);
            });
            return;
        }
        // Tout autre message (erreur, info ponctuelle) : dialog d'info.
        if (message != null && !message.isEmpty()) {
            showInfo(message);
        }
    }
    @Override public void afficherChoixInvalide()      { showError("Choix invalide."); }
    @Override public void afficherConnexionReussie()   { showInfo("Connexion réussie."); }
    @Override public void afficherMdpIncorrect()       { showError("Mot de passe incorrect."); }
    @Override public void afficherMailIncorrect()      { showError("Adresse e-mail inconnue."); }
    @Override public void afficherPasAdmin()           { showError("Ce compte n'est pas administrateur."); }
    @Override public void afficherInscriptionReussie() { showInfo("Votre compte a été créé avec succès."); }

    // Admin
    @Override public String demanderTitreMorceau()      { return ""; }
    @Override public int    demanderDureeMorceau()      { return 0; }
    @Override public String demanderGenreMorceau()      { return ""; }
    @Override public int    demanderAnneeMorceau()      { return 0; }
    @Override public int    demanderIdArtisteMorceau()  { return 0; }
    @Override public int    demanderIdGroupeMorceau()   { return 0; }
    @Override public void   afficherMorceauAjoute(int id) { }
    @Override public int    demanderIdSuppression()     { return -1; }
    @Override public void   afficherElementSupprime(String type)                { }
    @Override public void   afficherElementNonTrouve(String type, int id)       { }
}
