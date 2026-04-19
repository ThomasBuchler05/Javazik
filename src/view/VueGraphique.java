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
    private int utilisateurId = 0;             // ID de l'utilisateur connecté (0 si aucun)

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
        contentPanel.add(buildAccueilCard(),    "accueil");
        contentPanel.add(buildCatalogueCard(),  "catalogue");
        contentPanel.add(buildEcouteCard(),     "ecoute");

        contentPanel.add(buildConnexionCard("Connexion administrateur",
                "Acc\u00e9dez \u00e0 l'espace d'administration.", "connexionAdmin"),   "connexionAdmin");
        contentPanel.add(buildConnexionCard("Connexion client",
                "Acc\u00e9dez \u00e0 votre espace abonn\u00e9.",  "connexionClient"),  "connexionClient");
        contentPanel.add(buildInscriptionCard(),                             "inscription");

        contentPanel.add(buildPlaylistsCard(),  "playlists");
        contentPanel.add(buildHistoriqueCard(), "historique");

        contentPanel.add(buildAdminCatalogueCard(), "gestionCatalogue");
        contentPanel.add(buildAdminComptesCard(),   "gestionComptes");
        contentPanel.add(buildAdminStatsCard(),     "statistiques");
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
                addSidebarButton("G\u00e9rer le catalogue",  "gestionCatalogue");
                addSidebarButton("G\u00e9rer les comptes",   "gestionComptes");
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

        // Logo avec point "live" animé
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        logoRow.setOpaque(false);

        JLabel logo = new JLabel("JAVAZIK  \u266A");
        logo.setFont(Styles.FONT_LOGO);
        logo.setForeground(Styles.TEXT_ON_TEAL);

        // Indicateur "live" (petit cercle qui pulse entre deux alphas)
        JLabel liveDot = new JLabel("\u25CF") {
            private boolean bright = true;
            {
                javax.swing.Timer t = new javax.swing.Timer(800, e -> {
                    bright = !bright;
                    setForeground(bright
                            ? new Color(255, 255, 255, 220)
                            : new Color(255, 255, 255, 80));
                    repaint();
                });
                t.start();
            }
        };
        liveDot.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        liveDot.setForeground(new Color(255, 255, 255, 220));

        logoRow.add(logo);
        logoRow.add(liveDot);
        p.add(logoRow);

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
     * Débloque le thread contrôleur quel que soit l'endroit où il attend :
     * <ul>
     *   <li>afficherMenuCatalogue() / afficherMenuNavigation() bloqués sur catalogueIntQueue</li>
     *   <li>demanderRechercheMusique() / demanderIdMorceauEcoute() bloqués sur ecouteQueue</li>
     *   <li>afficherMenuPlaylist() / demanderIdPlaylist() bloqués sur playlistsQueue</li>
     * </ul>
     * Utilisé avant chaque changement de section depuis la sidebar pour garantir
     * que le contrôleur ressort de sa méthode bloquante courante et peut prendre
     * en compte le nouveau signal posé dans clientNavQueue.
     *
     * IMPORTANT : une seule valeur suffit pour chaque queue. Si le contrôleur
     * n'est pas en train d'attendre sur l'une d'elles, les afficherMenu*()
     * correspondants la videront avec queue.clear() avant leur prochaine attente.
     */
    private void interrompreAttentesClient() {
        catalogueIntQueue.offer(7);
        ecouteQueue.offer("nav:sidebar");
        playlistsQueue.offer("retour");
    }

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

        // Sidebar CLIENT / VISITEUR
        if (sessionState == SessionState.CLIENT || sessionState == SessionState.VISITEUR) {
            switch (actionKey) {
                case "catalogue":
                    interrompreAttentesClient();
                    showCard("catalogue");
                    clientNavQueue.offer("catalogue");
                    return;
                case "playlists":
                    interrompreAttentesClient();
                    showCard("playlists");
                    clientNavQueue.offer("playlists");
                    return;
                case "ecoute":
                    interrompreAttentesClient();
                    showCard("ecoute");
                    clientNavQueue.offer("ecoute");
                    return;
                case "historique":
                    interrompreAttentesClient();
                    showCard("historique");
                    // Recharger l'historique immédiatement depuis le fichier (I/O hors EDT)
                    new Thread(() -> {
                        if (utilisateurId > 0) {
                            java.util.List<model.Historique> hist =
                                    model.Historique.getHistoriqueClient(utilisateurId);
                            afficherHistorique(hist);
                        }
                    }).start();
                    clientNavQueue.offer("historique");
                    return;
                case "accueil":
                    interrompreAttentesClient();
                    showCard("accueil");
                    clientNavQueue.offer("accueil");
                    return;
                case "deconnexion":
                    interrompreAttentesClient();
                    clientNavQueue.offer("deconnexion");
                    return;
            }
        }

        // Sidebar admin : les clics doivent pousser dans adminQueue pour débloquer afficherMenuAdmin()
        if (sessionState == SessionState.ADMIN) {
            if (actionKey.equals("gestionComptes")) {
                showCard("gestionComptes");
                catalogueIntQueue.offer(7); // débloquer si on était dans menuCatalogue
                try { adminQueue.put("gererComptes"); } catch (InterruptedException ignored) {}
                return;
            }
            if (actionKey.equals("statistiques")) {
                showCard("statistiques");
                catalogueIntQueue.offer(7);
                try { adminQueue.put("statistiques"); } catch (InterruptedException ignored) {}
                return;
            }
            if (actionKey.equals("accueil")) {
                showCard("accueil");
                catalogueIntQueue.offer(7); // débloquer si bloqué dans menuCatalogue
                return;
            }
            if (actionKey.equals("catalogue")) {
                showCard("catalogue");
                catalogueIntQueue.offer(7); // débloquer si déjà bloqué dans menuCatalogue
                try { adminQueue.put("catalogue"); } catch (InterruptedException ignored) {}
                return;
            }
            if (actionKey.equals("gestionCatalogue")) {
                showCard("gestionCatalogue");
                resetAdminCatalogueMsg();
                clearAdminCatalogueContent();
                catalogueIntQueue.offer(7);
                try { adminQueue.put("gestionCatalogue"); } catch (InterruptedException ignored) {}
                return;
            }
            if (actionKey.equals("deconnexion")) {
                catalogueIntQueue.offer(7);
                try { adminQueue.put("retour"); } catch (InterruptedException ignored) {}
                return;
            }
        }

        if (isCardKey(actionKey)) {
            showCard(actionKey);
        }
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
        Runnable rebuild = () -> {
            this.sessionState = newState;
            this.nomUtilisateur = utilisateur;
            if (newState == SessionState.DECONNECTE) this.utilisateurId = 0;
            cardCourante = "accueil";
            rebuildSidebar();
            contentLayout.show(contentPanel, "accueil");
        };
        if (SwingUtilities.isEventDispatchThread()) {
            rebuild.run();
        } else {
            try { SwingUtilities.invokeAndWait(rebuild); }
            catch (Exception ignored) {}
        }
    }

    // ==================== CARTES (placeholders) ====================

    private JPanel buildAccueilCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Styles.BG_MAIN);

        // Bannière hero avec dégradé teal simulé et notes animées
        JPanel hero = new JPanel() {
            private float noteOffset = 0f;
            {
                javax.swing.Timer anim = new javax.swing.Timer(60, e -> {
                    noteOffset = (noteOffset + 0.6f) % 40f;
                    repaint();
                });
                anim.start();
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, Styles.TEAL, getWidth(), getHeight(), Styles.TEAL_DARK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Notes musicales semi-transparentes flottantes
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 80));
                g2.setColor(new Color(255, 255, 255, 22));
                g2.drawString("\u266B", getWidth() - 160, (int)(95 + noteOffset));
                g2.drawString("\u266A", getWidth() - 290, (int)(55 + noteOffset * 0.7f));
                g2.drawString("\u2669", getWidth() - 55,  (int)(135 + noteOffset * 1.3f));
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 44));
                g2.setColor(new Color(255, 255, 255, 12));
                g2.drawString("\u266C", getWidth() - 380, (int)(65 + noteOffset * 0.5f));
                g2.drawString("\u266B", 50, (int)(155 + noteOffset * 0.9f));
                g2.dispose();
            }
        };
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBorder(new EmptyBorder(60, Styles.PADDING_LG * 3, 60, Styles.PADDING_LG * 3));
        hero.setPreferredSize(new Dimension(0, 220));

        JLabel logoLabel = new JLabel("JAVAZIK  \u266A");
        logoLabel.setFont(Styles.FONT_LOGO.deriveFont(Font.BOLD, 42f));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("Votre catalogue musical, vos playlists, votre historique.");
        tagline.setFont(Styles.FONT_BODY.deriveFont(16f));
        tagline.setForeground(new Color(255, 255, 255, 210));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel cta = new JLabel("Choisissez une action dans le menu de gauche pour commencer.");
        cta.setFont(Styles.FONT_SMALL);
        cta.setForeground(new Color(255, 255, 255, 160));
        cta.setAlignmentX(Component.LEFT_ALIGNMENT);

        hero.add(logoLabel);
        hero.add(Box.createVerticalStrut(Styles.PADDING_MD));
        hero.add(tagline);
        hero.add(Box.createVerticalStrut(Styles.PADDING_SM));
        hero.add(cta);

        card.add(hero, BorderLayout.NORTH);

        // Section features
        JPanel features = new JPanel(new GridLayout(1, 3, Styles.PADDING_LG, 0));
        features.setBackground(Styles.BG_MAIN);
        features.setBorder(new EmptyBorder(Styles.PADDING_LG * 2, Styles.PADDING_LG * 3,
                Styles.PADDING_LG * 2, Styles.PADDING_LG * 3));

        String[][] feats = {
                {"\uD83D\uDCBF", "Catalogue", "Parcourez morceaux, albums, artistes et groupes."},
                {"\uD83C\uDFB5", "Écouter", "Lancez la lecture et suivez vos écoutes."},
                {"\uD83D\uDCCB", "Playlists", "Créez et gérez vos playlists personnalisées."},
        };
        for (String[] f : feats) {
            JPanel tile = new JPanel();
            tile.setBackground(Styles.BG_ALT);
            tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
            tile.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Styles.BORDER, 1, true),
                    BorderFactory.createEmptyBorder(Styles.PADDING_LG, Styles.PADDING_LG,
                            Styles.PADDING_LG, Styles.PADDING_LG)));

            JLabel icon = new JLabel(f[0]);
            icon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));
            icon.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel title = new JLabel(f[1]);
            title.setFont(Styles.FONT_BODY_BOLD);
            title.setForeground(Styles.TEXT);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel desc = Styles.mutedLabel(f[2]);
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);

            tile.add(icon);
            tile.add(Box.createVerticalStrut(Styles.PADDING_SM));
            tile.add(title);
            tile.add(Box.createVerticalStrut(4));
            tile.add(desc);

            features.add(tile);
        }
        card.add(features, BorderLayout.CENTER);

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

        JLabel todo = new JLabel("\u00c9cran en cours de construction.");
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

    // ==================== CARTES CONNEXION / INSCRIPTION ====================

    /**
     * File d'attente partagée pour la saisie inline dans les formulaires.
     * Le contrôleur appelle demanderMail() → bloque sur cette queue.
     * Le bouton "Valider" du formulaire y pousse la valeur saisie.
     */
    private final java.util.concurrent.SynchronousQueue<String> saisieCourante =
            new java.util.concurrent.SynchronousQueue<>();

    /**
     * Label d'erreur associé à la carte active.
     * Mis à jour via afficherMdpIncorrect(), afficherMailIncorrect(), etc.
     */
    private JLabel labelErreurCourant = null;

    /**
     * Mini-bannière hero réutilisable affichée en haut des pages connexion et inscription.
     * Reprend le dégradé teal + notes animées + logo de la page d'accueil.
     */
    private JPanel buildMiniHero() {
        JPanel hero = new JPanel() {
            private float noteOffset = 0f;
            {
                javax.swing.Timer anim = new javax.swing.Timer(60, e -> {
                    noteOffset = (noteOffset + 0.6f) % 40f;
                    repaint();
                });
                anim.start();
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, Styles.TEAL, getWidth(), getHeight(), Styles.TEAL_DARK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Notes musicales flottantes semi-transparentes
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 50));
                g2.setColor(new Color(255, 255, 255, 22));
                g2.drawString("\u266B", getWidth() - 120, (int)(70 + noteOffset));
                g2.drawString("\u266A", getWidth() - 220, (int)(40 + noteOffset * 0.7f));
                g2.drawString("\u2669", getWidth() - 40,  (int)(90 + noteOffset * 1.3f));
                g2.dispose();
            }
        };
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBorder(new EmptyBorder(Styles.PADDING_LG * 2, Styles.PADDING_LG * 3,
                Styles.PADDING_LG * 2, Styles.PADDING_LG * 3));
        hero.setPreferredSize(new Dimension(0, 120));

        JLabel logoLabel = new JLabel("JAVAZIK  \u266A");
        logoLabel.setFont(Styles.FONT_LOGO.deriveFont(Font.BOLD, 30f));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("Votre catalogue musical, vos playlists, votre historique.");
        tagline.setFont(Styles.FONT_BODY.deriveFont(13f));
        tagline.setForeground(new Color(255, 255, 255, 200));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        hero.add(logoLabel);
        hero.add(Box.createVerticalStrut(Styles.PADDING_SM));
        hero.add(tagline);

        return hero;
    }

    /**
     * Construit une carte formulaire de connexion réutilisable (admin ou client).
     * Elle contient un champ email, un champ mot de passe et un bouton Valider.
     *
     * Fonctionnement :
     *  - quand le contrôleur appelle demanderMail(), on active le champ email
     *    et on place le focus dessus ; le clic sur Valider pousse l'email dans
     *    saisieCourante, débloquant le thread contrôleur.
     *  - même logique pour demanderMdp() avec le champ password.
     *
     * On stocke les références aux champs + bouton dans des tableaux à 1 case
     * pour y accéder depuis les méthodes demanderMail/demanderMdp.
     */
    private final JTextField[]     champEmailConnexion = new JTextField[2];
    private final JPasswordField[] champMdpConnexion   = new JPasswordField[2];
    private final JLabel[]         labelErreurConnexion = new JLabel[2];
    // DocumentListeners actifs pour effacer l'erreur à la frappe — retirés avant ajout
    private final javax.swing.event.DocumentListener[] errDocListenerEmail = new javax.swing.event.DocumentListener[2];
    private final javax.swing.event.DocumentListener[] errDocListenerMdp   = new javax.swing.event.DocumentListener[2];

    // index 0 = admin, index 1 = client
    private int indexCarteActive = -1;

    /**
     * Flag d'armement de la saisie de connexion/inscription.
     * Tant qu'il est à false, submitAction (ActionListener des champs / bouton)
     * ne pousse RIEN dans saisieCourante. Il est armé uniquement pendant que
     * le contrôleur attend une valeur via attendreSaisie(), et désarmé dès
     * qu'une valeur est envoyée. Ceci évite les "submits fantômes" provoqués
     * par d'anciens ActionEvents, des focus transfers, ou des touches qui
     * traversent les écrans lors de transitions de cartes.
     */
    private volatile boolean saisieArmee = false;

    private JPanel buildConnexionCard(String titreStr, String sousTitreStr, String cardKey) {
        int idx = cardKey.equals("connexionAdmin") ? 0 : 1;

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Styles.BG_MAIN);

        // — Mini-bannière hero avec logo animé —
        outer.add(buildMiniHero(), BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(Styles.BG_MAIN);

        JPanel form = new JPanel();
        form.setBackground(Styles.BG_MAIN);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(0, 0, 0, 0));
        // Largeur maximale du formulaire
        form.setMaximumSize(new Dimension(420, Integer.MAX_VALUE));

        // — Titre —
        JLabel titre = Styles.titleLabel(titreStr);
        titre.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(titre);

        JLabel sousTitre = Styles.mutedLabel(sousTitreStr);
        sousTitre.setBorder(new EmptyBorder(Styles.PADDING_SM, 0, Styles.PADDING_LG * 2, 0));
        sousTitre.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sousTitre);

        // — Champ Email —
        JLabel lblEmail = Styles.bodyLabel("Adresse e-mail");
        lblEmail.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblEmail);
        form.add(Box.createVerticalStrut(Styles.PADDING_SM / 2));

        JTextField fieldEmail = buildTextField(320);
        champEmailConnexion[idx] = fieldEmail;
        form.add(fieldEmail);
        form.add(Box.createVerticalStrut(Styles.PADDING_MD));

        // — Champ Mot de passe —
        JLabel lblMdp = Styles.bodyLabel("Mot de passe");
        lblMdp.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblMdp);
        form.add(Box.createVerticalStrut(Styles.PADDING_SM / 2));

        JPasswordField fieldMdp = buildPasswordField(320);
        champMdpConnexion[idx] = fieldMdp;
        form.add(fieldMdp);
        form.add(Box.createVerticalStrut(Styles.PADDING_MD));

        // — Label erreur —
        JLabel lblErreur = new JLabel(" ");
        lblErreur.setFont(Styles.FONT_SMALL);
        lblErreur.setForeground(new Color(220, 38, 38));
        lblErreur.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelErreurConnexion[idx] = lblErreur;
        form.add(lblErreur);
        form.add(Box.createVerticalStrut(Styles.PADDING_MD));

        // — Bouton Valider —
        JButton btnValider = Styles.primaryButton("Se connecter");
        btnValider.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnValider.setMaximumSize(new Dimension(200, 42));
        form.add(btnValider);

        // — Action Entrée / clic —
        Runnable submitAction = () -> {
            // Garde anti-submit fantôme : si le contrôleur n'a pas armé une attente,
            // on ignore purement l'événement (peut venir d'un focus transfer, d'un
            // ActionEvent résiduel, d'une touche Entrée traversant un changement de carte...)
            if (!saisieArmee) {
                System.err.println("[SUBMIT IGNORED] saisie non armée");
                return;
            }
            // On determine quel champ est actif pour pousser la bonne valeur
            String val;
            if (fieldEmail.isEnabled() && !fieldMdp.isEnabled()) {
                val = fieldEmail.getText().trim();
            } else if (fieldMdp.isEnabled()) {
                val = new String(fieldMdp.getPassword()).trim();
            } else {
                // aucun champ enabled : rien à soumettre
                System.err.println("[SUBMIT IGNORED] aucun champ enabled");
                return;
            }
            // Désarmer AVANT le put pour empêcher une double soumission
            saisieArmee = false;
            lblErreur.setText(" ");
            try { saisieCourante.put(val); } catch (InterruptedException ignored) {}
        };

        fieldEmail.addActionListener(e -> submitAction.run());
        fieldMdp.addActionListener(e -> submitAction.run());
        btnValider.addActionListener(e -> submitAction.run());

        centerWrapper.add(form);
        outer.add(centerWrapper, BorderLayout.CENTER);
        return outer;
    }

    // ==================== CARTE INSCRIPTION ====================

    private final JTextField     inscNom    = buildTextField(320);
    private final JTextField     inscPrenom = buildTextField(320);
    private final JTextField     inscEmail  = buildTextField(320);
    private final JPasswordField inscMdp    = buildPasswordField(320);
    private final JLabel         inscErreur = new JLabel(" ");

    private JPanel buildInscriptionCard() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Styles.BG_MAIN);

        // — Mini-bannière hero avec logo animé —
        outer.add(buildMiniHero(), BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(Styles.BG_MAIN);

        JPanel form = new JPanel();
        form.setBackground(Styles.BG_MAIN);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setMaximumSize(new Dimension(420, Integer.MAX_VALUE));

        JLabel titre = Styles.titleLabel("Cr\u00e9er un compte");
        titre.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(titre);

        JLabel sousTitre = Styles.mutedLabel("Rejoignez JavaZik et acc\u00e9dez \u00e0 toutes les fonctionnalit\u00e9s.");
        sousTitre.setBorder(new EmptyBorder(Styles.PADDING_SM, 0, Styles.PADDING_LG * 2, 0));
        sousTitre.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sousTitre);

        form.add(buildFieldRow("Nom", inscNom));
        form.add(Box.createVerticalStrut(Styles.PADDING_MD));
        form.add(buildFieldRow("Pr\u00e9nom", inscPrenom));
        form.add(Box.createVerticalStrut(Styles.PADDING_MD));
        form.add(buildFieldRow("Adresse e-mail", inscEmail));
        form.add(Box.createVerticalStrut(Styles.PADDING_MD));
        form.add(buildFieldRow("Mot de passe", inscMdp));
        form.add(Box.createVerticalStrut(Styles.PADDING_MD));

        inscErreur.setFont(Styles.FONT_SMALL);
        inscErreur.setForeground(new Color(220, 38, 38));
        inscErreur.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(inscErreur);
        form.add(Box.createVerticalStrut(Styles.PADDING_MD));

        JButton btnValider = Styles.primaryButton("Cr\u00e9er mon compte");
        btnValider.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnValider.setMaximumSize(new Dimension(220, 42));
        form.add(btnValider);

        // Pour l'inscription le contrôleur appelle 4 méthodes séquentiellement
        // (demanderNom, demanderPrenom, demanderEmail, demanderMotDePasse).
        // On utilise saisieCourante : chaque appel active un champ et attend.
        Runnable submitInscription = () -> {
            if (!saisieArmee) {
                System.err.println("[SUBMIT INSCR IGNORED] saisie non armée");
                return;
            }
            String val = determinerValeurInscription();
            saisieArmee = false;
            inscErreur.setText(" ");
            try { saisieCourante.put(val); } catch (InterruptedException ignored) {}
        };

        inscNom.addActionListener(e -> submitInscription.run());
        inscPrenom.addActionListener(e -> submitInscription.run());
        inscEmail.addActionListener(e -> submitInscription.run());
        inscMdp.addActionListener(e -> submitInscription.run());
        btnValider.addActionListener(e -> submitInscription.run());

        centerWrapper.add(form);
        outer.add(centerWrapper, BorderLayout.CENTER);
        return outer;
    }

    /** Retourne la valeur du champ actuellement activé dans le formulaire d'inscription. */
    private String determinerValeurInscription() {
        if (inscNom.isEnabled()    && !inscPrenom.isEnabled()) return inscNom.getText().trim();
        if (inscPrenom.isEnabled() && !inscEmail.isEnabled())  return inscPrenom.getText().trim();
        if (inscEmail.isEnabled()  && !inscMdp.isEnabled())    return inscEmail.getText().trim();
        return new String(inscMdp.getPassword()).trim();
    }

    // ==================== HELPERS CONSTRUCTION FORMULAIRES ====================

    private static JTextField buildTextField(int width) {
        JTextField f = new JTextField();
        f.setFont(Styles.FONT_BODY);
        f.setForeground(Styles.TEXT);
        f.setBackground(Styles.BG_ALT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Styles.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        f.setMaximumSize(new Dimension(width, 40));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    private static JPasswordField buildPasswordField(int width) {
        JPasswordField f = new JPasswordField();
        f.setFont(Styles.FONT_BODY);
        f.setForeground(Styles.TEXT);
        f.setBackground(Styles.BG_ALT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Styles.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        f.setMaximumSize(new Dimension(width, 40));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    /** Construit un sous-panneau label + champ, aligné à gauche. */
    private JPanel buildFieldRow(String labelText, JComponent field) {
        JPanel row = new JPanel();
        row.setBackground(Styles.BG_MAIN);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = Styles.bodyLabel(labelText);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(lbl);
        row.add(Box.createVerticalStrut(Styles.PADDING_SM / 2));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(field);
        return row;
    }

    /**
     * Active un champ (focus + enabled), désactive les autres du même formulaire,
     * et nettoie le champ si nécessaire. Doit être appelé sur l'EDT.
     *
     * Ordre critique :
     *   1. retirer l'ancien DocumentListener (s'il y en a un) — sinon le
     *      setText("") qui suit déclencherait son clearErr() intempestif ;
     *   2. setEnabled + setText("") sur le champ à armer : aucun listener
     *      attaché, donc aucun effet de bord ;
     *   3. ajouter le nouveau DocumentListener APRÈS le setText("") : il ne
     *      fire que sur la vraie frappe utilisateur ;
     *   4. requestFocus en invokeLater pour laisser le layout se stabiliser.
     */
    private void activerChampConnexion(int idx, boolean emailActif) {
        JTextField email = champEmailConnexion[idx];
        JPasswordField mdp = champMdpConnexion[idx];

        if (emailActif) {
            // 1. Retirer l'ancien listener avant toute modif du Document
            if (errDocListenerEmail[idx] != null) {
                email.getDocument().removeDocumentListener(errDocListenerEmail[idx]);
                errDocListenerEmail[idx] = null;
            }
            // 2. Configurer les champs (aucun listener attaché → setText sans effet de bord)
            email.setEnabled(true);
            email.setText("");
            email.setBackground(Styles.BG_ALT);
            mdp.setEnabled(false);
            mdp.setText("");
            mdp.setBackground(new Color(240, 240, 240));
            // 3. Installer le nouveau DocumentListener MAINTENANT (pas en invokeLater :
            //    on est déjà sur l'EDT et le champ est vide et stable)
            errDocListenerEmail[idx] = new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { clearErr(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { clearErr(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) {}
                void clearErr() {
                    if (labelErreurConnexion[idx] != null)
                        labelErreurConnexion[idx].setText(" ");
                    email.getDocument().removeDocumentListener(this);
                    errDocListenerEmail[idx] = null;
                }
            };
            email.getDocument().addDocumentListener(errDocListenerEmail[idx]);
            // 4. Focus en invokeLater (l'EDT doit avoir fini de layouter la carte)
            SwingUtilities.invokeLater(email::requestFocusInWindow);

        } else {
            // 1. Retirer l'ancien listener mdp
            if (errDocListenerMdp[idx] != null) {
                mdp.getDocument().removeDocumentListener(errDocListenerMdp[idx]);
                errDocListenerMdp[idx] = null;
            }
            // 2. Configurer les champs
            email.setEnabled(false);
            email.setBackground(new Color(240, 240, 240));
            // NE PAS vider l'email — l'utilisateur vient de le valider
            mdp.setEnabled(true);
            mdp.setText("");
            mdp.setBackground(Styles.BG_ALT);
            // 3. Installer le nouveau DocumentListener
            errDocListenerMdp[idx] = new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { clearErr(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { clearErr(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) {}
                void clearErr() {
                    if (labelErreurConnexion[idx] != null)
                        labelErreurConnexion[idx].setText(" ");
                    mdp.getDocument().removeDocumentListener(this);
                    errDocListenerMdp[idx] = null;
                }
            };
            mdp.getDocument().addDocumentListener(errDocListenerMdp[idx]);
            // 4. Focus en invokeLater
            SwingUtilities.invokeLater(mdp::requestFocusInWindow);
        }
        // Le label d'erreur reste visible jusqu'à la première frappe
    }

    private void activerChampInscription(JComponent actif) {
        JComponent[] tous = {inscNom, inscPrenom, inscEmail, inscMdp};
        for (JComponent c : tous) {
            boolean isActif = (c == actif);
            c.setEnabled(isActif);
            c.setBackground(isActif ? Styles.BG_ALT : new Color(240, 240, 240));
        }
        SwingUtilities.invokeLater(actif::requestFocusInWindow);
        inscErreur.setText(" ");
    }

    /**
     * Bloque le thread contrôleur jusqu'à ce que saisieCourante contienne une valeur.
     * Affiche d'abord la carte demandée et configure les champs.
     *
     * Protocole anti-submit fantôme :
     *  - purge de toute valeur résiduelle dans la queue (rare mais possible
     *    avec un offer() passé par submitAction juste avant armement) ;
     *  - setup EDT SYNCHRONE (invokeAndWait) : installation des champs et
     *    listeners finie avant l'armement ;
     *  - armement (saisieArmee = true) APRÈS setupEdt : aucun event antérieur
     *    ne pourra push une valeur, puisque submitAction l'ignorera ;
     *  - take() bloquant jusqu'à la vraie soumission utilisateur ;
     *  - désarmement garanti en finally (le submitAction désarme aussi avant
     *    put(), mais on re-désarme ici par sécurité en cas d'exception).
     */
    private String attendreSaisie(String cardKey, Runnable setupEdt) {
        try {
            // 1. Désarmer avant toute chose + purger queue de tout résidu
            saisieArmee = false;
            saisieCourante.poll(); // non-bloquant, consomme une éventuelle valeur en attente
            // 2. Setup EDT synchrone : champs configurés avant qu'on arme
            if (SwingUtilities.isEventDispatchThread()) {
                setupEdt.run();
            } else {
                SwingUtilities.invokeAndWait(setupEdt);
            }
            // 3. Armer : à partir d'ici, la prochaine soumission utilisateur sera acceptée
            saisieArmee = true;
            // 4. Bloquer jusqu'à la saisie
            return saisieCourante.take();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return "";
        } finally {
            // Sécurité : toujours désarmé à la sortie
            saisieArmee = false;
        }
    }

    // ==================== CARTE CATALOGUE ====================

    /**
     * Modèle de données affiché dans la carte catalogue.
     * Mis à jour par les méthodes afficherListeXxx() / afficherDetailsXxx().
     * Accès uniquement depuis l'EDT via runOnEdt().
     */
    private final java.util.concurrent.atomic.AtomicReference<Object> catalogueDonnees =
            new java.util.concurrent.atomic.AtomicReference<>(null);

    // Composants réutilisables de la carte catalogue
    private JPanel          catalogueContentArea;   // zone scrollable centrale
    private JTextField      catalogueSearchField;
    private JLabel          catalogueTitleLabel;
    // Queue pour les menus/ID demandés par le contrôleur
    private final java.util.concurrent.LinkedBlockingQueue<Integer> catalogueIntQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();
    /** Filtre en attente détecté dans naviguer() et à consommer au prochain afficherMenuCatalogue(). */
    private volatile Integer catalogueFiltreEnAttente = null;

    private JPanel buildCatalogueCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Styles.BG_MAIN);

        // ---- Barre du haut : titre + barre de recherche ----
        JPanel topBar = new JPanel();
        topBar.setBackground(Styles.BG_MAIN);
        topBar.setLayout(new BoxLayout(topBar, BoxLayout.Y_AXIS));
        topBar.setBorder(new EmptyBorder(Styles.PADDING_LG * 2, Styles.PADDING_LG * 2,
                Styles.PADDING_MD, Styles.PADDING_LG * 2));

        catalogueTitleLabel = Styles.titleLabel("Catalogue musical");
        catalogueTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topBar.add(catalogueTitleLabel);
        topBar.add(Box.createVerticalStrut(Styles.PADDING_MD));

        // Barre de recherche + bouton
        JPanel searchBar = new JPanel(new BorderLayout(Styles.PADDING_SM, 0));
        searchBar.setBackground(Styles.BG_MAIN);
        searchBar.setMaximumSize(new Dimension(500, 42));
        searchBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        catalogueSearchField = buildTextField(400);
        catalogueSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        catalogueSearchField.setToolTipText("Rechercher un morceau, album, artiste ou groupe...");
        searchBar.add(catalogueSearchField, BorderLayout.CENTER);

        JButton btnSearch = Styles.primaryButton("Rechercher");
        btnSearch.setPreferredSize(new Dimension(130, 40));
        searchBar.add(btnSearch, BorderLayout.EAST);

        topBar.add(searchBar);
        topBar.add(Box.createVerticalStrut(Styles.PADDING_SM));

        // Filtres rapides
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.PADDING_SM, 0));
        filterBar.setBackground(Styles.BG_MAIN);
        filterBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        // "Tout" affiche morceaux+albums+artistes+groupes via recherche vide (case 1 avec q="")
        // Les autres filtres : 2=morceaux, 3=albums, 4=artistes, 5=groupes
        String[] filtres   = {"Tout",  "Morceaux", "Albums", "Artistes", "Groupes"};
        int[]    filtreIds = {100,     2,           3,        4,          5};
        List<JButton> filtreButtons = new ArrayList<>();
        for (int i = 0; i < filtres.length; i++) {
            final int filtreId = filtreIds[i];
            final int fi = i;
            JButton fb = new JButton(filtres[i]) {
                @Override public void updateUI() { super.updateUI(); setOpaque(true); setBorderPainted(true); }
            };
            fb.setFont(Styles.FONT_SMALL);
            fb.setFocusPainted(false);
            fb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            fb.setBackground(fi == 0 ? Styles.TEAL : Styles.BG_MAIN);
            fb.setForeground(fi == 0 ? Styles.TEXT_ON_TEAL : Styles.TEAL);
            fb.setOpaque(true);
            fb.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Styles.TEAL, 1, true),
                    BorderFactory.createEmptyBorder(5, 14, 5, 14)));
            fb.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (!fb.getBackground().equals(Styles.TEAL)) {
                        fb.setBackground(Styles.TEAL_SURFACE);
                    }
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    if (!fb.getBackground().equals(Styles.TEAL)) {
                        fb.setBackground(Styles.BG_MAIN);
                    }
                }
            });
            fb.addActionListener(e -> {
                // Mise à jour immédiate du style du bouton actif (sans attendre le contrôleur)
                for (int j = 0; j < filtreButtons.size(); j++) {
                    JButton btn = filtreButtons.get(j);
                    if (j == fi) {
                        btn.setBackground(Styles.TEAL);
                        btn.setForeground(Styles.TEXT_ON_TEAL);
                    } else {
                        btn.setBackground(Styles.BG_MAIN);
                        btn.setForeground(Styles.TEAL);
                    }
                }
                // Vider les clics précédents non consommés pour éviter les doublons
                catalogueIntQueue.clear();
                try { catalogueIntQueue.put(filtreId); } catch (InterruptedException ignored) {}
            });
            filtreButtons.add(fb);
            filterBar.add(fb);
        }
        topBar.add(filterBar);

        // Wrapper avec notes flottantes a droite
        JPanel topBarWrapper1 = new JPanel(new BorderLayout());
        topBarWrapper1.setBackground(Styles.BG_MAIN);
        topBarWrapper1.add(topBar, BorderLayout.CENTER);
        topBarWrapper1.add(buildFloatingNotesPanel(), BorderLayout.EAST);
        card.add(topBarWrapper1, BorderLayout.NORTH);

        // ---- Zone de contenu scrollable ----
        catalogueContentArea = new JPanel();
        catalogueContentArea.setBackground(Styles.BG_MAIN);
        catalogueContentArea.setLayout(new BoxLayout(catalogueContentArea, BoxLayout.Y_AXIS));
        catalogueContentArea.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2,
                Styles.PADDING_LG, Styles.PADDING_LG * 2));

        // Wrapper BorderLayout.NORTH pour que BoxLayout calcule la preferredSize correctement
        ScrollablePanel catalogueWrapper = new ScrollablePanel();
        catalogueWrapper.setBackground(Styles.BG_MAIN);
        catalogueWrapper.setLayout(new BoxLayout(catalogueWrapper, BoxLayout.Y_AXIS));
        catalogueWrapper.add(catalogueContentArea);
        catalogueWrapper.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(catalogueWrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Styles.BG_MAIN);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        // Action recherche
        Runnable doSearch = () -> {
            String q = catalogueSearchField.getText().trim();
            if (!q.isEmpty()) {
                try { catalogueIntQueue.put(1); } catch (InterruptedException ignored) {}
            }
        };
        catalogueSearchField.addActionListener(e -> doSearch.run());
        btnSearch.addActionListener(e -> doSearch.run());

        return card;
    }

    /** Vide et repeuple la zone de contenu catalogue. Appelé sur l'EDT. */
    private void setCatalogueContent(JPanel newContent) {
        catalogueContentArea.removeAll();
        catalogueContentArea.add(newContent);
        catalogueContentArea.revalidate();
        catalogueContentArea.repaint();
    }

    /**
     * Construit un panneau liste générique.
     * typeCode : 1=morceau, 2=album, 3=artiste, 4=groupe
     * Les boutons Détails poussent typeCode*1_000_000 + id dans catalogueIntQueue.
     * En contexte admin (isAdminCatalogueContext()), les lignes affichent l'ID
     * et un bouton "Supprimer" direct qui pousse dans suppressionDirecteQueue.
     */
    private <T> JPanel buildListPanel(String sectionTitle, List<T> items,
                                      java.util.function.Function<T, String> toLabel,
                                      java.util.function.Function<T, Integer> toId,
                                      int typeCode) {
        JPanel p = new JPanel();
        p.setBackground(Styles.BG_MAIN);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        boolean adminCtx = isAdminCatalogueContext();

        if (!items.isEmpty()) {
            JLabel sec = Styles.subtitleLabel(sectionTitle);
            sec.setAlignmentX(Component.LEFT_ALIGNMENT);
            sec.setBorder(new EmptyBorder(Styles.PADDING_MD, 0, Styles.PADDING_SM, 0));
            p.add(sec);

            // En contexte admin : en-tête avec colonne ID visible
            if (adminCtx) {
                JPanel header = new JPanel(new java.awt.GridLayout(1, 3, Styles.PADDING_MD, 0));
                header.setBackground(Styles.TEAL_SURFACE);
                header.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
                header.setPreferredSize(new Dimension(0, 30));
                header.setMinimumSize(new Dimension(0, 30));
                header.setAlignmentX(Component.LEFT_ALIGNMENT);
                for (String col : new String[]{"ID", "Nom / Titre", "Action rapide"}) {
                    JLabel h = new JLabel(col);
                    h.setFont(Styles.FONT_SMALL_BOLD);
                    h.setForeground(Styles.TEAL_DARK);
                    header.add(h);
                }
                p.add(header);
            }
        }

        for (T item : items) {
            int itemId = toId.apply(item);

            JPanel row;
            if (adminCtx) {
                row = new JPanel(new java.awt.GridLayout(1, 3, Styles.PADDING_MD, 0));
            } else {
                row = new JPanel(new BorderLayout());
            }
            row.setBackground(Styles.BG_ALT);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            row.setPreferredSize(new Dimension(0, adminCtx ? 44 : 52));
            row.setMinimumSize(new Dimension(0, adminCtx ? 44 : 52));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    row.setBackground(Styles.TEAL_SURFACE);
                    row.repaint();
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    row.setBackground(Styles.BG_ALT);
                    row.repaint();
                }
            });

            if (adminCtx) {
                // Colonne ID avec badge teal
                JLabel lblId = new JLabel("# " + itemId);
                lblId.setFont(Styles.FONT_SMALL_BOLD);
                lblId.setForeground(Styles.TEAL);
                row.add(lblId);

                // Colonne Nom
                JLabel lbl = Styles.bodyLabel(toLabel.apply(item));
                row.add(lbl);

                // Colonne Action : bouton Supprimer direct (remplissage automatique de l'ID)
                JButton btnSupprDirect = Styles.dangerButton("\u2212 Supprimer (ID " + itemId + ")");
                btnSupprDirect.setFont(Styles.FONT_SMALL);
                btnSupprDirect.addActionListener(e -> {
                    try { suppressionDirecteQueue.put(itemId); } catch (InterruptedException ignored) {}
                });
                row.add(btnSupprDirect);
            } else {
                JLabel lbl = Styles.bodyLabel(toLabel.apply(item));
                row.add(lbl, BorderLayout.CENTER);

                if (typeCode > 0) {
                    JButton btnDetail = Styles.secondaryButton("D\u00e9tails \u203a");
                    btnDetail.setFont(Styles.FONT_SMALL);
                    int encoded = typeCode * 1_000_000 + itemId;
                    btnDetail.addActionListener(e -> {
                        try { catalogueIntQueue.put(encoded); } catch (InterruptedException ignored) {}
                    });
                    row.add(btnDetail, BorderLayout.EAST);
                }
            }
            p.add(row);
        }

        if (items.isEmpty()) {
            JLabel vide = Styles.mutedLabel("Aucun \u00e9l\u00e9ment \u00e0 afficher.");
            vide.setAlignmentX(Component.LEFT_ALIGNMENT);
            vide.setBorder(new EmptyBorder(Styles.PADDING_MD, 0, 0, 0));
            p.add(vide);
        }
        return p;
    }

    // Surcharge sans typeCode (pas de bouton Détails) pour les listes contextuelles
    private <T> JPanel buildListPanel(String sectionTitle, List<T> items,
                                      java.util.function.Function<T, String> toLabel,
                                      java.util.function.Function<T, Integer> toId) {
        return buildListPanel(sectionTitle, items, toLabel, toId, 0);
    }

    /** Construit un panneau de détails carte (morceau, album, artiste ou groupe). */
    private JPanel buildDetailPanel(String titre, java.util.List<String[]> lignes) {
        JPanel p = new JPanel();
        p.setBackground(Styles.BG_MAIN);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // Bouton retour avec icône flèche
        JButton btnBack = Styles.secondaryButton("\u2190 Retour \u00e0 la liste");
        btnBack.setFont(Styles.FONT_SMALL);
        btnBack.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnBack.setMaximumSize(new Dimension(180, 34));
        btnBack.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Styles.BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        btnBack.setForeground(Styles.TEXT_MUTED);
        btnBack.addActionListener(e -> {
            try { catalogueIntQueue.put(5); } catch (InterruptedException ignored) {}
        });
        p.add(btnBack);
        p.add(Box.createVerticalStrut(Styles.PADDING_MD));

        JLabel t = Styles.titleLabel(titre);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setBorder(new EmptyBorder(0, 0, Styles.PADDING_MD, 0));
        p.add(t);

        // Carte avec barre d'accent teal à gauche pour un style plus dynamique
        JPanel card = Styles.accentCardPanel(Styles.TEAL);
        card.setLayout(new GridBagLayout());
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(620, Integer.MAX_VALUE));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 10, 6, 20);

        int row = 0;
        for (String[] kv : lignes) {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
            JLabel key = new JLabel(kv[0]);
            key.setFont(Styles.FONT_SMALL_BOLD);
            key.setForeground(Styles.TEXT_MUTED);
            card.add(key, gbc);

            gbc.gridx = 1; gbc.weightx = 1;
            // L'ID est affiché comme un badge coloré
            JComponent valComp;
            if (kv[0].equals("ID")) {
                valComp = Styles.badgeLabel("# " + kv[1]);
            } else {
                JLabel val = new JLabel(kv[1]);
                val.setFont(Styles.FONT_BODY);
                val.setForeground(Styles.TEXT);
                valComp = val;
            }
            card.add(valComp, gbc);
            row++;
        }
        p.add(card);
        return p;
    }

    // ==================== CARTE PLAYLISTS ====================

    private JPanel          playlistsListPanel;   // panneau gauche : liste des playlists
    private JPanel          playlistsDetailPanel; // panneau droit  : contenu playlist sélectionnée
    private CardLayout      playlistsDetailLayout;
    private JLabel          playlistsStatusLabel;
    private JPanel          playlistsFormPanel;   // formulaire inline création/renommage
    private JTextField      playlistsFormField;   // champ texte du formulaire
    private JLabel          playlistsFormTitle;   // titre du formulaire ("Nouvelle playlist" / "Renommer")
    // --- Lecteur inline (visible pendant la lecture d'une playlist) ---
    private JPanel          playlistsLecteurPanel;
    private JLabel          playlistsLecteurTitre;
    private JLabel          playlistsLecteurArtiste;
    private JProgressBar    playlistsLecteurBar;
    private JButton         playlistsLecteurPrev;
    private JButton         playlistsLecteurNext;
    private JButton         playlistsLecteurStop;
    private JButton         playlistsLecteurPause;
    private volatile boolean lecteurEnPause = false;
    /** Queue pour les contrôles du lecteur playlist : 1=prev, 2=next, 3=stop, 4=pause/reprise */
    private final java.util.concurrent.LinkedBlockingQueue<Integer> lecteurPlaylistQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();
    private final java.util.concurrent.LinkedBlockingQueue<Object> playlistsQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();
    private final java.util.concurrent.SynchronousQueue<String> playlistsNomQueue =
            new java.util.concurrent.SynchronousQueue<>();
    private List<model.Playlist> playlistsCourantes = new java.util.ArrayList<>();
    private model.Playlist       playlistSelectionnee = null;
    /** Id du morceau sélectionné via le picker — résolu avant de pousser dans playlistsQueue. */
    private volatile int morceauChoisiId = -1;
    /** true quand on est en train d'ajouter un morceau depuis la vue playlists —
     *  permet de court-circuiter demanderRechercheMusique() qui naviguerait vers Écouter. */
    private volatile boolean contexteAjoutPlaylist = false;

    private JPanel buildPlaylistsCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Styles.BG_MAIN);

        // ===== BANDEAU STATUS en haut =====
        playlistsStatusLabel = new JLabel(" ");
        playlistsStatusLabel.setFont(Styles.FONT_SMALL_BOLD);
        playlistsStatusLabel.setForeground(new Color(22, 163, 74));
        playlistsStatusLabel.setOpaque(true);
        playlistsStatusLabel.setBackground(Styles.BG_ALT);
        playlistsStatusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(Styles.PADDING_SM, Styles.PADDING_LG,
                        Styles.PADDING_SM, Styles.PADDING_LG)));
        card.add(playlistsStatusLabel, BorderLayout.NORTH);

        // ===== SPLIT : liste gauche | détail droit =====
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBorder(null);
        split.setDividerSize(1);
        split.setBackground(new Color(220, 225, 230));
        split.setResizeWeight(0.0);

        // ---- Panneau GAUCHE : liste des playlists ----
        JPanel leftOuter = new JPanel(new BorderLayout());
        leftOuter.setBackground(Styles.BG_ALT);
        leftOuter.setPreferredSize(new Dimension(280, 0));
        leftOuter.setMinimumSize(new Dimension(240, 0));

        // Header gauche
        JPanel leftTop = new JPanel();
        leftTop.setBackground(Styles.BG_ALT);
        leftTop.setLayout(new BoxLayout(leftTop, BoxLayout.Y_AXIS));

        JPanel leftHeader = new JPanel(new BorderLayout());
        leftHeader.setBackground(Styles.BG_ALT);
        leftHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftHeader.setBorder(new EmptyBorder(Styles.PADDING_LG, Styles.PADDING_LG,
                Styles.PADDING_MD, Styles.PADDING_LG));
        JLabel titreGauche = Styles.subtitleLabel("Mes playlists");
        leftHeader.add(titreGauche, BorderLayout.CENTER);
        JButton btnNew = Styles.primaryButton("+ Nouvelle");
        btnNew.setFont(Styles.FONT_SMALL_BOLD);
        btnNew.addActionListener(e -> {
            // Pousser "new" pour débloquer afficherMenuPlaylist() → case 1 → creerPlaylist()
            // Le formulaire inline sera affiché par demanderNomPlaylist()
            try { playlistsQueue.put("new"); } catch (InterruptedException ignored) {}
        });
        leftHeader.add(btnNew, BorderLayout.EAST);
        leftTop.add(leftHeader);

        // Formulaire inline (caché par défaut)
        playlistsFormPanel = new JPanel(new BorderLayout(Styles.PADDING_SM, 0));
        playlistsFormPanel.setBackground(new Color(240, 248, 248));
        playlistsFormPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, Styles.TEAL),
                BorderFactory.createEmptyBorder(Styles.PADDING_SM, Styles.PADDING_LG,
                        Styles.PADDING_SM, Styles.PADDING_LG)));
        playlistsFormPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        playlistsFormPanel.setVisible(false);

        playlistsFormTitle = new JLabel("Nouvelle playlist");
        playlistsFormTitle.setFont(Styles.FONT_SMALL_BOLD);
        playlistsFormTitle.setForeground(Styles.TEAL_DARK);

        playlistsFormField = new JTextField();
        playlistsFormField.setFont(Styles.FONT_SMALL);
        playlistsFormField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Styles.TEAL, 1, true),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JPanel formBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        formBtns.setOpaque(false);
        JButton btnValiderForm = Styles.primaryButton("OK");
        btnValiderForm.setFont(Styles.FONT_SMALL_BOLD);
        btnValiderForm.setPreferredSize(new Dimension(44, 28));
        JButton btnAnnulerForm = Styles.secondaryButton("✕");
        btnAnnulerForm.setFont(Styles.FONT_SMALL);
        btnAnnulerForm.setPreferredSize(new Dimension(36, 28));
        formBtns.add(btnValiderForm);
        formBtns.add(btnAnnulerForm);

        JPanel formCenter = new JPanel(new BorderLayout(4, 2));
        formCenter.setOpaque(false);
        formCenter.add(playlistsFormTitle, BorderLayout.NORTH);
        formCenter.add(playlistsFormField, BorderLayout.CENTER);
        playlistsFormPanel.add(formCenter, BorderLayout.CENTER);
        playlistsFormPanel.add(formBtns, BorderLayout.EAST);

        // Valider : pousser le texte dans playlistsNomQueue
        Runnable validerForm = () -> {
            String val = playlistsFormField.getText().trim();
            if (!val.isEmpty()) {
                playlistsFormPanel.setVisible(false);
                playlistsFormField.setText("");
                try { playlistsNomQueue.put(val); } catch (InterruptedException ignored) {}
            }
        };
        btnValiderForm.addActionListener(e -> validerForm.run());
        playlistsFormField.addActionListener(e -> validerForm.run()); // Entrée = valider

        btnAnnulerForm.addActionListener(e -> {
            playlistsFormPanel.setVisible(false);
            playlistsFormField.setText("");
            try { playlistsNomQueue.put(""); } catch (InterruptedException ignored) {}
        });

        leftTop.add(playlistsFormPanel);
        leftOuter.add(leftTop, BorderLayout.NORTH);

        // Zone liste scrollable
        playlistsListPanel = new JPanel();
        playlistsListPanel.setBackground(Styles.BG_ALT);
        playlistsListPanel.setLayout(new BoxLayout(playlistsListPanel, BoxLayout.Y_AXIS));
        playlistsListPanel.setBorder(new EmptyBorder(0, Styles.PADDING_SM, Styles.PADDING_SM, Styles.PADDING_SM));

        ScrollablePanel listWrapper = new ScrollablePanel();
        listWrapper.setBackground(Styles.BG_ALT);
        listWrapper.setLayout(new BoxLayout(listWrapper, BoxLayout.Y_AXIS));
        listWrapper.add(playlistsListPanel);
        listWrapper.add(Box.createVerticalGlue());

        JScrollPane listScroll = new JScrollPane(listWrapper);
        listScroll.setBorder(null);
        listScroll.getViewport().setBackground(Styles.BG_ALT);
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftOuter.add(listScroll, BorderLayout.CENTER);

        // ---- Panneau DROIT : détail de la playlist sélectionnée ----
        playlistsDetailLayout = new CardLayout();
        playlistsDetailPanel = new JPanel(playlistsDetailLayout);
        playlistsDetailPanel.setBackground(Styles.BG_MAIN);

        // Carte vide (aucune playlist sélectionnée)
        JPanel accueilDetail = new JPanel(new GridBagLayout());
        accueilDetail.setBackground(Styles.BG_MAIN);
        JLabel hintDetail = new JLabel("\u2190  S\u00e9lectionnez une playlist");
        hintDetail.setFont(Styles.FONT_BODY);
        hintDetail.setForeground(Styles.TEXT_MUTED);
        accueilDetail.add(hintDetail);
        playlistsDetailPanel.add(accueilDetail, "accueil");
        playlistsDetailLayout.show(playlistsDetailPanel, "accueil");

        split.setLeftComponent(leftOuter);
        split.setRightComponent(playlistsDetailPanel);
        card.add(split, BorderLayout.CENTER);

        // ===== LECTEUR INLINE en bas (visible pendant la lecture) =====
        playlistsLecteurPanel = new JPanel(new BorderLayout(Styles.PADDING_MD, 0));
        playlistsLecteurPanel.setBackground(new Color(18, 90, 91));
        playlistsLecteurPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, Styles.TEAL_DARK),
                BorderFactory.createEmptyBorder(Styles.PADDING_MD, Styles.PADDING_LG,
                        Styles.PADDING_MD, Styles.PADDING_LG)));
        playlistsLecteurPanel.setVisible(false);

        // Icône musicale
        JLabel ico = new JLabel("~J~");
        ico.setFont(new Font(Styles.FONT_BODY.getFamily(), Font.BOLD, 14));
        ico.setForeground(new Color(130, 220, 220));
        ico.setBorder(new EmptyBorder(0, 0, 0, Styles.PADDING_MD));
        playlistsLecteurPanel.add(ico, BorderLayout.WEST);

        // Infos titre + barre de progression
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        playlistsLecteurTitre = new JLabel("—");
        playlistsLecteurTitre.setFont(Styles.FONT_SMALL_BOLD);
        playlistsLecteurTitre.setForeground(Color.WHITE);
        playlistsLecteurTitre.setAlignmentX(Component.LEFT_ALIGNMENT);

        playlistsLecteurArtiste = new JLabel("");
        playlistsLecteurArtiste.setFont(new Font(Styles.FONT_SMALL.getFamily(), Font.PLAIN, 11));
        playlistsLecteurArtiste.setForeground(new Color(160, 220, 220));
        playlistsLecteurArtiste.setAlignmentX(Component.LEFT_ALIGNMENT);

        playlistsLecteurBar = new JProgressBar(0, 100);
        playlistsLecteurBar.setValue(0);
        playlistsLecteurBar.setStringPainted(false);
        playlistsLecteurBar.setForeground(new Color(0, 200, 180));
        playlistsLecteurBar.setBackground(new Color(40, 110, 112));
        playlistsLecteurBar.setBorder(null);
        playlistsLecteurBar.setPreferredSize(new Dimension(0, 4));
        playlistsLecteurBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        playlistsLecteurBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(playlistsLecteurTitre);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(playlistsLecteurArtiste);
        infoPanel.add(Box.createVerticalStrut(6));
        infoPanel.add(playlistsLecteurBar);
        playlistsLecteurPanel.add(infoPanel, BorderLayout.CENTER);

        // Boutons de contrôle
        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Styles.PADDING_SM, 0));
        ctrlPanel.setOpaque(false);

        playlistsLecteurPrev  = new JButton("|<");
        playlistsLecteurPause = new JButton("||");
        playlistsLecteurNext  = new JButton(">|");
        playlistsLecteurStop  = new JButton("Stop");

        for (JButton b : new JButton[]{playlistsLecteurPrev, playlistsLecteurPause, playlistsLecteurNext, playlistsLecteurStop}) {
            b.setFont(new Font(Styles.FONT_SMALL.getFamily(), Font.BOLD, 13));
            b.setForeground(Color.WHITE);
            b.setBackground(new Color(0, 100, 100));
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 150, 150), 1, true),
                    BorderFactory.createEmptyBorder(5, 12, 5, 12)));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        playlistsLecteurPrev.addActionListener(e -> {
            lecteurEnPause = false;
            lecteurPlaylistQueue.offer(1);
        });
        playlistsLecteurPause.addActionListener(e -> {
            lecteurEnPause = !lecteurEnPause;
            playlistsLecteurPause.setText(lecteurEnPause ? ">" : "||");
        });
        playlistsLecteurNext.addActionListener(e -> {
            lecteurEnPause = false;
            lecteurPlaylistQueue.offer(2);
        });
        playlistsLecteurStop.addActionListener(e -> {
            lecteurEnPause = false;
            lecteurPlaylistQueue.offer(3);
        });

        ctrlPanel.add(playlistsLecteurPrev);
        ctrlPanel.add(playlistsLecteurPause);
        ctrlPanel.add(playlistsLecteurNext);
        ctrlPanel.add(playlistsLecteurStop);
        playlistsLecteurPanel.add(ctrlPanel, BorderLayout.EAST);

        card.add(playlistsLecteurPanel, BorderLayout.SOUTH);
        return card;
    }

    /** Reconstruit le panneau gauche avec la liste des playlists. */
    private void refreshPlaylistsUI(List<model.Playlist> playlists) {
        playlistsCourantes = playlists;
        runOnEdt(() -> {
            playlistsListPanel.removeAll();
            if (playlists.isEmpty()) {
                JLabel vide = Styles.mutedLabel("Aucune playlist.\nCliquez sur + Nouvelle.");
                vide.setFont(Styles.FONT_SMALL);
                vide.setAlignmentX(Component.LEFT_ALIGNMENT);
                vide.setBorder(new EmptyBorder(Styles.PADDING_MD, Styles.PADDING_SM, 0, 0));
                playlistsListPanel.add(vide);
                // Côté droit : hint
                playlistsDetailLayout.show(playlistsDetailPanel, "accueil");
                playlistSelectionnee = null;
            } else {
                for (model.Playlist p : playlists) {
                    playlistsListPanel.add(buildPlaylistSideItem(p));
                    playlistsListPanel.add(Box.createVerticalStrut(2));
                }
                // Si une playlist était sélectionnée, rafraîchir son détail
                if (playlistSelectionnee != null) {
                    model.Playlist updated = playlists.stream()
                            .filter(p -> p.getId() == playlistSelectionnee.getId())
                            .findFirst().orElse(null);
                    if (updated != null) showPlaylistDetail(updated);
                }
            }
            playlistsListPanel.revalidate();
            playlistsListPanel.repaint();
        });
    }

    /** Construit une entrée de playlist dans le panneau gauche. */
    private JPanel buildPlaylistSideItem(model.Playlist p) {
        boolean selected = playlistSelectionnee != null && playlistSelectionnee.getId() == p.getId();
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(selected ? Styles.TEAL_SURFACE : Styles.BG_ALT);
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, selected ? 3 : 0, 0, 0, Styles.TEAL),
                BorderFactory.createEmptyBorder(Styles.PADDING_SM, Styles.PADDING_MD,
                        Styles.PADDING_SM, Styles.PADDING_SM)));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel nom = new JLabel(p.getNom());
        nom.setFont(Styles.FONT_SMALL_BOLD);
        nom.setForeground(Styles.TEXT);
        JLabel details = new JLabel(p.getMorceaux().size() + " morceau(x)  \u2022  " + p.getDureeTotaleFormatee());
        details.setFont(new Font(Styles.FONT_SMALL.getFamily(), Font.PLAIN, 11));
        details.setForeground(Styles.TEXT_MUTED);
        info.add(nom);
        info.add(details);
        item.add(info, BorderLayout.CENTER);

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                playlistSelectionnee = p;
                showPlaylistDetail(p);
                // Rafraîchir la sélection visuelle
                refreshPlaylistsUI(playlistsCourantes);
            }
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (playlistSelectionnee == null || playlistSelectionnee.getId() != p.getId())
                    item.setBackground(new Color(240, 248, 248));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (playlistSelectionnee == null || playlistSelectionnee.getId() != p.getId())
                    item.setBackground(Styles.BG_ALT);
            }
        });
        return item;
    }

    /** Affiche le détail d'une playlist dans le panneau droit. */
    /** Affiche le formulaire inline dans le panneau gauche.
     *  mode = "new" (créer) ou "rename" (renommer). */
    private void showPlaylistForm(String mode, String valeurInitiale) {
        if (playlistsFormPanel == null) return;
        playlistsFormTitle.setText("new".equals(mode) ? "Nom de la nouvelle playlist" : "Nouveau nom");
        playlistsFormField.setText(valeurInitiale);
        playlistsFormPanel.setVisible(true);
        playlistsFormPanel.revalidate();
        playlistsFormPanel.repaint();
        playlistsFormField.requestFocusInWindow();
        playlistsFormField.selectAll();
        // Si appelé depuis le bouton "+ Nouvelle", pousser "new" dans playlistsQueue
        if ("new".equals(mode)) {
            // On ne pousse pas ici : le bouton btnNew le fera via addActionListener
            // mais dans ce cas c'est demanderNomPlaylist() qui appelle showPlaylistForm
            // → pas besoin de pousser dans playlistsQueue ici
        }
    }

    private void showPlaylistDetail(model.Playlist p) {
        // Construire le panneau de détail
        JPanel detail = new JPanel(new BorderLayout());
        detail.setBackground(Styles.BG_MAIN);
        detail.setName("detail");

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Styles.BG_MAIN);
        header.setBorder(new EmptyBorder(Styles.PADDING_LG, Styles.PADDING_LG,
                Styles.PADDING_MD, Styles.PADDING_LG));

        JPanel headerLeft = new JPanel();
        headerLeft.setOpaque(false);
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));
        JLabel nom = Styles.subtitleLabel(p.getNom());
        JLabel infos = Styles.mutedLabel(p.getMorceaux().size() + " morceau(x)  \u2022  " + p.getDureeTotaleFormatee());
        infos.setFont(Styles.FONT_SMALL);
        headerLeft.add(nom);
        headerLeft.add(Box.createVerticalStrut(2));
        headerLeft.add(infos);
        header.add(headerLeft, BorderLayout.CENTER);

        // Boutons d'action sur la playlist
        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, Styles.PADDING_SM, 0));
        actionBtns.setOpaque(false);

        JButton btnAjouter   = Styles.primaryButton("+ Ajouter morceau");
        JButton btnRetirer   = Styles.secondaryButton("\u2212 Retirer morceau");
        JButton btnEcouter   = Styles.primaryButton("Ecouter");
        JButton btnRenommer  = Styles.secondaryButton("Renommer");
        JButton btnSupprimer = Styles.dangerButton("Supprimer");

        for (JButton b : new JButton[]{btnAjouter, btnRetirer, btnEcouter, btnRenommer, btnSupprimer})
            b.setFont(Styles.FONT_SMALL);

        btnAjouter.addActionListener(e -> {
            List<model.Morceau> catalogue = model.Catalogue.getTousLesMorceaux();
            morceauChoisiId = showMorceauPicker(catalogue, "Ajouter un morceau", "Choisir un morceau à ajouter");
            if (morceauChoisiId == -1) return; // annulé
            contexteAjoutPlaylist = true;
            try { playlistsQueue.put("ajouter:" + p.getId()); } catch (InterruptedException ignored) {}
        });
        btnRetirer.addActionListener(e -> {
            if (p.getMorceaux().isEmpty()) return;
            morceauChoisiId = showMorceauPicker(p.getMorceaux(), "Retirer un morceau", "Choisir le morceau à retirer");
            if (morceauChoisiId == -1) return; // annulé
            contexteAjoutPlaylist = true;
            try { playlistsQueue.put("retirer:" + p.getId()); } catch (InterruptedException ignored) {}
        });
        btnEcouter .addActionListener(e -> { try { playlistsQueue.put("ecouter:"  + p.getId()); } catch (InterruptedException ignored) {} });
        btnRenommer.addActionListener(e -> { try { playlistsQueue.put("renommer:" + p.getId()); } catch (InterruptedException ignored) {} });
        btnSupprimer.addActionListener(e -> { try { playlistsQueue.put("supprimer:" + p.getId()); } catch (InterruptedException ignored) {} });

        actionBtns.add(btnAjouter);
        actionBtns.add(btnRetirer);
        actionBtns.add(btnEcouter);
        actionBtns.add(btnRenommer);
        actionBtns.add(btnSupprimer);
        header.add(actionBtns, BorderLayout.SOUTH);
        detail.add(header, BorderLayout.NORTH);

        // Liste des morceaux
        JPanel morceauxPanel = new JPanel();
        morceauxPanel.setBackground(Styles.BG_MAIN);
        morceauxPanel.setLayout(new BoxLayout(morceauxPanel, BoxLayout.Y_AXIS));
        morceauxPanel.setBorder(new EmptyBorder(0, Styles.PADDING_LG, Styles.PADDING_LG, Styles.PADDING_LG));

        List<model.Morceau> morceaux = p.getMorceaux();
        if (morceaux.isEmpty()) {
            JLabel vide = Styles.mutedLabel("Aucun morceau dans cette playlist.");
            vide.setAlignmentX(Component.LEFT_ALIGNMENT);
            morceauxPanel.add(vide);
        } else {
            // En-tête
            JPanel colHeader = new JPanel(new java.awt.GridLayout(1, 3, Styles.PADDING_MD, 0));
            colHeader.setBackground(Styles.TEAL_SURFACE);
            colHeader.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            colHeader.setPreferredSize(new Dimension(0, 30));
            colHeader.setMinimumSize(new Dimension(0, 30));
            colHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (String col : new String[]{"#", "Titre / Artiste", "Dur\u00e9e"}) {
                JLabel h = new JLabel(col);
                h.setFont(Styles.FONT_SMALL_BOLD);
                h.setForeground(Styles.TEAL_DARK);
                colHeader.add(h);
            }
            morceauxPanel.add(colHeader);
            morceauxPanel.add(Box.createVerticalStrut(2));

            int num = 1;
            for (model.Morceau m : morceaux) {
                JPanel row = new JPanel(new java.awt.GridLayout(1, 3, Styles.PADDING_MD, 0));
                boolean alt = (num % 2 == 0);
                row.setBackground(alt ? Styles.BG_ALT : Styles.BG_MAIN);
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 238, 240)),
                        BorderFactory.createEmptyBorder(8, 14, 8, 14)));
                row.setPreferredSize(new Dimension(0, 40));
                row.setMinimumSize(new Dimension(0, 40));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel lblNum = new JLabel(String.valueOf(num++));
                lblNum.setFont(Styles.FONT_SMALL);
                lblNum.setForeground(Styles.TEXT_MUTED);

                JLabel lblTitre = new JLabel(m.getTitre());
                lblTitre.setFont(Styles.FONT_SMALL_BOLD);
                lblTitre.setForeground(Styles.TEXT);

                int sec = m.getDuree();
                JLabel lblDuree = new JLabel(sec / 60 + ":" + String.format("%02d", sec % 60));
                lblDuree.setFont(Styles.FONT_SMALL);
                lblDuree.setForeground(Styles.TEXT_MUTED);

                row.add(lblNum);
                row.add(lblTitre);
                row.add(lblDuree);
                morceauxPanel.add(row);
            }
        }

        ScrollablePanel detailWrapper = new ScrollablePanel();
        detailWrapper.setBackground(Styles.BG_MAIN);
        detailWrapper.setLayout(new BoxLayout(detailWrapper, BoxLayout.Y_AXIS));
        detailWrapper.add(morceauxPanel);
        detailWrapper.add(Box.createVerticalGlue());

        JScrollPane detailScroll = new JScrollPane(detailWrapper);
        detailScroll.setBorder(null);
        detailScroll.getViewport().setBackground(Styles.BG_MAIN);
        detailScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.getVerticalScrollBar().setUnitIncrement(16);
        detail.add(detailScroll, BorderLayout.CENTER);

        // Remplacer la carte "detail" dans le CardLayout
        for (Component c : playlistsDetailPanel.getComponents()) {
            if ("detail".equals(c.getName())) {
                playlistsDetailPanel.remove(c);
                break;
            }
        }
        detail.setName("detail");
        playlistsDetailPanel.add(detail, "detail");
        playlistsDetailLayout.show(playlistsDetailPanel, "detail");
        playlistsDetailPanel.revalidate();
        playlistsDetailPanel.repaint();
    }

    // ==================== CARTE ÉCOUTE ====================

    // ==================== CARTE ÉCOUTE ====================

    private JTextField      ecouteSearchField;
    private JPanel          ecouteContentArea;
    private JProgressBar    ecouteProgressBar;
    private JLabel          ecouteTitreCourant;
    private JLabel          ecouteInterpreteLabel;
    private JLabel          ecouteLimiteLabel;
    private JPanel          ecoutePlayerPanel;
    private JButton         ecouteBtnPrev;
    private JButton         ecouteBtnPause;
    private JButton         ecouteBtnNext;
    private JButton         ecouteBtnStop;
    private volatile boolean ecouteEnPause = false;
    /** Queue contrôles lecteur standalone : "pause", "stop", "prev", "next" */
    private final java.util.concurrent.LinkedBlockingQueue<String> ecouteControleQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();
    private final java.util.concurrent.LinkedBlockingQueue<Object> ecouteQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();

    private JPanel buildEcouteCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Styles.BG_MAIN);

        // ---- Barre haute : recherche ----
        JPanel topBar = new JPanel();
        topBar.setBackground(Styles.BG_MAIN);
        topBar.setLayout(new BoxLayout(topBar, BoxLayout.Y_AXIS));
        topBar.setBorder(new EmptyBorder(Styles.PADDING_LG * 2, Styles.PADDING_LG * 2,
                Styles.PADDING_MD, Styles.PADDING_LG * 2));

        JLabel titre = Styles.titleLabel("\u00c9couter un morceau");
        titre.setAlignmentX(Component.LEFT_ALIGNMENT);
        topBar.add(titre);
        topBar.add(Box.createVerticalStrut(Styles.PADDING_SM));

        ecouteLimiteLabel = Styles.mutedLabel("");
        ecouteLimiteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topBar.add(ecouteLimiteLabel);
        topBar.add(Box.createVerticalStrut(Styles.PADDING_MD));

        JPanel searchBar = new JPanel(new BorderLayout(Styles.PADDING_SM, 0));
        searchBar.setBackground(Styles.BG_MAIN);
        searchBar.setMaximumSize(new Dimension(500, 42));
        searchBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        ecouteSearchField = buildTextField(400);
        ecouteSearchField.setToolTipText("Rechercher un morceau...");
        searchBar.add(ecouteSearchField, BorderLayout.CENTER);

        JButton btnSearch = Styles.primaryButton("Rechercher");
        btnSearch.setPreferredSize(new Dimension(130, 40));
        searchBar.add(btnSearch, BorderLayout.EAST);
        topBar.add(searchBar);

        JPanel topBarWrapper3 = new JPanel(new BorderLayout());
        topBarWrapper3.setBackground(Styles.BG_MAIN);
        topBarWrapper3.add(topBar, BorderLayout.CENTER);
        topBarWrapper3.add(buildFloatingNotesPanel(), BorderLayout.EAST);
        card.add(topBarWrapper3, BorderLayout.NORTH);

        // ---- Zone résultats ----
        ecouteContentArea = new JPanel();
        ecouteContentArea.setBackground(Styles.BG_MAIN);
        ecouteContentArea.setLayout(new BoxLayout(ecouteContentArea, BoxLayout.Y_AXIS));
        ecouteContentArea.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2,
                Styles.PADDING_MD, Styles.PADDING_LG * 2));

        ScrollablePanel ecouteWrapper = new ScrollablePanel();
        ecouteWrapper.setBackground(Styles.BG_MAIN);
        ecouteWrapper.setLayout(new BoxLayout(ecouteWrapper, BoxLayout.Y_AXIS));
        ecouteWrapper.add(ecouteContentArea);
        ecouteWrapper.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(ecouteWrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Styles.BG_MAIN);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        // ---- Lecteur en bas ----
        ecoutePlayerPanel = new JPanel(new BorderLayout(Styles.PADDING_MD, 0));
        ecoutePlayerPanel.setBackground(Styles.BG_ALT);
        ecoutePlayerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Styles.BORDER),
                BorderFactory.createEmptyBorder(Styles.PADDING_MD, Styles.PADDING_LG * 2,
                        Styles.PADDING_MD, Styles.PADDING_LG * 2)));
        ecoutePlayerPanel.setVisible(false);

        // Infos titre + barre
        JPanel ecouteInfoPanel = new JPanel();
        ecouteInfoPanel.setOpaque(false);
        ecouteInfoPanel.setLayout(new BoxLayout(ecouteInfoPanel, BoxLayout.Y_AXIS));

        ecouteTitreCourant = new JLabel("—");
        ecouteTitreCourant.setFont(Styles.FONT_BODY_BOLD);
        ecouteTitreCourant.setForeground(Styles.TEXT);
        ecouteTitreCourant.setAlignmentX(Component.LEFT_ALIGNMENT);

        ecouteInterpreteLabel = new JLabel("");
        ecouteInterpreteLabel.setFont(Styles.FONT_SMALL);
        ecouteInterpreteLabel.setForeground(Styles.TEXT_MUTED);
        ecouteInterpreteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ecouteProgressBar = new JProgressBar(0, 100);
        ecouteProgressBar.setStringPainted(false);
        ecouteProgressBar.setForeground(Styles.TEAL);
        ecouteProgressBar.setBackground(Styles.BORDER);
        ecouteProgressBar.setBorderPainted(false);
        ecouteProgressBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 6));
        ecouteProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        ecouteProgressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        ecouteInfoPanel.add(ecouteTitreCourant);
        ecouteInfoPanel.add(Box.createVerticalStrut(2));
        ecouteInfoPanel.add(ecouteInterpreteLabel);
        ecouteInfoPanel.add(Box.createVerticalStrut(Styles.PADDING_SM));
        ecouteInfoPanel.add(ecouteProgressBar);
        ecoutePlayerPanel.add(ecouteInfoPanel, BorderLayout.CENTER);

        // Boutons de contrôle
        JPanel ecouteCtrlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Styles.PADDING_SM, 0));
        ecouteCtrlPanel.setOpaque(false);

        ecouteBtnPrev  = new JButton("|<");
        ecouteBtnPause = new JButton("||");
        ecouteBtnNext  = new JButton(">|");
        ecouteBtnStop  = new JButton("Stop");

        for (JButton b : new JButton[]{ecouteBtnPrev, ecouteBtnPause, ecouteBtnNext, ecouteBtnStop}) {
            b.setFont(Styles.FONT_SMALL_BOLD);
            b.setForeground(Styles.TEAL_DARK);
            b.setBackground(Styles.BG_MAIN);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Styles.TEAL, 1, true),
                    BorderFactory.createEmptyBorder(5, 14, 5, 14)));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        ecouteBtnPrev.addActionListener(e -> {
            ecouteEnPause = false;
            ecouteBtnPause.setText("||");
            ecouteControleQueue.offer("prev");
        });
        ecouteBtnPause.addActionListener(e -> {
            ecouteEnPause = !ecouteEnPause;
            ecouteBtnPause.setText(ecouteEnPause ? ">" : "||");
        });
        ecouteBtnNext.addActionListener(e -> {
            ecouteEnPause = false;
            ecouteBtnPause.setText("||");
            ecouteControleQueue.offer("next");
        });
        ecouteBtnStop.addActionListener(e -> {
            ecouteEnPause = false;
            ecouteControleQueue.offer("stop");
        });

        ecouteCtrlPanel.add(ecouteBtnPrev);
        ecouteCtrlPanel.add(ecouteBtnPause);
        ecouteCtrlPanel.add(ecouteBtnNext);
        ecouteCtrlPanel.add(ecouteBtnStop);
        ecoutePlayerPanel.add(ecouteCtrlPanel, BorderLayout.EAST);

        card.add(ecoutePlayerPanel, BorderLayout.SOUTH);

        // Actions recherche
        Runnable doSearch = () -> {
            String q = ecouteSearchField.getText().trim();
            if (!q.isEmpty()) {
                try { ecouteQueue.put("search:" + q); } catch (InterruptedException ignored) {}
            }
        };
        ecouteSearchField.addActionListener(e -> doSearch.run());
        btnSearch.addActionListener(e -> doSearch.run());

        return card;
    }

    // ==================== CARTE HISTORIQUE ====================

    private JPanel historiqueContentArea;

    private JPanel buildHistoriqueCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Styles.BG_MAIN);

        JPanel topBar = new JPanel();
        topBar.setBackground(Styles.BG_MAIN);
        topBar.setLayout(new BoxLayout(topBar, BoxLayout.Y_AXIS));
        topBar.setBorder(new EmptyBorder(Styles.PADDING_LG * 2, Styles.PADDING_LG * 2,
                Styles.PADDING_MD, Styles.PADDING_LG * 2));

        // Ligne titre + bouton supprimer
        JPanel titreRow = new JPanel(new BorderLayout());
        titreRow.setOpaque(false);
        titreRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titre = Styles.titleLabel("Mon historique d'\u00e9coute");
        titreRow.add(titre, BorderLayout.CENTER);

        JButton btnSupprimerHistorique = Styles.dangerButton("Effacer l'historique");
        btnSupprimerHistorique.setFont(Styles.FONT_SMALL);
        btnSupprimerHistorique.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    fenetre,
                    "Voulez-vous vraiment supprimer tout votre historique d'\u00e9coute ?",
                    "Confirmer la suppression",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION && utilisateurId > 0) {
                model.Historique.supprimerHistoriqueClient(utilisateurId);
                afficherHistorique(new java.util.ArrayList<>());
            }
        });
        titreRow.add(btnSupprimerHistorique, BorderLayout.EAST);
        topBar.add(titreRow);
        topBar.add(Box.createVerticalStrut(Styles.PADDING_SM));

        JLabel sub = Styles.mutedLabel("Les morceaux que vous avez \u00e9cout\u00e9s r\u00e9cemment.");
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        topBar.add(sub);

        JPanel topBarWrapper4 = new JPanel(new BorderLayout());
        topBarWrapper4.setBackground(Styles.BG_MAIN);
        topBarWrapper4.add(topBar, BorderLayout.CENTER);
        topBarWrapper4.add(buildFloatingNotesPanel(), BorderLayout.EAST);
        card.add(topBarWrapper4, BorderLayout.NORTH);

        historiqueContentArea = new JPanel();
        historiqueContentArea.setBackground(Styles.BG_MAIN);
        historiqueContentArea.setLayout(new BoxLayout(historiqueContentArea, BoxLayout.Y_AXIS));
        historiqueContentArea.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2,
                Styles.PADDING_LG, Styles.PADDING_LG * 2));

        ScrollablePanel historiqueWrapper = new ScrollablePanel();
        historiqueWrapper.setBackground(Styles.BG_MAIN);
        historiqueWrapper.setLayout(new BoxLayout(historiqueWrapper, BoxLayout.Y_AXIS));
        historiqueWrapper.add(historiqueContentArea);
        historiqueWrapper.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(historiqueWrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Styles.BG_MAIN);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ==================== CARTES ADMIN ====================

    // Queue partagée pour toutes les saisies admin (bloquantes, séquentielles)
    // LinkedBlockingQueue : le put() depuis l'EDT (sidebar) ne bloque jamais,
    // même si le thread contrôleur n'est pas encore en train de faire un take().
    private final java.util.concurrent.LinkedBlockingQueue<Object> adminQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();

    /**
     * Queue dédiée pour la suppression par clic direct sur une ligne de liste.
     * Quand l'utilisateur clique "Supprimer" sur une ligne de la liste admin,
     * l'ID est poussé ici. demanderIdSuppression() consomme depuis cette queue
     * en priorité (si non vide) avant d'ouvrir un dialog.
     */
    private final java.util.concurrent.LinkedBlockingQueue<Integer> suppressionDirecteQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();

    /**
     * Queue de navigation pour CLIENT et VISITEUR.
     * Utilisée pour débloquer afficherMenuCatalogue(), afficherMenuPlaylist(),
     * demanderRechercheMusique() quand l'utilisateur clique dans la sidebar.
     * Valeurs : "catalogue", "playlists", "ecoute", "historique", "deconnexion", "accueil"
     */
    private final java.util.concurrent.LinkedBlockingQueue<String> clientNavQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();

    // Références aux composants mis à jour dynamiquement
    private JPanel adminCatalogueContent;
    private JLabel adminCatalogueStatus;   // bandeau de feedback en haut de la carte admin
    private JButton btnAnnulerSuppression; // bouton Annuler affiché pendant une suppression
    private JPanel adminComptesContent;
    private JLabel adminComptesStatus;
    private JPanel adminStatsCard;   // le JPanel complet de la carte statistiques

    // ==================== FORMULAIRES ADMIN INLINE ====================
    //
    // Chaque action admin (ajouter morceau, supprimer album, etc.) affiche
    // un formulaire dans le panneau droit de la carte gestionCatalogue.
    // Les méthodes demanderXxx() bloquent sur formulaireQueue jusqu'à ce que
    // le formulaire soit validé (bouton Valider) ou annulé (bouton Annuler).
    //
    // Sélecteur artiste/groupe : deux radio buttons permettent de choisir
    // entre sélectionner un existant (JComboBox) ou créer un nouveau (champs inline).
    // Dans les deux cas la valeur finale est résolue avant d'être poussée dans la queue.

    /** Queue unique pour tous les champs de formulaire admin. Chaque demanderXxx()
     *  consomme exactement un élément. L'annulation pousse CANCEL_SENTINEL. */
    private final java.util.concurrent.LinkedBlockingQueue<Object> formulaireQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();

    /** Sentinel pour l'annulation dans formulaireQueue. */
    private static final Object CANCEL_SENTINEL = new Object();

    /** Panneau droit du split-pane admin, contient le formulaire contextuel. */
    private JPanel adminFormulairePanel;
    /** CardLayout du panneau formulaire. */
    private CardLayout adminFormulaireLayout;

    // ---- Carte : Gérer le catalogue ----
    private JPanel buildAdminCatalogueCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Styles.BG_MAIN);

        // ===== BANDEAU STATUS en haut (label + bouton Annuler) =====
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(Styles.BG_ALT);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(Styles.PADDING_SM, Styles.PADDING_LG,
                        Styles.PADDING_SM, Styles.PADDING_LG)));

        adminCatalogueStatus = new JLabel(" ");
        adminCatalogueStatus.setFont(Styles.FONT_SMALL_BOLD);
        adminCatalogueStatus.setForeground(Styles.TEXT_MUTED);
        statusBar.add(adminCatalogueStatus, BorderLayout.CENTER);

        btnAnnulerSuppression = Styles.secondaryButton("\u00d7  Annuler");
        btnAnnulerSuppression.setFont(Styles.FONT_SMALL);
        btnAnnulerSuppression.setVisible(false);
        btnAnnulerSuppression.addActionListener(e -> {
            suppressionDirecteQueue.offer(CANCEL_INT);
            btnAnnulerSuppression.setVisible(false);
            resetAdminCatalogueMsg();
            if (adminCatalogueContent != null) {
                adminCatalogueContent.removeAll();
                adminCatalogueContent.revalidate();
                adminCatalogueContent.repaint();
            }
            adminFormulaireLayout.show(adminFormulairePanel, "accueil");
        });
        statusBar.add(btnAnnulerSuppression, BorderLayout.EAST);
        card.add(statusBar, BorderLayout.NORTH);

        // ===== SPLIT : menu gauche | formulaire droit =====
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBorder(null);
        split.setDividerSize(1);
        split.setBackground(new Color(220, 225, 230));
        split.setResizeWeight(0.0);

        // ---- Panneau GAUCHE : menu des actions ----
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(Styles.BG_ALT);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(Styles.PADDING_LG, Styles.PADDING_LG,
                Styles.PADDING_LG, Styles.PADDING_LG));
        leftPanel.setPreferredSize(new Dimension(280, 0));
        leftPanel.setMinimumSize(new Dimension(260, 0));

        JLabel titreMenu = Styles.subtitleLabel("Catalogue");
        titreMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(titreMenu);
        leftPanel.add(Box.createVerticalStrut(Styles.PADDING_SM));
        JLabel subMenu = Styles.mutedLabel("Choisissez une action");
        subMenu.setFont(Styles.FONT_SMALL);
        subMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(subMenu);
        leftPanel.add(Box.createVerticalStrut(Styles.PADDING_LG));

        // Sections de boutons
        leftPanel.add(buildAdminMenuSection("Morceaux", new String[][]{
                {"+ Ajouter un morceau",       "ajouterMorceau"},
                {"\u2212 Supprimer un morceau","supprimerMorceau"},
        }));
        leftPanel.add(Box.createVerticalStrut(Styles.PADDING_MD));
        leftPanel.add(buildAdminMenuSection("Albums", new String[][]{
                {"+ Ajouter un album",         "ajouterAlbum"},
                {"\u2212 Supprimer un album",  "supprimerAlbum"},
        }));
        leftPanel.add(Box.createVerticalStrut(Styles.PADDING_MD));
        leftPanel.add(buildAdminMenuSection("Artistes", new String[][]{
                {"+ Ajouter un artiste",        "ajouterArtiste"},
                {"\u2212 Supprimer un artiste", "supprimerArtiste"},
        }));
        leftPanel.add(Box.createVerticalStrut(Styles.PADDING_MD));
        leftPanel.add(buildAdminMenuSection("Groupes", new String[][]{
                {"+ Ajouter un groupe",         "ajouterGroupe"},
                {"\u2212 Supprimer un groupe",  "supprimerGroupe"},
        }));
        leftPanel.add(Box.createVerticalStrut(Styles.PADDING_MD));
        leftPanel.add(buildAdminMenuSection("Associations", new String[][]{
                {"Morceau \u2192 Album",        "ajouterMorceauAlbum"},
                {"Artiste \u2192 Groupe",       "ajouterMembreGroupe"},
        }));
        leftPanel.add(Box.createVerticalGlue());

        // ---- Panneau DROIT : formulaires contextuels (CardLayout) ----
        adminFormulaireLayout = new CardLayout();
        adminFormulairePanel  = new JPanel(adminFormulaireLayout);
        adminFormulairePanel.setBackground(Styles.BG_MAIN);

        // Carte d'accueil (aucune action sélectionnée)
        JPanel accueilForm = new JPanel(new GridBagLayout());
        accueilForm.setBackground(Styles.BG_MAIN);
        JLabel hint = new JLabel("\u2190  S\u00e9lectionnez une action dans le menu");
        hint.setFont(Styles.FONT_BODY);
        hint.setForeground(Styles.TEXT_MUTED);
        accueilForm.add(hint);
        adminFormulairePanel.add(accueilForm, "accueil");

        // Zone de liste contextuelle (suppression / associations)
        adminCatalogueContent = new JPanel();
        adminCatalogueContent.setBackground(Styles.BG_MAIN);
        adminCatalogueContent.setLayout(new BoxLayout(adminCatalogueContent, BoxLayout.Y_AXIS));

        ScrollablePanel listWrapper = new ScrollablePanel();
        listWrapper.setBackground(Styles.BG_MAIN);
        listWrapper.setLayout(new BoxLayout(listWrapper, BoxLayout.Y_AXIS));
        listWrapper.add(adminCatalogueContent);
        listWrapper.add(Box.createVerticalGlue());
        JScrollPane listScroll = new JScrollPane(listWrapper);
        listScroll.setBorder(null);
        listScroll.getViewport().setBackground(Styles.BG_MAIN);
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.getVerticalScrollBar().setUnitIncrement(16);
        adminFormulairePanel.add(listScroll, "liste");

        adminFormulaireLayout.show(adminFormulairePanel, "accueil");

        split.setLeftComponent(leftPanel);
        split.setRightComponent(adminFormulairePanel);
        card.add(split, BorderLayout.CENTER);

        return card;
    }

    /** Construit une section du menu gauche admin (label + boutons empilés). */
    private JPanel buildAdminMenuSection(String titre, String[][] actions) {
        JPanel section = new JPanel();
        section.setBackground(Styles.BG_ALT);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JLabel lbl = new JLabel(titre.toUpperCase());
        lbl.setFont(new Font(Styles.FONT_SMALL_BOLD.getFamily(), Font.BOLD, 10));
        lbl.setForeground(Styles.TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 2, 4, 0));
        section.add(lbl);

        for (String[] a : actions) {
            boolean isDanger = a[0].startsWith("\u2212");
            JButton b = new JButton(a[0]);
            b.setFont(Styles.FONT_SMALL);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setOpaque(true);
            b.setBackground(Styles.BG_ALT);
            b.setForeground(isDanger ? Styles.DANGER : Styles.TEAL);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, isDanger ? Styles.DANGER : Styles.BG_ALT),
                    BorderFactory.createEmptyBorder(6, Styles.PADDING_SM, 6, Styles.PADDING_SM)));
            b.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    b.setBackground(isDanger ? new Color(255, 241, 241) : Styles.TEAL_SURFACE);
                    b.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 3, 0, 0, isDanger ? Styles.DANGER : Styles.TEAL),
                            BorderFactory.createEmptyBorder(6, Styles.PADDING_SM, 6, Styles.PADDING_SM)));
                }
                public void mouseExited(java.awt.event.MouseEvent e) {
                    b.setBackground(Styles.BG_ALT);
                    b.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 3, 0, 0, isDanger ? Styles.DANGER : Styles.BG_ALT),
                            BorderFactory.createEmptyBorder(6, Styles.PADDING_SM, 6, Styles.PADDING_SM)));
                }
            });
            final String key = a[1];
            b.addActionListener(e -> {
                // Afficher le formulaire correspondant dans le panneau droit
                showAdminFormulaire(key);
                // Pousser dans adminQueue pour débloquer afficherMenuAdmin()
                try { adminQueue.put(key); } catch (InterruptedException ignored) {}
            });
            section.add(b);
            section.add(Box.createVerticalStrut(2));
        }
        return section;
    }

    /**
     * Affiche le formulaire approprié dans le panneau droit de gestionCatalogue.
     * Appelé depuis l'EDT lors du clic sur un bouton du menu gauche.
     */
    private void showAdminFormulaire(String key) {
        // Pour les suppressions et associations : montrer la zone de liste
        // (le contrôleur va appeler afficherListeXxx() puis demanderIdSuppression())
        if (key.startsWith("supprimer") || key.equals("ajouterMorceauAlbum") || key.equals("ajouterMembreGroupe")) {
            // Vider la liste et basculer sur la carte "liste"
            if (adminCatalogueContent != null) {
                adminCatalogueContent.removeAll();
                adminCatalogueContent.revalidate();
                adminCatalogueContent.repaint();
            }
            adminFormulaireLayout.show(adminFormulairePanel, "liste");
        } else {
            // Pour les ajouts : construire le formulaire inline
            JPanel form = buildAdminForm(key);
            // Retirer l'ancienne carte "form" si elle existe, puis l'ajouter
            for (Component c : adminFormulairePanel.getComponents()) {
                if ("form".equals(c.getName())) {
                    adminFormulairePanel.remove(c);
                    break;
                }
            }
            form.setName("form");
            adminFormulairePanel.add(form, "form");
            adminFormulaireLayout.show(adminFormulairePanel, "form");
            adminFormulairePanel.revalidate();
            adminFormulairePanel.repaint();
        }
    }

    /**
     * Construit le formulaire inline pour une action d'ajout donnée.
     * Le formulaire pousse les valeurs dans formulaireQueue dans l'ordre
     * exact attendu par le contrôleur via les demanderXxx() overrides.
     */
    private JPanel buildAdminForm(String key) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Styles.BG_MAIN);

        JScrollPane scroll = new JScrollPane();
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Styles.BG_MAIN);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel form = new JPanel();
        form.setBackground(Styles.BG_MAIN);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(Styles.PADDING_LG, Styles.PADDING_LG * 2,
                Styles.PADDING_LG, Styles.PADDING_LG * 2));

        // Titre du formulaire
        String[] titreDesc = getFormTitreDesc(key);
        JLabel titreLabel = Styles.subtitleLabel(titreDesc[0]);
        titreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(titreLabel);
        form.add(Box.createVerticalStrut(4));
        JLabel descLabel = Styles.mutedLabel(titreDesc[1]);
        descLabel.setFont(Styles.FONT_SMALL);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(descLabel);
        form.add(Box.createVerticalStrut(Styles.PADDING_LG));

        // Liste des champs selon l'action
        List<Object[]> fields = getFormFields(key); // {label, type, ...}

        // Référence aux composants pour collecte à la validation
        List<Object[]> fieldRefs = new ArrayList<>(); // {type, composant(s)}

        for (Object[] f : fields) {
            String fieldType = (String) f[0];
            String fieldLabel = (String) f[1];

            JPanel row = buildFormRow(fieldType, fieldLabel, f, fieldRefs);
            if (row != null) {
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                form.add(row);
                form.add(Box.createVerticalStrut(Styles.PADDING_MD));
            }
        }

        form.add(Box.createVerticalStrut(Styles.PADDING_SM));

        // Séparateur
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(220, 225, 230));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sep);
        form.add(Box.createVerticalStrut(Styles.PADDING_MD));

        // Boutons Valider / Annuler
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.PADDING_SM, 0));
        btnRow.setBackground(Styles.BG_MAIN);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnValider  = Styles.primaryButton("Valider");
        JButton btnAnnuler  = Styles.secondaryButton("Annuler");
        btnValider.setFont(Styles.FONT_SMALL_BOLD);
        btnAnnuler.setFont(Styles.FONT_SMALL);
        btnRow.add(btnValider);
        btnRow.add(btnAnnuler);
        form.add(btnRow);

        // Label d'erreur inline
        JLabel errLabel = new JLabel(" ");
        errLabel.setFont(Styles.FONT_SMALL);
        errLabel.setForeground(Styles.DANGER);
        errLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(Box.createVerticalStrut(Styles.PADDING_SM));
        form.add(errLabel);

        // Action Valider : collecte + validation + push dans formulaireQueue
        btnValider.addActionListener(e -> {
            // Vider d'abord les éventuels résidus
            formulaireQueue.clear();
            List<Object> values = collectFormValues(fieldRefs, key, errLabel);
            if (values == null) return; // erreur de validation

            // Pousser toutes les valeurs dans l'ordre
            for (Object v : values) {
                formulaireQueue.offer(v);
            }
            // Revenir à l'accueil du formulaire
            adminFormulaireLayout.show(adminFormulairePanel, "accueil");
        });

        // Action Annuler
        btnAnnuler.addActionListener(e -> {
            formulaireQueue.clear();
            formulaireQueue.offer(CANCEL_SENTINEL);
            adminFormulaireLayout.show(adminFormulairePanel, "accueil");
            resetAdminCatalogueMsg();
        });

        scroll.setViewportView(form);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    /** Retourne titre + description pour chaque action. */
    private String[] getFormTitreDesc(String key) {
        switch (key) {
            case "ajouterMorceau":  return new String[]{"Ajouter un morceau",  "Remplissez les informations du nouveau morceau."};
            case "ajouterAlbum":    return new String[]{"Ajouter un album",    "Remplissez les informations du nouvel album."};
            case "ajouterArtiste":  return new String[]{"Ajouter un artiste",  "Remplissez les informations du nouvel artiste."};
            case "ajouterGroupe":   return new String[]{"Ajouter un groupe",   "Remplissez les informations du nouveau groupe."};
            default:                return new String[]{"Action",              ""};
        }
    }

    /**
     * Retourne la liste des champs pour un formulaire donné.
     * Format : {type, label, ...params optionnels}
     * Types : "text", "number", "artiste_ou_groupe", "album_select", "morceau_select", "groupe_select"
     */
    private List<Object[]> getFormFields(String key) {
        List<Object[]> fields = new ArrayList<>();
        switch (key) {
            case "ajouterMorceau":
                fields.add(new Object[]{"text",             "Titre"});
                fields.add(new Object[]{"number",           "Dur\u00e9e (en secondes)"});
                fields.add(new Object[]{"text",             "Genre"});
                fields.add(new Object[]{"number",           "Ann\u00e9e de sortie"});
                fields.add(new Object[]{"artiste_ou_groupe","Interpr\u00e8te"});
                break;
            case "ajouterAlbum":
                fields.add(new Object[]{"text",             "Titre"});
                fields.add(new Object[]{"number",           "Ann\u00e9e de sortie"});
                fields.add(new Object[]{"artiste_ou_groupe","Interpr\u00e8te"});
                break;
            case "ajouterArtiste":
                fields.add(new Object[]{"text",  "Nom"});
                fields.add(new Object[]{"text",  "Pr\u00e9nom"});
                fields.add(new Object[]{"text",  "Nationalit\u00e9"});
                break;
            case "ajouterGroupe":
                fields.add(new Object[]{"text",   "Nom du groupe"});
                fields.add(new Object[]{"number", "Ann\u00e9e de cr\u00e9ation"});
                fields.add(new Object[]{"text",   "Nationalit\u00e9"});
                break;
        }
        return fields;
    }

    /**
     * Construit un composant de saisie pour un champ.
     * fieldRefs est enrichi avec {type, composant(s)} pour la collecte.
     */
    private JPanel buildFormRow(String fieldType, String fieldLabel,
                                Object[] fieldDef, List<Object[]> fieldRefs) {
        JPanel row = new JPanel();
        row.setBackground(Styles.BG_MAIN);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(fieldLabel);
        lbl.setFont(Styles.FONT_SMALL_BOLD);
        lbl.setForeground(Styles.TEXT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(lbl);
        row.add(Box.createVerticalStrut(4));

        switch (fieldType) {
            case "text": {
                JTextField tf = new JTextField();
                tf.setFont(Styles.FONT_BODY);
                tf.setMaximumSize(new Dimension(420, 36));
                tf.setAlignmentX(Component.LEFT_ALIGNMENT);
                styleTextField(tf);
                row.add(tf);
                fieldRefs.add(new Object[]{"text", tf});
                break;
            }
            case "number": {
                JTextField tf = new JTextField();
                tf.setFont(Styles.FONT_BODY);
                tf.setMaximumSize(new Dimension(200, 36));
                tf.setAlignmentX(Component.LEFT_ALIGNMENT);
                styleTextField(tf);
                row.add(tf);
                fieldRefs.add(new Object[]{"number", tf});
                break;
            }
            case "artiste_ou_groupe": {
                // Deux radio buttons : Artiste existant | Groupe existant | Nouvel artiste | Nouveau groupe
                JPanel artGroupe = buildArtisteOuGroupeSelector();
                artGroupe.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.add(artGroupe);
                // Le sélecteur va s'auto-enregistrer; on stocke le panel
                fieldRefs.add(new Object[]{"artiste_ou_groupe", artGroupe});
                break;
            }
        }
        return row;
    }

    /**
     * Sélecteur "Interprète" avec 4 options en radio :
     *  - Artiste existant → JComboBox peuplé
     *  - Créer un nouvel artiste → 3 champs inline
     *  - Groupe existant → JComboBox peuplé
     *  - Créer un nouveau groupe → 3 champs inline
     *
     * Le panneau expose getSelectedArtisteId(), getSelectedGroupeId(),
     * getNewArtisteData(), getNewGroupeData() via le client tag "selector".
     */
    private JPanel buildArtisteOuGroupeSelector() {
        JPanel panel = new JPanel();
        panel.setBackground(Styles.BG_MAIN);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setMaximumSize(new Dimension(500, 400));

        // Fond teinté
        panel.setOpaque(true);
        panel.setBackground(Styles.BG_ALT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1, true),
                BorderFactory.createEmptyBorder(Styles.PADDING_MD, Styles.PADDING_MD,
                        Styles.PADDING_MD, Styles.PADDING_MD)));

        // ---- Radio buttons ----
        ButtonGroup bg = new ButtonGroup();
        JRadioButton rbArtisteEx  = new JRadioButton("Artiste existant");
        JRadioButton rbArtisteNew = new JRadioButton("Cr\u00e9er un nouvel artiste");
        JRadioButton rbGroupeEx   = new JRadioButton("Groupe existant");
        JRadioButton rbGroupeNew  = new JRadioButton("Cr\u00e9er un nouveau groupe");

        for (JRadioButton rb : new JRadioButton[]{rbArtisteEx, rbArtisteNew, rbGroupeEx, rbGroupeNew}) {
            rb.setFont(Styles.FONT_SMALL);
            rb.setBackground(Styles.BG_ALT);
            rb.setForeground(Styles.TEXT);
            bg.add(rb);
        }
        rbArtisteEx.setSelected(true);

        JPanel radioRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.PADDING_SM, 0));
        radioRow1.setBackground(Styles.BG_ALT);
        radioRow1.add(rbArtisteEx);
        radioRow1.add(rbArtisteNew);
        JPanel radioRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.PADDING_SM, 0));
        radioRow2.setBackground(Styles.BG_ALT);
        radioRow2.add(rbGroupeEx);
        radioRow2.add(rbGroupeNew);
        panel.add(radioRow1);
        panel.add(radioRow2);
        panel.add(Box.createVerticalStrut(Styles.PADDING_SM));

        // ---- Zone dynamique (CardLayout) ----
        CardLayout cl = new CardLayout();
        JPanel dynamicZone = new JPanel(cl);
        dynamicZone.setBackground(Styles.BG_ALT);
        dynamicZone.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Carte : artiste existant
        List<Artiste> artistes = Catalogue.getTousLesArtistes();
        JComboBox<String> cbArtiste = new JComboBox<>();
        cbArtiste.setFont(Styles.FONT_SMALL);
        cbArtiste.setMaximumSize(new Dimension(380, 32));
        if (artistes.isEmpty()) {
            cbArtiste.addItem("(aucun artiste dans le catalogue)");
        } else {
            for (Artiste a : artistes) cbArtiste.addItem("[" + a.getId() + "] " + a.getNomComplet());
        }
        JPanel pArtisteEx = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pArtisteEx.setBackground(Styles.BG_ALT);
        pArtisteEx.add(cbArtiste);
        dynamicZone.add(pArtisteEx, "artiEx");

        // Carte : nouvel artiste
        JTextField tfNomA  = styledSmallField("Nom");
        JTextField tfPrenA = styledSmallField("Pr\u00e9nom");
        JTextField tfNatA  = styledSmallField("Nationalit\u00e9");
        JPanel pArtisteNew = buildInlineFields(
                new String[]{"Nom", "Pr\u00e9nom", "Nationalit\u00e9"},
                new JTextField[]{tfNomA, tfPrenA, tfNatA});
        dynamicZone.add(pArtisteNew, "artiNew");

        // Carte : groupe existant
        List<Groupe> groupes = Catalogue.getTousLesGroupes();
        JComboBox<String> cbGroupe = new JComboBox<>();
        cbGroupe.setFont(Styles.FONT_SMALL);
        cbGroupe.setMaximumSize(new Dimension(380, 32));
        if (groupes.isEmpty()) {
            cbGroupe.addItem("(aucun groupe dans le catalogue)");
        } else {
            for (Groupe g : groupes) cbGroupe.addItem("[" + g.getId() + "] " + g.getNom());
        }
        JPanel pGroupeEx = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pGroupeEx.setBackground(Styles.BG_ALT);
        pGroupeEx.add(cbGroupe);
        dynamicZone.add(pGroupeEx, "groupeEx");

        // Carte : nouveau groupe
        JTextField tfNomG  = styledSmallField("Nom du groupe");
        JTextField tfDateG = styledSmallField("Ann\u00e9e de cr\u00e9ation");
        JTextField tfNatG  = styledSmallField("Nationalit\u00e9");
        JPanel pGroupeNew = buildInlineFields(
                new String[]{"Nom", "Ann\u00e9e", "Nationalit\u00e9"},
                new JTextField[]{tfNomG, tfDateG, tfNatG});
        dynamicZone.add(pGroupeNew, "groupeNew");

        cl.show(dynamicZone, "artiEx");
        panel.add(dynamicZone);

        // Listeners radio → switch de carte
        rbArtisteEx .addActionListener(e -> cl.show(dynamicZone, "artiEx"));
        rbArtisteNew.addActionListener(e -> cl.show(dynamicZone, "artiNew"));
        rbGroupeEx  .addActionListener(e -> cl.show(dynamicZone, "groupeEx"));
        rbGroupeNew .addActionListener(e -> cl.show(dynamicZone, "groupeNew"));

        // Tag client : stocker toutes les références nécessaires pour la collecte
        panel.putClientProperty("rbArtisteEx",  rbArtisteEx);
        panel.putClientProperty("rbArtisteNew", rbArtisteNew);
        panel.putClientProperty("rbGroupeEx",   rbGroupeEx);
        panel.putClientProperty("rbGroupeNew",  rbGroupeNew);
        panel.putClientProperty("cbArtiste",    cbArtiste);
        panel.putClientProperty("cbGroupe",     cbGroupe);
        panel.putClientProperty("artistes",     artistes);
        panel.putClientProperty("groupes",      groupes);
        panel.putClientProperty("tfNomA",       tfNomA);
        panel.putClientProperty("tfPrenA",      tfPrenA);
        panel.putClientProperty("tfNatA",       tfNatA);
        panel.putClientProperty("tfNomG",       tfNomG);
        panel.putClientProperty("tfDateG",      tfDateG);
        panel.putClientProperty("tfNatG",       tfNatG);

        return panel;
    }

    /** Construit un mini-panel de champs texte alignés en colonne. */
    private JPanel buildInlineFields(String[] labels, JTextField[] fields) {
        JPanel p = new JPanel();
        p.setBackground(Styles.BG_ALT);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (int i = 0; i < labels.length; i++) {
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(Styles.FONT_SMALL);
            lbl.setForeground(Styles.TEXT_MUTED);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(lbl);
            p.add(Box.createVerticalStrut(2));
            fields[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(fields[i]);
            if (i < labels.length - 1) p.add(Box.createVerticalStrut(Styles.PADDING_SM));
        }
        return p;
    }

    /** JTextField compact pré-stylé pour les sélecteurs inline. */
    private JTextField styledSmallField(String placeholder) {
        JTextField tf = new JTextField(18);
        tf.setFont(Styles.FONT_SMALL);
        tf.setMaximumSize(new Dimension(320, 30));
        styleTextField(tf);
        return tf;
    }

    /** Applique un style cohérent à un JTextField. */
    private void styleTextField(JTextField tf) {
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 215), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tf.setBackground(Color.WHITE);
        tf.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Styles.TEAL, 1, true),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 205, 215), 1, true),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            }
        });
    }

    /**
     * Collecte les valeurs du formulaire et retourne la liste ordonnée à pousser
     * dans formulaireQueue. Retourne null + affiche erreur si validation échoue.
     *
     * Ordre pour ajouterMorceau  : titre(S) durée(I) genre(S) année(I) idArtiste(I) idGroupe(I)
     * Ordre pour ajouterAlbum    : titre(S) année(I) idArtiste(I) idGroupe(I)
     * Ordre pour ajouterArtiste  : nom(S) prénom(S) nationalité(S)
     * Ordre pour ajouterGroupe   : nom(S) date(I) nationalité(S)
     */
    @SuppressWarnings("unchecked")
    private List<Object> collectFormValues(List<Object[]> fieldRefs, String key, JLabel errLabel) {
        List<Object> values = new ArrayList<>();
        int refIdx = 0;

        for (Object[] ref : fieldRefs) {
            String type = (String) ref[0];
            switch (type) {
                case "text": {
                    JTextField tf = (JTextField) ref[1];
                    String val = tf.getText().trim();
                    if (val.isEmpty()) {
                        errLabel.setText("Tous les champs sont obligatoires.");
                        return null;
                    }
                    values.add(val);
                    break;
                }
                case "number": {
                    JTextField tf = (JTextField) ref[1];
                    String raw = tf.getText().trim();
                    if (raw.isEmpty()) {
                        errLabel.setText("Tous les champs num\u00e9riques sont obligatoires.");
                        return null;
                    }
                    try {
                        values.add(Integer.parseInt(raw));
                    } catch (NumberFormatException ex) {
                        errLabel.setText("Valeur num\u00e9rique invalide : \"" + raw + "\"");
                        return null;
                    }
                    break;
                }
                case "artiste_ou_groupe": {
                    JPanel sel = (JPanel) ref[1];
                    JRadioButton rbArtisteEx  = (JRadioButton) sel.getClientProperty("rbArtisteEx");
                    JRadioButton rbArtisteNew = (JRadioButton) sel.getClientProperty("rbArtisteNew");
                    JRadioButton rbGroupeEx   = (JRadioButton) sel.getClientProperty("rbGroupeEx");
                    JRadioButton rbGroupeNew  = (JRadioButton) sel.getClientProperty("rbGroupeNew");
                    JComboBox<String> cbArtiste = (JComboBox<String>) sel.getClientProperty("cbArtiste");
                    JComboBox<String> cbGroupe  = (JComboBox<String>) sel.getClientProperty("cbGroupe");
                    List<Artiste> artistes = (List<Artiste>) sel.getClientProperty("artistes");
                    List<Groupe>  groupes  = (List<Groupe>)  sel.getClientProperty("groupes");
                    JTextField tfNomA  = (JTextField) sel.getClientProperty("tfNomA");
                    JTextField tfPrenA = (JTextField) sel.getClientProperty("tfPrenA");
                    JTextField tfNatA  = (JTextField) sel.getClientProperty("tfNatA");
                    JTextField tfNomG  = (JTextField) sel.getClientProperty("tfNomG");
                    JTextField tfDateG = (JTextField) sel.getClientProperty("tfDateG");
                    JTextField tfNatG  = (JTextField) sel.getClientProperty("tfNatG");

                    if (rbArtisteEx.isSelected()) {
                        // Artiste existant sélectionné dans le combobox
                        if (artistes.isEmpty()) {
                            errLabel.setText("Aucun artiste disponible. Cr\u00e9ez-en un d\u2019abord.");
                            return null;
                        }
                        int idx = cbArtiste.getSelectedIndex();
                        int idArtiste = (idx >= 0 && idx < artistes.size()) ? artistes.get(idx).getId() : 0;
                        values.add(idArtiste); // idArtiste
                        values.add(0);         // idGroupe = 0
                    } else if (rbArtisteNew.isSelected()) {
                        // Créer un nouvel artiste : pousser d'abord nom/prénom/nat
                        // Le contrôleur appellera demanderIdArtisteMorceau() → on lui donne -1
                        // puis demanderIdGroupeMorceau() → 0
                        // Pour les nouveaux artistes : on utilise le sentinel NEW_ARTISTE
                        // suivi des 3 valeurs, puis -1 pour groupe
                        String nomA  = tfNomA.getText().trim();
                        String prenA = tfPrenA.getText().trim();
                        String natA  = tfNatA.getText().trim();
                        if (nomA.isEmpty() || prenA.isEmpty() || natA.isEmpty()) {
                            errLabel.setText("Remplissez tous les champs du nouvel artiste.");
                            return null;
                        }
                        // On crée l'artiste maintenant (côté EDT) pour récupérer l'ID
                        Artiste newA = Artiste.ajouter(nomA, prenA, natA);
                        values.add(newA.getId()); // idArtiste
                        values.add(0);            // idGroupe = 0
                    } else if (rbGroupeEx.isSelected()) {
                        if (groupes.isEmpty()) {
                            errLabel.setText("Aucun groupe disponible. Cr\u00e9ez-en un d\u2019abord.");
                            return null;
                        }
                        int idx = cbGroupe.getSelectedIndex();
                        int idGroupe = (idx >= 0 && idx < groupes.size()) ? groupes.get(idx).getId() : 0;
                        values.add(0);       // idArtiste = 0
                        values.add(idGroupe);
                    } else { // rbGroupeNew
                        String nomG  = tfNomG.getText().trim();
                        String dateG = tfDateG.getText().trim();
                        String natG  = tfNatG.getText().trim();
                        if (nomG.isEmpty() || dateG.isEmpty() || natG.isEmpty()) {
                            errLabel.setText("Remplissez tous les champs du nouveau groupe.");
                            return null;
                        }
                        try {
                            int anneeG = Integer.parseInt(dateG);
                            Groupe newG = Groupe.ajouter(nomG, anneeG, natG);
                            values.add(0);           // idArtiste = 0
                            values.add(newG.getId());
                        } catch (NumberFormatException ex) {
                            errLabel.setText("Ann\u00e9e de cr\u00e9ation invalide.");
                            return null;
                        }
                    }
                    break;
                }
            }
            refIdx++;
        }
        errLabel.setText(" ");
        return values;
    }

    // ---- Carte : Gérer les comptes ----
    private JPanel buildAdminComptesCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Styles.BG_MAIN);

        JPanel top = new JPanel();
        top.setBackground(Styles.BG_MAIN);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(Styles.PADDING_LG * 2, Styles.PADDING_LG * 2, Styles.PADDING_MD, Styles.PADDING_LG * 2));

        JLabel titre = Styles.titleLabel("G\u00e9rer les comptes abonn\u00e9s");
        titre.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(titre);
        top.add(Box.createVerticalStrut(Styles.PADDING_SM));

        adminComptesStatus = new JLabel(" ");
        adminComptesStatus.setFont(Styles.FONT_SMALL);
        adminComptesStatus.setForeground(new Color(22, 163, 74));
        adminComptesStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(adminComptesStatus);

        JPanel topBarWrapperCpt = new JPanel(new BorderLayout());
        topBarWrapperCpt.setBackground(Styles.BG_MAIN);
        topBarWrapperCpt.add(top, BorderLayout.CENTER);
        topBarWrapperCpt.add(buildFloatingNotesPanel(), BorderLayout.EAST);
        card.add(topBarWrapperCpt, BorderLayout.NORTH);

        adminComptesContent = new JPanel();
        adminComptesContent.setBackground(Styles.BG_MAIN);
        adminComptesContent.setLayout(new BoxLayout(adminComptesContent, BoxLayout.Y_AXIS));
        adminComptesContent.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2, Styles.PADDING_LG, Styles.PADDING_LG * 2));

        ScrollablePanel wrapper = new ScrollablePanel();
        wrapper.setBackground(Styles.BG_MAIN);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(adminComptesContent);
        wrapper.add(Box.createVerticalGlue());
        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Styles.BG_MAIN);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ---- Carte : Statistiques ----
    private JPanel buildAdminStatsCard() {
        adminStatsCard = new JPanel(new BorderLayout());
        adminStatsCard.setBackground(Styles.BG_MAIN);

        JPanel top = new JPanel();
        top.setBackground(Styles.BG_MAIN);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(Styles.PADDING_LG * 2, Styles.PADDING_LG * 2, Styles.PADDING_MD, Styles.PADDING_LG * 2));

        JLabel titre = Styles.titleLabel("Statistiques");
        titre.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(titre);
        top.add(Box.createVerticalStrut(Styles.PADDING_SM));

        JLabel sub = Styles.mutedLabel("Chargement des statistiques\u2026");
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(sub);

        adminStatsCard.add(top, BorderLayout.NORTH);
        return adminStatsCard;
    }

    /**
     * Met à jour le bandeau de feedback en haut de la carte admin.
     * La bordure gauche change de couleur : teal=succès, rouge=erreur.
     */
    private void setAdminCatalogueMsg(String msg, boolean success) {
        runOnEdt(() -> {
            if (adminCatalogueStatus == null) return;
            adminCatalogueStatus.setText(msg);
            Color accentColor = success ? Styles.SUCCESS : Styles.DANGER;
            Color bgColor     = success ? Styles.SUCCESS_SURFACE : new Color(254, 242, 242);
            Color textColor   = success ? Styles.SUCCESS : Styles.DANGER;
            adminCatalogueStatus.setForeground(textColor);
            adminCatalogueStatus.setBackground(bgColor);
            adminCatalogueStatus.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                    BorderFactory.createEmptyBorder(Styles.PADDING_SM, Styles.PADDING_MD,
                            Styles.PADDING_SM, Styles.PADDING_MD)));
        });
    }

    /** Réinitialise le bandeau de feedback admin (état neutre). */
    private void resetAdminCatalogueMsg() {
        runOnEdt(() -> {
            if (adminCatalogueStatus == null) return;
            adminCatalogueStatus.setText(" ");
            adminCatalogueStatus.setForeground(Styles.TEXT_MUTED);
            adminCatalogueStatus.setBackground(Styles.BG_ALT);
        });
    }

    /** Vide la zone de liste contextuelle admin. */
    private void clearAdminCatalogueContent() {
        runOnEdt(() -> {
            if (adminCatalogueContent == null) return;
            adminCatalogueContent.removeAll();
            adminCatalogueContent.revalidate();
            adminCatalogueContent.repaint();
        });
    }

    /** Affiche la liste des abonnés dans la carte comptes. */
    private void refreshAdminComptesUI(List<String[]> abonnes) {
        runOnEdt(() -> {
            adminComptesContent.removeAll();
            if (abonnes.isEmpty()) {
                JLabel vide = Styles.mutedLabel("Aucun abonn\u00e9 pour le moment.");
                vide.setAlignmentX(Component.LEFT_ALIGNMENT);
                adminComptesContent.add(vide);
            } else {
                // En-tête
                JPanel header = new JPanel(new GridLayout(1, 4, Styles.PADDING_MD, 0));
                header.setBackground(Styles.TEAL_SURFACE);
                header.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
                header.setPreferredSize(new Dimension(0, 36));
                header.setMinimumSize(new Dimension(0, 36));
                header.setAlignmentX(Component.LEFT_ALIGNMENT);
                for (String col : new String[]{"ID", "Nom", "E-mail", "Actions"}) {
                    JLabel h = new JLabel(col);
                    h.setFont(Styles.FONT_BODY_BOLD);
                    h.setForeground(Styles.TEAL_DARK);
                    header.add(h);
                }
                adminComptesContent.add(header);

                for (String[] a : abonnes) {
                    String id    = a.length > 0 ? a[0] : "?";
                    String nom   = (a.length > 2 ? a[1] + " " + a[2] : (a.length > 1 ? a[1] : "?"));
                    String email = a.length > 4 ? a[4] : (a.length > 3 ? a[3] : "?");

                    JPanel row = new JPanel(new GridLayout(1, 4, Styles.PADDING_MD, 0));
                    row.setBackground(Styles.BG_ALT);
                    row.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER),
                            BorderFactory.createEmptyBorder(8, 14, 8, 14)));
                    row.setPreferredSize(new Dimension(0, 48));
                    row.setMinimumSize(new Dimension(0, 48));
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);
                    row.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseEntered(java.awt.event.MouseEvent e) { row.setBackground(Styles.TEAL_SURFACE); }
                        @Override public void mouseExited(java.awt.event.MouseEvent e)  { row.setBackground(Styles.BG_ALT); }
                    });

                    row.add(new JLabel(id));
                    row.add(Styles.bodyLabel(nom));
                    row.add(Styles.mutedLabel(email));

                    JButton btnSuppr = Styles.dangerButton("Supprimer");
                    btnSuppr.setFont(Styles.FONT_SMALL);
                    try {
                        int idInt = Integer.parseInt(id.trim());
                        btnSuppr.addActionListener(e -> {
                            try { adminQueue.put("supprimerAbonne:" + idInt); }
                            catch (InterruptedException ignored) {}
                        });
                    } catch (NumberFormatException ignored) { btnSuppr.setEnabled(false); }
                    row.add(btnSuppr);
                    adminComptesContent.add(row);
                }
            }
            adminComptesContent.revalidate();
            adminComptesContent.repaint();
        });
    }

    // ==================== UTILITAIRES INTERNES ====================

    private JPanel transparentPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    /**
     * Cree un panneau decoratif avec des notes musicales flottantes animees.
     * A ajouter en BorderLayout.EAST sur n'importe quelle topBar de page.
     * Les notes descendent lentement en boucle.
     */
    private JPanel buildFloatingNotesPanel() {
        JPanel notes = new JPanel() {
            private float offset = 0f;
            {
                javax.swing.Timer t = new javax.swing.Timer(60, e -> {
                    offset = (offset + 0.4f) % getHeight();
                    repaint();
                });
                t.start();
            }
            @Override protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                int h = getHeight();
                if (h == 0) { g2.dispose(); return; }
                // Trois notes en positions decalees qui descendent en boucle
                String[] noteChars = {"\u266B", "\u266A", "\u2669", "\u266C"};
                int[] xPos   = {20, 55, 10, 45};
                int[] yStart = {0, 40, 20, 60};
                int[] sizes  = {22, 16, 18, 14};
                int[] alphas = {90, 70, 80, 55};
                for (int i = 0; i < noteChars.length; i++) {
                    float yF = (yStart[i] + offset) % h;
                    g2.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF,
                            java.awt.Font.PLAIN, sizes[i]));
                    // Fondu entrant/sortant selon position verticale
                    float rel = yF / h;
                    int fadeAlpha = (int)(alphas[i] * Math.sin(Math.PI * rel));
                    fadeAlpha = Math.max(10, Math.min(alphas[i], fadeAlpha));
                    g2.setColor(new java.awt.Color(
                            Styles.TEAL.getRed(),
                            Styles.TEAL.getGreen(),
                            Styles.TEAL.getBlue(),
                            fadeAlpha));
                    g2.drawString(noteChars[i], xPos[i], (int)yF);
                }
                g2.dispose();
            }
        };
        notes.setOpaque(false);
        notes.setPreferredSize(new java.awt.Dimension(80, 0));
        return notes;
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
     * Exécute un Runnable sur l'EDT et attend sa complétion avant de rendre la main.
     * À utiliser depuis le thread contrôleur quand on DOIT garantir que l'UI
     * est effectivement mise à jour avant de se mettre à attendre sur une queue
     * (élimine les races entre planification d'affichage et arrivée d'événements).
     *
     * Sécurité anti-deadlock : si on est déjà sur l'EDT, on exécute directement
     * (invokeAndWait depuis l'EDT déclencherait une erreur). Ce cas ne devrait
     * jamais arriver en pratique car le thread contrôleur n'est jamais l'EDT.
     */
    private void runOnEdtAndWait(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (java.lang.reflect.InvocationTargetException ite) {
            // Propager uniquement en log : ne pas crasher le thread contrôleur
            Throwable cause = ite.getCause();
            System.err.println("[runOnEdtAndWait] erreur sur l'EDT : " + cause);
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
     * Variante de promptText qui distingue annulation et saisie vide.
     * Retourne {@code null} si l'utilisateur a cliqué Annuler ou fermé le dialog,
     * sinon la chaîne saisie (éventuellement vide).
     *
     * Utilisée par les saisies admin pour pouvoir interrompre proprement un
     * workflow multi-étapes quand l'utilisateur annule.
     */
    private String promptTextCancellable(String titre, String message) {
        final String[] resultat = {null};
        try {
            Runnable r = () -> {
                Object rep = JOptionPane.showInputDialog(
                        fenetre, message, titre,
                        JOptionPane.PLAIN_MESSAGE);
                resultat[0] = (rep == null) ? null : rep.toString();
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
     * Sentinelle retournée par les prompts entiers admin quand l'utilisateur annule
     * ou saisit une valeur non numérique. Le contrôleur doit tester cette valeur
     * et interrompre le workflow en cours si elle est rencontrée.
     */
    public static final int CANCEL_INT = Integer.MIN_VALUE;

    /**
     * Variante de parseIntPrompt qui signale l'annulation par {@link #CANCEL_INT}
     * au lieu de retourner une valeur par défaut silencieuse.
     * - Annulation du dialog → CANCEL_INT
     * - Saisie non numérique → CANCEL_INT
     * - Saisie vide         → CANCEL_INT
     * - Saisie numérique    → la valeur parsée
     */
    private int parseIntPromptCancellable(String titre, String msg) {
        String s = promptTextCancellable(titre, msg);
        if (s == null || s.trim().isEmpty()) return CANCEL_INT;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return CANCEL_INT; }
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

    /** Navigue vers la carte connexion admin avant que le contrôleur demande les saisies. */
    @Override public void afficherConnexionAdmin() {
        if (SwingUtilities.isEventDispatchThread()) {
            showCard("connexionAdmin");
        } else {
            try { SwingUtilities.invokeAndWait(() -> showCard("connexionAdmin")); }
            catch (Exception ignored) {}
        }
    }

    @Override public void afficherConnexionClient() {
        if (SwingUtilities.isEventDispatchThread()) {
            showCard("connexionClient");
        } else {
            try { SwingUtilities.invokeAndWait(() -> showCard("connexionClient")); }
            catch (Exception ignored) {}
        }
    }

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

    // ========== MENUS SIDEBAR-DRIVEN ==========

    @Override public int afficherMenuAdmin() {
        // NE PAS forcer showCard ici : la carte est déjà visible depuis le clic sidebar
        // (ou depuis la première entrée gérée par notifierSessionAdmin via setSessionState)
        try {
            while (true) {
                Object o = adminQueue.take();
                String s = o.toString();
                switch (s) {
                    case "ajouterMorceau":      return 1;
                    case "supprimerMorceau":    return 2;
                    case "ajouterAlbum":        return 3;
                    case "supprimerAlbum":      return 4;
                    case "ajouterMorceauAlbum": return 5;
                    case "ajouterArtiste":      return 6;
                    case "supprimerArtiste":    return 7;
                    case "ajouterGroupe":       return 8;
                    case "supprimerGroupe":     return 9;
                    case "ajouterMembreGroupe": return 10;
                    case "gererComptes":        return 11;
                    case "statistiques":        return 12;
                    case "retour":              return 13;
                    case "catalogue":           return 14;
                    case "gestionCatalogue":
                        runOnEdt(() -> {
                            showCard("gestionCatalogue");
                            if (adminCatalogueStatus != null) {
                                adminCatalogueStatus.setText(" ");
                                adminCatalogueStatus.setForeground(Styles.TEXT_MUTED);
                                adminCatalogueStatus.setBackground(Styles.BG_ALT);
                            }
                            if (adminCatalogueContent != null) {
                                adminCatalogueContent.removeAll();
                                adminCatalogueContent.revalidate();
                                adminCatalogueContent.repaint();
                            }
                        });
                        // reboucler pour attendre la prochaine action
                        break;
                    default:
                        if (s.startsWith("supprimerAbonne:")) return 11;
                        adminQueue.put(o);
                        return 11;
                }
            }
        } catch (InterruptedException e) { return 13; }
    }

    // ==================== CONSOMMATEURS DE formulaireQueue ====================
    //
    // Les demanderXxx() pour les AJOUTS consomment depuis formulaireQueue,
    // alimentée par le bouton Valider du formulaire inline.
    // Les demanderXxx() pour les ASSOCIATIONS (album<->morceau, groupe<->artiste)
    // restent sur promptTextCancellable car leur flux est géré par la zone de liste.

    /** Consomme un String depuis formulaireQueue. Retourne "" si annulé. */
    private String fqTakeString() {
        try {
            Object o = formulaireQueue.take();
            if (o == CANCEL_SENTINEL) return "";
            return o.toString();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return ""; }
    }

    /** Consomme un Integer depuis formulaireQueue. Retourne CANCEL_INT si annulé. */
    private int fqTakeInt() {
        try {
            Object o = formulaireQueue.take();
            if (o == CANCEL_SENTINEL) return CANCEL_INT;
            if (o instanceof Integer) return (Integer) o;
            try { return Integer.parseInt(o.toString()); }
            catch (NumberFormatException e) { return CANCEL_INT; }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return CANCEL_INT; }
    }

    // Morceau
    @Override public String demanderTitreMorceau()     { return fqTakeString(); }
    @Override public int    demanderDureeMorceau()     { return fqTakeInt(); }
    @Override public String demanderGenreMorceau()     { return fqTakeString(); }
    @Override public int    demanderAnneeMorceau()     { return fqTakeInt(); }
    @Override public int    demanderIdArtisteMorceau() { return fqTakeInt(); }
    @Override public int    demanderIdGroupeMorceau()  { return fqTakeInt(); }
    @Override public void   afficherMorceauAjoute(int id) { setAdminCatalogueMsg("Morceau ajout\u00e9 avec succ\u00e8s (ID\u00a0: " + id + ")\u00a0\u2714", true); }

    // Album
    @Override public String demanderTitreAlbum()       { return fqTakeString(); }
    @Override public int    demanderAnneeAlbum()       { return fqTakeInt(); }
    @Override public int    demanderIdArtisteAlbum()   { return fqTakeInt(); }
    @Override public int    demanderIdGroupeAlbum()    { return fqTakeInt(); }
    @Override public void   afficherAlbumAjoute(int id){ setAdminCatalogueMsg("Album ajout\u00e9 avec succ\u00e8s (ID\u00a0: " + id + ")\u00a0\u2714", true); }

    // Artiste (ajout standalone depuis menu)
    @Override public String demanderNomArtiste()        { return fqTakeString(); }
    @Override public String demanderPrenomArtiste()     { return fqTakeString(); }
    @Override public String demanderNationaliteArtiste(){ return fqTakeString(); }
    @Override public void   afficherArtisteAjoute(int id){ setAdminCatalogueMsg("Artiste ajout\u00e9 avec succ\u00e8s (ID\u00a0: " + id + ")\u00a0\u2714", true); }

    // Groupe (ajout standalone depuis menu)
    @Override public String demanderNomGroupe()         { return fqTakeString(); }
    @Override public int    demanderDateCreationGroupe(){ return fqTakeInt(); }
    @Override public String demanderNationaliteGroupe() { return fqTakeString(); }
    @Override public void   afficherGroupeAjoute(int id){ setAdminCatalogueMsg("Groupe ajout\u00e9 avec succ\u00e8s (ID\u00a0: " + id + ")\u00a0\u2714", true); }

    // Associations : restent sur prompt car flux géré par la zone de liste + boutons
    @Override public int    demanderIdAlbumAssociation()   { return parseIntPromptCancellable("Associer morceau \u2192 album", "ID de l\u2019album\u00a0:"); }
    @Override public int    demanderIdMorceauAssociation() { return parseIntPromptCancellable("Associer morceau \u2192 album", "ID du morceau\u00a0:"); }
    @Override public int    demanderNumeroPiste()           { return parseIntPromptCancellable("Num\u00e9ro de piste", "Num\u00e9ro de piste dans l\u2019album\u00a0:"); }
    @Override public void   afficherMorceauAjouteDansAlbum(String tm, String ta){ setAdminCatalogueMsg("\u00ab " + tm + " \u00bb ajout\u00e9 dans \u00ab " + ta + " \u00bb \u2714", true); }

    @Override public int    demanderIdGroupeAssociation()  { return parseIntPromptCancellable("Associer artiste \u2192 groupe", "ID du groupe\u00a0:"); }
    @Override public int    demanderIdArtisteAssociation() { return parseIntPromptCancellable("Associer artiste \u2192 groupe", "ID de l\u2019artiste\u00a0:"); }
    @Override public void   afficherMembreAjouteDansGroupe(String na, String ng){ setAdminCatalogueMsg("\u00ab " + na + " \u00bb ajout\u00e9 dans \u00ab " + ng + " \u00bb \u2714", true); }

    @Override public int    demanderIdSuppression() {
        // Bloque jusqu'au clic sur un bouton "Supprimer" dans la liste,
        // ou sur le bouton Annuler du bandeau.
        try {
            runOnEdt(() -> {
                if (adminCatalogueStatus != null) {
                    adminCatalogueStatus.setText(
                            "\u2193  Cliquez sur \u00ab\u00a0\u2212 Supprimer\u00a0\u00bb dans la liste ci-dessous");
                    adminCatalogueStatus.setForeground(new Color(217, 119, 6));
                }
                if (btnAnnulerSuppression != null) btnAnnulerSuppression.setVisible(true);
            });
            while (true) {
                Integer direct = suppressionDirecteQueue.poll(50, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (direct != null) {
                    suppressionDirecteQueue.clear();
                    runOnEdt(() -> {
                        if (btnAnnulerSuppression != null) btnAnnulerSuppression.setVisible(false);
                    });
                    return direct;
                }
                // Si navigation sortante (clic sidebar) → annuler
                Object ao = adminQueue.peek();
                if (ao != null) {
                    runOnEdt(() -> { if (btnAnnulerSuppression != null) btnAnnulerSuppression.setVisible(false); });
                    return CANCEL_INT;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CANCEL_INT;
        }
    }
    @Override public void   afficherElementSupprime(String type)          { setAdminCatalogueMsg(type + " supprim\u00e9(e) avec succ\u00e8s \u2714", true); }
    @Override public void   afficherElementNonTrouve(String type, int id) { setAdminCatalogueMsg(type + " introuvable (ID : " + id + ")", false); }

    // Comptes abonnés
    @Override public void afficherListeAbonnes(List<String[]> abonnes) {
        refreshAdminComptesUI(abonnes);
        runOnEdt(() -> showCard("gestionComptes"));
    }

    @Override public int afficherMenuGestionComptes() {
        // Attend un clic sur un bouton Supprimer de la liste
        try {
            Object o = adminQueue.take();
            String s = o.toString();
            if (s.startsWith("supprimerAbonne:")) return 1;
            if (s.equals("retour")) return 4;
            return 4;
        } catch (InterruptedException e) { return 4; }
    }

    @Override public int demanderIdAbonne() {
        // L'id est déjà dans la queue sous la forme "supprimerAbonne:ID"
        try {
            Object o = adminQueue.take();
            String s = o.toString();
            if (s.startsWith("supprimerAbonne:")) {
                return Integer.parseInt(s.substring("supprimerAbonne:".length()));
            }
            return -1;
        } catch (InterruptedException | NumberFormatException e) { return -1; }
    }

    @Override public void afficherAbonneSupprime() {
        runOnEdt(() -> {
            adminComptesStatus.setForeground(new Color(22, 163, 74));
            adminComptesStatus.setText("Compte supprim\u00e9 avec succ\u00e8s \u2714");
        });
    }
    @Override public void afficherAbonneNonTrouve() {
        runOnEdt(() -> {
            adminComptesStatus.setForeground(new Color(220, 38, 38));
            adminComptesStatus.setText("Abonn\u00e9 introuvable.");
        });
    }
    @Override public void afficherAbonneSuspendu()                      { runOnEdt(() -> { adminComptesStatus.setForeground(new Color(22,163,74)); adminComptesStatus.setText("Compte suspendu \u2714"); }); }
    @Override public void afficherAbonneReactive()                      { runOnEdt(() -> { adminComptesStatus.setForeground(new Color(22,163,74)); adminComptesStatus.setText("Compte r\u00e9activ\u00e9 \u2714"); }); }
    @Override public void afficherAbonneNonTrouveOuDejaEtat(String e)   { runOnEdt(() -> { adminComptesStatus.setForeground(new Color(220,38,38)); adminComptesStatus.setText("Abonn\u00e9 introuvable ou d\u00e9j\u00e0 " + e); }); }

    // Statistiques
    @Override public void afficherStatistiques(int nbM, int nbA, int nbAr, int nbG, int nbU, int nbE) {
        runOnEdt(() -> {
            showCard("statistiques");
            if (adminStatsCard == null) return;

            adminStatsCard.removeAll();
            adminStatsCard.setLayout(new BorderLayout());

            // En-tête
            JPanel top = new JPanel();
            top.setBackground(Styles.BG_MAIN);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.setBorder(new EmptyBorder(Styles.PADDING_LG * 2, Styles.PADDING_LG * 2, Styles.PADDING_MD, Styles.PADDING_LG * 2));
            JLabel titre = Styles.titleLabel("Statistiques");
            titre.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.add(titre);
            top.add(Box.createVerticalStrut(Styles.PADDING_SM));
            JLabel sub = Styles.mutedLabel("Vue d\u2019ensemble du catalogue et de l\u2019activit\u00e9.");
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.add(sub);
            adminStatsCard.add(top, BorderLayout.NORTH);

            // Contenu scrollable (tuiles + tableau notes)
            JPanel scrollContent = new JPanel();
            scrollContent.setBackground(Styles.BG_MAIN);
            scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));

            // Grille de tuiles
            JPanel grid = new JPanel(new GridLayout(0, 3, Styles.PADDING_LG, Styles.PADDING_LG));
            grid.setBackground(Styles.BG_MAIN);
            grid.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2, Styles.PADDING_LG, Styles.PADDING_LG * 2));
            grid.setAlignmentX(Component.LEFT_ALIGNMENT);

            Object[][] stats = {
                    {"Morceaux",      nbM,  Styles.TEAL},
                    {"Albums",        nbA,  new Color(99, 102, 241)},
                    {"Artistes",      nbAr, new Color(16, 185, 129)},
                    {"Groupes",       nbG,  new Color(245, 158, 11)},
                    {"Utilisateurs",  nbU,  new Color(59, 130, 246)},
                    {"Ecoutes totales", nbE, new Color(239, 68, 68)},
            };
            for (Object[] s : stats) {
                JPanel tile = Styles.cardPanel();
                tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
                tile.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Styles.BORDER, 1, true),
                        BorderFactory.createEmptyBorder(Styles.PADDING_LG, Styles.PADDING_LG, Styles.PADDING_MD, Styles.PADDING_LG)));
                JLabel val = new JLabel("" + s[1]);
                val.setFont(Styles.FONT_TITLE.deriveFont(Font.BOLD, 40f));
                val.setForeground((Color) s[2]);
                val.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel lbl = Styles.mutedLabel(s[0].toString());
                lbl.setFont(Styles.FONT_BODY_BOLD);
                lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                tile.add(val);
                tile.add(Box.createVerticalStrut(4));
                tile.add(lbl);
                grid.add(tile);
            }
            scrollContent.add(grid);

            // ---- Tableau des notes ----
            List<int[]> morceauxNotes = model.Morceau.getMorceauxNotes();

            JPanel notesSection = new JPanel();
            notesSection.setBackground(Styles.BG_MAIN);
            notesSection.setLayout(new BoxLayout(notesSection, BoxLayout.Y_AXIS));
            notesSection.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2, Styles.PADDING_LG * 2, Styles.PADDING_LG * 2));
            notesSection.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titreNotes = Styles.subtitleLabel("Notes des morceaux");
            titreNotes.setAlignmentX(Component.LEFT_ALIGNMENT);
            titreNotes.setBorder(new EmptyBorder(0, 0, Styles.PADDING_SM, 0));
            notesSection.add(titreNotes);

            if (morceauxNotes.isEmpty()) {
                JLabel aucune = Styles.mutedLabel("Aucune note enregistr\u00e9e pour l'instant.");
                aucune.setAlignmentX(Component.LEFT_ALIGNMENT);
                notesSection.add(aucune);
            } else {
                // En-tête tableau
                JPanel header = new JPanel(new GridLayout(1, 4, Styles.PADDING_MD, 0));
                header.setBackground(Styles.TEAL_SURFACE);
                header.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
                header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
                header.setAlignmentX(Component.LEFT_ALIGNMENT);
                for (String col : new String[]{"Morceau", "Artiste / Groupe", "Note moyenne", "Nb votes"}) {
                    JLabel h = new JLabel(col);
                    h.setFont(Styles.FONT_SMALL_BOLD);
                    h.setForeground(Styles.TEAL_DARK);
                    header.add(h);
                }
                notesSection.add(header);
                notesSection.add(Box.createVerticalStrut(2));

                // Lignes
                for (int i = 0; i < morceauxNotes.size(); i++) {
                    int[] entry = morceauxNotes.get(i);
                    int idM = entry[0];
                    double moyenne = entry[1] / 10.0;
                    int nbVotes = entry[2];

                    model.Morceau m = model.Morceau.rechercherParId(idM);
                    String titreM = m != null ? m.getTitre() : "ID " + idM;
                    String artiste = m != null ? m.getNomInterprete() : "—";

                    Color rowBg = (i % 2 == 0) ? Styles.BG_MAIN : Styles.BG_ALT;
                    JPanel row = new JPanel(new GridLayout(1, 4, Styles.PADDING_MD, 0));
                    row.setBackground(rowBg);
                    row.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER),
                            BorderFactory.createEmptyBorder(8, 14, 8, 14)));
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JLabel lblTitre = Styles.bodyLabel(titreM);
                    JLabel lblArtiste = Styles.bodyLabel(artiste);
                    lblArtiste.setForeground(Styles.TEXT_MUTED);

                    // Note moyenne avec barre colorée
                    JPanel noteCell = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                    noteCell.setBackground(rowBg);
                    JLabel lblMoyenne = new JLabel(String.format("%.1f / 5", moyenne));
                    lblMoyenne.setFont(Styles.FONT_SMALL_BOLD);
                    lblMoyenne.setForeground(moyenne >= 4 ? new Color(22, 163, 74)
                            : moyenne >= 3 ? new Color(202, 138, 4)
                              : new Color(220, 38, 38));
                    noteCell.add(lblMoyenne);

                    JLabel lblVotes = Styles.bodyLabel(nbVotes + " vote" + (nbVotes > 1 ? "s" : ""));
                    lblVotes.setForeground(Styles.TEXT_MUTED);

                    row.add(lblTitre);
                    row.add(lblArtiste);
                    row.add(noteCell);
                    row.add(lblVotes);
                    notesSection.add(row);
                }
            }
            scrollContent.add(notesSection);
            scrollContent.add(Box.createVerticalGlue());

            JScrollPane scroll = new JScrollPane(scrollContent);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(Styles.BG_MAIN);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            adminStatsCard.add(scroll, BorderLayout.CENTER);

            adminStatsCard.revalidate();
            adminStatsCard.repaint();
        });
    }

    /** Helper : promptText + parseInt avec valeur par défaut. */
    private int parseIntPrompt(String titre, String msg, int defaut) {
        String s = promptText(titre, msg);
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return defaut; }
    }

    @Override public int afficherMenuClient() {
        try {
            while (true) {
                String nav = clientNavQueue.take();
                switch (nav) {
                    case "catalogue":   return 1;
                    case "playlists":   return 2;
                    case "ecoute":      return 3;
                    case "historique":  return 4;
                    case "deconnexion": return 5;
                    default: break;
                }
            }
        } catch (InterruptedException e) { return 5; }
    }

    @Override public int afficherMenuVisiteur() {
        try {
            while (true) {
                String nav = clientNavQueue.take();
                switch (nav) {
                    case "catalogue":   return 1;
                    case "ecoute":      return 2;
                    case "deconnexion": return 3;
                    default: break;
                }
            }
        } catch (InterruptedException e) { return 3; }
    }

    @Override public int afficherMenuCatalogue() {
        // 1. Détermination du filtre "immédiat" à retourner sans attendre :
        //    priorité au filtre posé par naviguer() (via catalogueFiltreEnAttente),
        //    sinon on regarde si un filtre traîne déjà dans la queue (clic filtre
        //    arrivé pendant une transition depuis une autre carte).
        Integer filtreImmediat = catalogueFiltreEnAttente;
        catalogueFiltreEnAttente = null;

        if (filtreImmediat == null) {
            // Drain complet : ignorer les 7 résiduels, garder le dernier filtre en date
            Integer v;
            while ((v = catalogueIntQueue.poll()) != null) {
                if (v == 7) continue;                // signal de sortie : on ignore
                filtreImmediat = v;                  // dernier filtre gagne
            }
        }
        // Queue propre avant de partir (au cas où on aurait laissé des résidus)
        catalogueIntQueue.clear();

        // 2. Affichage de l'état initial SYNCHRONE (on attend que l'EDT l'ait rendu).
        //    Cela élimine la race entre planification d'affichage (invokeLater) et
        //    arrivée d'un clic filtre : quand on entre dans la boucle d'attente
        //    ci-dessous, l'UI est déjà peinte et les boutons sont opérationnels.
        final Integer fv = filtreImmediat;
        final boolean isClientOrVisitor =
                (sessionState == SessionState.CLIENT || sessionState == SessionState.VISITEUR);

        runOnEdtAndWait(() -> {
            showCard("catalogue");
            // Par défaut (aucun filtre en attente) : afficher la liste des morceaux.
            // Pour ADMIN : ne rien imposer (on laisse l'écran admin gérer son contenu).
            if (fv == null && isClientOrVisitor) {
                afficherListeMorceauxDirect(model.Catalogue.getTousLesMorceaux());
            }
        });

        // 3. Si un filtre était déjà en attente, le retourner immédiatement
        //    (le contrôleur va alors afficher la liste correspondante).
        if (filtreImmediat != null) {
            if (filtreImmediat == 100) {
                runOnEdt(() -> { if (catalogueSearchField != null) catalogueSearchField.setText(""); });
                return 1;
            }
            return filtreImmediat;
        }

        // 4. Boucle d'attente normale : on attend un clic filtre, un clic détail,
        //    ou un signal de sortie (navigation sidebar).
        //    Timeout réduit à 20ms pour une réactivité maximale.
        try {
            while (true) {
                Integer v = catalogueIntQueue.poll(20, java.util.concurrent.TimeUnit.MILLISECONDS);

                if (v != null) {
                    if (v == 7) { return 7; }
                    if (v == 100) {
                        runOnEdt(() -> { if (catalogueSearchField != null) catalogueSearchField.setText(""); });
                        return 1;
                    }
                    return v;
                }

                if (sessionState == SessionState.ADMIN) {
                    Object ao = adminQueue.poll();
                    if (ao != null) {
                        adminQueue.put(ao);
                        return 7;
                    }
                }

                if (sessionState == SessionState.CLIENT || sessionState == SessionState.VISITEUR) {
                    String nav = clientNavQueue.poll();
                    if (nav != null) {
                        clientNavQueue.offer(nav);
                        return 7;
                    }
                }
            }
        } catch (InterruptedException e) { return 7; }
    }

    @Override public String demanderRecherche() {
        // La recherche a déjà été saisie, on la récupère depuis le champ
        return catalogueSearchField != null ? catalogueSearchField.getText().trim() : "";
    }

    /**
     * Navigation : les boutons "Détails" poussent "detail:TYPE:ID" dans catalogueIntQueue.
     * afficherMenuNavigation() décode et retourne le choix au contrôleur.
     * demanderIdElement() retourne l'ID associé.
     */
    private int   dernierTypeNav = 5;  // 1=morceau, 2=album, 3=artiste, 4=groupe, 5=retour
    private int   dernierIdNav   = -1;

    @Override public int afficherMenuNavigation() {
        try {
            while (true) {
                // Poll avec timeout réduit à 20ms pour une réactivité maximale
                Integer v = catalogueIntQueue.poll(20, java.util.concurrent.TimeUnit.MILLISECONDS);

                if (v != null) {
                    if (v >= 1_000_000) {
                        // Encodage : type*1000000 + id → clic "Détails"
                        dernierTypeNav = v / 1_000_000;
                        dernierIdNav   = v % 1_000_000;
                        return dernierTypeNav;
                    }
                    if (v == 7) {
                        // Signal de sortie injecté par la sidebar — vider les éventuels
                        // résidus pour ne pas polluer le prochain afficherMenuCatalogue().
                        catalogueIntQueue.clear();
                        return 5;
                    }
                    // Bouton filtre cliqué pendant la navigation
                    catalogueFiltreEnAttente = v;
                    catalogueIntQueue.clear();
                    return 5;
                }

                // Rien dans catalogueIntQueue : vérifier si la sidebar admin a produit un ordre
                if (sessionState == SessionState.ADMIN) {
                    Object ao = adminQueue.poll();
                    if (ao != null) {
                        // Remettre l'ordre pour que afficherMenuAdmin() le consomme normalement
                        adminQueue.put(ao);
                        catalogueIntQueue.clear();
                        return 5; // sortir de naviguer()
                    }
                }

                // CLIENT / VISITEUR : sortir de naviguer() si la sidebar a envoyé une action
                if (sessionState == SessionState.CLIENT || sessionState == SessionState.VISITEUR) {
                    String nav = clientNavQueue.poll();
                    if (nav != null) {
                        clientNavQueue.offer(nav); // remettre pour afficherMenuClient/Visiteur
                        catalogueIntQueue.clear();
                        return 5;
                    }
                }
            }
        } catch (InterruptedException e) { return 5; }
    }

    @Override public int demanderIdElement() {
        return dernierIdNav;
    }

    // ========== AFFICHAGE CATALOGUE ==========

    @Override public void afficherResultatsRecherche(Catalogue.ResultatRecherche r) {
        runOnEdt(() -> {
            JPanel p = new JPanel();
            p.setBackground(Styles.BG_MAIN);
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.add(buildListPanel("Morceaux (" + r.morceaux.size() + ")", r.morceaux,
                    m -> m.getTitre() + " \u2014 " + m.getNomInterprete() + " (" + m.getAnnee() + ")",
                    Morceau::getId, 1));
            p.add(Box.createVerticalStrut(Styles.PADDING_MD));
            p.add(buildListPanel("Albums (" + r.albums.size() + ")", r.albums,
                    a -> a.getTitre() + " \u2014 " + a.getNomInterprete() + " (" + a.getAnnee() + ")",
                    Album::getId, 2));
            p.add(Box.createVerticalStrut(Styles.PADDING_MD));
            p.add(buildListPanel("Artistes (" + r.artistes.size() + ")", r.artistes,
                    a -> a.getNomComplet(), Artiste::getId, 3));
            p.add(Box.createVerticalStrut(Styles.PADDING_MD));
            p.add(buildListPanel("Groupes (" + r.groupes.size() + ")", r.groupes,
                    g -> g.getNom(), Groupe::getId, 4));
            setCatalogueContent(p);
        });
    }

    /**
     * Détermine si on est actuellement dans un flux admin en train de consulter
     * la carte "gestionCatalogue". Dans ce cas, les listes affichées par le
     * contrôleur (pendant une suppression, un ajout dans album, etc.) doivent
     * apparaître dans la zone admin, pas dans la carte catalogue standard.
     */
    private boolean isAdminCatalogueContext() {
        return sessionState == SessionState.ADMIN
                && "gestionCatalogue".equals(cardCourante);
    }

    /**
     * Remplit la zone de liste admin (adminCatalogueContent) avec le panneau donné.
     * À appeler sur l'EDT.
     */
    private void setAdminCatalogueListe(JPanel content) {
        if (adminCatalogueContent == null) return;
        adminCatalogueContent.removeAll();
        adminCatalogueContent.add(content);
        adminCatalogueContent.revalidate();
        adminCatalogueContent.repaint();
    }

    @Override public void afficherListeMorceaux(List<Morceau> l) {
        JPanel panel = buildListPanel("Morceaux (" + l.size() + ")", l,
                m -> m.getTitre() + " \u2014 " + m.getNomInterprete() + " (" + m.getDureeFormatee() + ")",
                Morceau::getId, 1);
        // En contexte admin on attend le rendu AVANT que le contrôleur appelle
        // demanderIdSuppression() — sinon les boutons cliquables n'existent pas encore.
        if (isAdminCatalogueContext()) {
            suppressionDirecteQueue.clear(); // vider les anciens clics résiduels
            runOnEdtAndWait(() -> setAdminCatalogueListe(panel));
        } else {
            runOnEdt(() -> setCatalogueContent(panel));
        }
    }

    /**
     * Version SYNCHRONE de afficherListeMorceaux : n'utilise PAS runOnEdt.
     * À appeler UNIQUEMENT depuis un Runnable déjà exécuté sur l'EDT.
     */
    private void afficherListeMorceauxDirect(List<Morceau> l) {
        setCatalogueContent(buildListPanel("Morceaux (" + l.size() + ")", l,
                m -> m.getTitre() + " \u2014 " + m.getNomInterprete() + " (" + m.getDureeFormatee() + ")",
                Morceau::getId, 1));
    }

    @Override public void afficherListeAlbums(List<Album> l) {
        JPanel panel = buildListPanel("Albums (" + l.size() + ")", l,
                a -> a.getTitre() + " \u2014 " + a.getNomInterprete() + " (" + a.getAnnee() + ")",
                Album::getId, 2);
        if (isAdminCatalogueContext()) {
            suppressionDirecteQueue.clear();
            runOnEdtAndWait(() -> setAdminCatalogueListe(panel));
        } else {
            runOnEdt(() -> setCatalogueContent(panel));
        }
    }

    @Override public void afficherListeArtistes(List<Artiste> l) {
        JPanel panel = buildListPanel("Artistes (" + l.size() + ")", l,
                a -> a.getNomComplet() + "  \u2022  " + a.getNationalite(), Artiste::getId, 3);
        if (isAdminCatalogueContext()) {
            suppressionDirecteQueue.clear();
            runOnEdtAndWait(() -> setAdminCatalogueListe(panel));
        } else {
            runOnEdt(() -> setCatalogueContent(panel));
        }
    }

    @Override public void afficherListeGroupes(List<Groupe> l) {
        JPanel panel = buildListPanel("Groupes (" + l.size() + ")", l,
                g -> g.getNom() + "  \u2022  " + g.getNationalite(), Groupe::getId, 4);
        if (isAdminCatalogueContext()) {
            suppressionDirecteQueue.clear();
            runOnEdtAndWait(() -> setAdminCatalogueListe(panel));
        } else {
            runOnEdt(() -> setCatalogueContent(panel));
        }
    }

    @Override public void afficherDetailsMorceau(Morceau m) {
        runOnEdt(() -> setCatalogueContent(buildDetailPanel("\uD83C\uDFB5 " + m.getTitre(),
                java.util.Arrays.asList(
                        new String[]{"Interpr\u00e8te", m.getNomInterprete()},
                        new String[]{"Genre",       m.getGenre()},
                        new String[]{"Ann\u00e9e",  "" + m.getAnnee()},
                        new String[]{"Dur\u00e9e",  m.getDureeFormatee()},
                        new String[]{"ID",          "" + m.getId()}))));
    }

    @Override public void afficherDetailsAlbum(Album a) {
        runOnEdt(() -> {
            java.util.List<String[]> lignes = new java.util.ArrayList<>();
            lignes.add(new String[]{"Interpr\u00e8te", a.getNomInterprete()});
            lignes.add(new String[]{"Ann\u00e9e",       "" + a.getAnnee()});
            lignes.add(new String[]{"Dur\u00e9e",       a.getDureeTotaleFormatee()});
            lignes.add(new String[]{"Pistes",           "" + a.getMorceaux().size()});
            lignes.add(new String[]{"ID",               "" + a.getId()});
            setCatalogueContent(buildDetailPanel("\uD83D\uDCBF " + a.getTitre(), lignes));
        });
    }

    @Override public void afficherDetailsArtiste(Artiste a) {
        runOnEdt(() -> setCatalogueContent(buildDetailPanel("\uD83C\uDFA4 " + a.getNomComplet(),
                java.util.Arrays.asList(
                        new String[]{"Nationalit\u00e9", a.getNationalite()},
                        new String[]{"ID",               "" + a.getId()}))));
    }

    @Override public void afficherDetailsGroupe(Groupe g) {
        String membres = g.getMembres().stream()
                .map(Artiste::getNomComplet)
                .collect(java.util.stream.Collectors.joining(", "));
        runOnEdt(() -> setCatalogueContent(buildDetailPanel("\uD83C\uDFB8 " + g.getNom(),
                java.util.Arrays.asList(
                        new String[]{"Nationalit\u00e9", g.getNationalite()},
                        new String[]{"Fond\u00e9 en",    "" + g.getDateCreation()},
                        new String[]{"Membres",          membres.isEmpty() ? "\u2014" : membres},
                        new String[]{"ID",               "" + g.getId()}))));
    }

    // Méthodes de navigation supplémentaires (listes contextuelle depuis un élément)
    @Override public void afficherAlbumsDuMorceau(List<Album> l) {
        if (l.isEmpty()) return;
        runOnEdt(() -> {
            JPanel extra = buildListPanel("Pr\u00e9sent dans ces albums", l,
                    a -> a.getTitre() + " (" + a.getAnnee() + ")", Album::getId);
            catalogueContentArea.add(extra);
            catalogueContentArea.revalidate();
            catalogueContentArea.repaint();
        });
    }

    @Override public void afficherAutresMorceauxInterprete(List<Morceau> l) {
        if (l.isEmpty()) return;
        runOnEdt(() -> {
            JPanel extra = buildListPanel("Autres morceaux du m\u00eame interpr\u00e8te", l,
                    m -> m.getTitre() + " (" + m.getAnnee() + ")", Morceau::getId);
            catalogueContentArea.add(extra);
            catalogueContentArea.revalidate();
            catalogueContentArea.repaint();
        });
    }

    @Override public void afficherAutresAlbumsInterprete(List<Album> l) {
        if (l.isEmpty()) return;
        runOnEdt(() -> {
            JPanel extra = buildListPanel("Autres albums du m\u00eame interpr\u00e8te", l,
                    a -> a.getTitre() + " (" + a.getAnnee() + ")", Album::getId);
            catalogueContentArea.add(extra);
            catalogueContentArea.revalidate();
            catalogueContentArea.repaint();
        });
    }

    @Override public void afficherGroupesDeLArtiste(List<Groupe> l) {
        if (l.isEmpty()) return;
        runOnEdt(() -> {
            JPanel extra = buildListPanel("Membre de", l, Groupe::getNom, Groupe::getId);
            catalogueContentArea.add(extra);
            catalogueContentArea.revalidate();
            catalogueContentArea.repaint();
        });
    }

    @Override public void afficherMorceauxArtiste(List<Morceau> l) {
        if (l.isEmpty()) return;
        runOnEdt(() -> {
            JPanel extra = buildListPanel("Morceaux", l,
                    m -> m.getTitre() + " (" + m.getAnnee() + ")", Morceau::getId);
            catalogueContentArea.add(extra);
            catalogueContentArea.revalidate();
            catalogueContentArea.repaint();
        });
    }

    @Override public void afficherAlbumsArtiste(List<Album> l) {
        if (l.isEmpty()) return;
        runOnEdt(() -> {
            JPanel extra = buildListPanel("Albums", l,
                    a -> a.getTitre() + " (" + a.getAnnee() + ")", Album::getId);
            catalogueContentArea.add(extra);
            catalogueContentArea.revalidate();
            catalogueContentArea.repaint();
        });
    }

    @Override public void afficherMorceauxGroupe(List<Morceau> l) {
        if (l.isEmpty()) return;
        runOnEdt(() -> {
            JPanel extra = buildListPanel("Morceaux du groupe", l,
                    m -> m.getTitre() + " (" + m.getAnnee() + ")", Morceau::getId);
            catalogueContentArea.add(extra);
            catalogueContentArea.revalidate();
            catalogueContentArea.repaint();
        });
    }

    @Override public void afficherAlbumsGroupe(List<Album> l) {
        if (l.isEmpty()) return;
        runOnEdt(() -> {
            JPanel extra = buildListPanel("Albums du groupe", l,
                    a -> a.getTitre() + " (" + a.getAnnee() + ")", Album::getId);
            catalogueContentArea.add(extra);
            catalogueContentArea.revalidate();
            catalogueContentArea.repaint();
        });
    }

    @Override public void afficherGenresDisponibles(List<String> genres) {
        runOnEdt(() -> {
            JPanel p = new JPanel();
            p.setBackground(Styles.BG_MAIN);
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            JLabel t = Styles.subtitleLabel("Choisir un genre");
            t.setAlignmentX(Component.LEFT_ALIGNMENT);
            t.setBorder(new EmptyBorder(0, 0, Styles.PADDING_MD, 0));
            p.add(t);
            for (int i = 0; i < genres.size(); i++) {
                final int num = i + 1;
                final String g = genres.get(i);
                JButton b = Styles.secondaryButton((i + 1) + ". " + g);
                b.setAlignmentX(Component.LEFT_ALIGNMENT);
                b.setMaximumSize(new Dimension(300, 38));
                b.addActionListener(e -> {
                    try { catalogueIntQueue.put(num); } catch (InterruptedException ignored) {}
                });
                p.add(b);
                p.add(Box.createVerticalStrut(6));
            }
            setCatalogueContent(p);
        });
    }

    // ========== PLAYLISTS ==========

    @Override public int afficherMenuPlaylist() {
        playlistsQueue.clear();  // vider résidus d'actions précédentes
        clientNavQueue.clear();  // vider résidus de navigation
        runOnEdt(() -> showCard("playlists"));
        try {
            while (true) {
                // Vérifier d'abord si un clic sidebar a demandé une navigation
                String nav = clientNavQueue.poll();
                if (nav != null && !nav.equals("playlists")) {
                    // L'utilisateur a cliqué sur une autre section : on remet le signal
                    // dans clientNavQueue pour que afficherMenuClient() le consomme,
                    // puis on retourne "retour" (8) pour sortir de menuPlaylist()
                    clientNavQueue.offer(nav);
                    return 8;
                }

                Object o = playlistsQueue.poll(50, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (o == null) continue; // timeout : reboucler pour vérifier clientNavQueue
                String s = o.toString();
                if (s.equals("new"))            return 1;
                if (s.startsWith("voir:"))      return 5;
                if (s.startsWith("ajouter:"))   return 3;
                if (s.startsWith("retirer:"))   return 4;
                if (s.startsWith("ecouter:"))   return 5;
                if (s.startsWith("renommer:"))  return 6;
                if (s.startsWith("supprimer:")) return 7;
                if (s.equals("retour"))         return 8;
            }
        } catch (InterruptedException e) { return 8; }
    }

    @Override public void afficherListePlaylists(List<Playlist> l) { refreshPlaylistsUI(l); }

    @Override public void afficherContenuPlaylist(Playlist p) {
        if (p == null) return;
        playlistSelectionnee = p;
        runOnEdt(() -> showPlaylistDetail(p));
    }

    @Override public String demanderNomPlaylist() {
        runOnEdt(() -> showPlaylistForm("new", ""));
        try { return playlistsNomQueue.take(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return ""; }
    }

    @Override public String demanderNouveauNomPlaylist() {
        runOnEdt(() -> showPlaylistForm("rename", playlistSelectionnee != null ? playlistSelectionnee.getNom() : ""));
        try { return playlistsNomQueue.take(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return ""; }
    }

    @Override public int demanderIdPlaylist() {
        // La playlist est déjà sélectionnée via le clic dans la sidebar gauche.
        // On la retourne directement sans re-bloquer sur la queue (ce qui
        // provoquerait un double-clic nécessaire et une navigation erratique).
        return playlistSelectionnee != null ? playlistSelectionnee.getId() : -1;
    }

    @Override public int demanderIdMorceau() {
        // Le picker a déjà été ouvert et résolu depuis l'EDT par le bouton Ajouter/Retirer.
        contexteAjoutPlaylist = false; // reset du flag après lecture
        return morceauChoisiId;
    }

    /**
     * Affiche une modale de sélection de morceau avec recherche live.
     * @param source  liste de morceaux à afficher
     * @param titreDialog  titre de la fenêtre
     * @param sousTitre    sous-titre affiché dans la modale
     * @return id du morceau sélectionné, ou -1 si annulé
     */
    private int showMorceauPicker(List<model.Morceau> source, String titreDialog, String sousTitre) {
        JDialog dialog = new JDialog(fenetre, titreDialog, true);
        dialog.setSize(540, 500);
        dialog.setLocationRelativeTo(fenetre);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(Styles.BG_MAIN);

        final int[] chosen = {-1};

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Styles.BG_MAIN);
        root.setBorder(new EmptyBorder(Styles.PADDING_LG, Styles.PADDING_LG,
                Styles.PADDING_MD, Styles.PADDING_LG));

        // --- En-tête ---
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel lblTitreDialog = Styles.subtitleLabel("\uD83C\uDFB5  " + sousTitre);
        lblTitreDialog.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(lblTitreDialog);
        header.add(Box.createVerticalStrut(Styles.PADDING_MD));

        // --- Champ de recherche ---
        JPanel searchRow = new JPanel(new BorderLayout(Styles.PADDING_SM, 0));
        searchRow.setOpaque(false);
        JTextField searchField = new JTextField();
        searchField.setFont(Styles.FONT_SMALL);
        searchField.putClientProperty("JTextField.placeholderText", "Rechercher par titre, artiste, genre\u2026");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Styles.TEAL, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        searchField.setBackground(Color.WHITE);
        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(Styles.FONT_BODY);
        searchIcon.setBorder(new EmptyBorder(0, 0, 0, Styles.PADDING_SM));
        searchRow.add(searchIcon, BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
        header.add(searchRow);
        header.add(Box.createVerticalStrut(Styles.PADDING_SM));

        // Compteur de résultats
        JLabel lblCount = Styles.mutedLabel(source.size() + " morceau(x)");
        lblCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblCount.setFont(new Font(Styles.FONT_SMALL.getFamily(), Font.PLAIN, 11));
        header.add(lblCount);
        header.add(Box.createVerticalStrut(Styles.PADDING_SM));

        root.add(header, BorderLayout.NORTH);

        // --- Liste des résultats ---
        JPanel listPanel = new JPanel();
        listPanel.setBackground(Styles.BG_ALT);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Styles.BORDER),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        scroll.getViewport().setBackground(Styles.BG_ALT);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // Remplissage / filtrage de la liste
        Runnable fillList = () -> {
            listPanel.removeAll();
            String query = searchField.getText().trim().toLowerCase();
            java.util.List<model.Morceau> filtered = source.stream()
                    .filter(m -> query.isEmpty()
                            || m.getTitre().toLowerCase().contains(query)
                            || m.getNomInterprete().toLowerCase().contains(query)

                            || m.getGenre().toLowerCase().contains(query))
                    .collect(java.util.stream.Collectors.toList());

            lblCount.setText(filtered.size() + " résultat(s)");

            if (filtered.isEmpty()) {
                JPanel videPanel = new JPanel(new GridBagLayout());
                videPanel.setBackground(Styles.BG_ALT);
                videPanel.setPreferredSize(new Dimension(0, 120));
                JLabel videLabel = Styles.mutedLabel("Aucun résultat pour « " + query + " »");
                videLabel.setFont(Styles.FONT_SMALL);
                videPanel.add(videLabel);
                videPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                listPanel.add(videPanel);
            } else {
                int num = 0;
                for (model.Morceau m : filtered) {
                    boolean alt = (num % 2 == 0);
                    num++;

                    JPanel row = new JPanel(new BorderLayout(Styles.PADDING_MD, 0));
                    row.setBackground(alt ? new Color(248, 253, 253) : Color.WHITE);
                    row.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 238, 238)),
                            BorderFactory.createEmptyBorder(9, 14, 9, 14)));
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);
                    row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    // Bande colorée à gauche au hover
                    row.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 3, 1, 0, alt ? new Color(248, 253, 253) : Color.WHITE),
                            BorderFactory.createEmptyBorder(9, 11, 9, 14)));

                    // Infos texte (titre + artiste/genre)
                    JPanel info = new JPanel();
                    info.setOpaque(false);
                    info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                    JLabel lblT = new JLabel(m.getTitre());
                    lblT.setFont(Styles.FONT_SMALL_BOLD);
                    lblT.setForeground(Styles.TEXT);
                    String artiste = m.getNomInterprete() != null ? m.getNomInterprete() : "\u2014";
                    JLabel lblA = new JLabel(artiste + "  \u2022  " + m.getGenre());
                    lblA.setFont(new Font(Styles.FONT_SMALL.getFamily(), Font.PLAIN, 11));
                    lblA.setForeground(Styles.TEXT_MUTED);
                    info.add(lblT);
                    info.add(lblA);
                    row.add(info, BorderLayout.CENTER);

                    // Durée + bouton "+" à droite
                    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Styles.PADDING_SM, 0));
                    right.setOpaque(false);
                    int sec = m.getDuree();
                    JLabel lblDuree = new JLabel(sec / 60 + ":" + String.format("%02d", sec % 60));
                    lblDuree.setFont(new Font(Styles.FONT_SMALL.getFamily(), Font.PLAIN, 11));
                    lblDuree.setForeground(Styles.TEXT_MUTED);
                    JButton btnSelect = new JButton("\u2713 Choisir");
                    btnSelect.setFont(new Font(Styles.FONT_SMALL.getFamily(), Font.BOLD, 11));
                    btnSelect.setForeground(Color.WHITE);
                    btnSelect.setBackground(Styles.TEAL);
                    btnSelect.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Styles.TEAL_DARK, 1, true),
                            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
                    btnSelect.setFocusPainted(false);
                    btnSelect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    btnSelect.setVisible(false); // visible au hover uniquement
                    right.add(lblDuree);
                    right.add(btnSelect);
                    row.add(right, BorderLayout.EAST);

                    // Sélection : clic sur la ligne ou sur le bouton
                    Color bgBase = alt ? new Color(248, 253, 253) : Color.WHITE;
                    int idFinal = m.getId();
                    Runnable select = () -> { chosen[0] = idFinal; dialog.dispose(); };
                    btnSelect.addActionListener(e -> select.run());
                    row.addMouseListener(new java.awt.event.MouseAdapter() {
                        public void mouseClicked(java.awt.event.MouseEvent e) { select.run(); }
                        public void mouseEntered(java.awt.event.MouseEvent e) {
                            row.setBackground(Styles.TEAL_SURFACE);
                            row.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createMatteBorder(0, 3, 1, 0, Styles.TEAL),
                                    BorderFactory.createEmptyBorder(9, 11, 9, 14)));
                            btnSelect.setVisible(true);
                            row.revalidate(); row.repaint();
                        }
                        public void mouseExited(java.awt.event.MouseEvent e) {
                            row.setBackground(bgBase);
                            row.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createMatteBorder(0, 3, 1, 0, bgBase),
                                    BorderFactory.createEmptyBorder(9, 11, 9, 14)));
                            btnSelect.setVisible(false);
                            row.revalidate(); row.repaint();
                        }
                    });
                    listPanel.add(row);
                }
            }
            listPanel.revalidate();
            listPanel.repaint();
        };

        // Recherche live sur chaque frappe
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { fillList.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { fillList.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { fillList.run(); }
        });

        fillList.run(); // remplissage initial

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(Styles.BG_MAIN);
        center.add(scroll, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        // --- Pied : bouton Annuler ---
        JButton btnCancel = Styles.secondaryButton("Annuler");
        btnCancel.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, Styles.PADDING_SM));
        south.setBackground(Styles.BG_MAIN);
        south.add(btnCancel);
        root.add(south, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        SwingUtilities.invokeLater(() -> searchField.requestFocusInWindow());
        dialog.setVisible(true); // bloquant (modal=true)
        return chosen[0];
    }

    /** Recharge et rafraîchit la liste des playlists depuis le modèle. */
    private void reloadPlaylists() {
        if (utilisateurId <= 0) return;
        List<model.Playlist> updated = model.Playlist.getPlaylistsClient(utilisateurId);
        refreshPlaylistsUI(updated);
    }

    private void setPlaylistStatus(String msg, boolean ok) {
        runOnEdt(() -> {
            playlistsStatusLabel.setForeground(ok ? new Color(22, 163, 74) : new Color(220, 38, 38));
            playlistsStatusLabel.setText(msg);
        });
    }

    @Override public void afficherPlaylistCreee(int id, String nom) {
        setPlaylistStatus("Playlist \u00ab\u00a0" + nom + "\u00a0\u00bb cr\u00e9\u00e9e \u2714", true);
        reloadPlaylists();
    }

    @Override public void afficherPlaylistRenommee(String ancien, String nouveau) {
        setPlaylistStatus("\u00ab\u00a0" + ancien + "\u00a0\u00bb renomm\u00e9e en \u00ab\u00a0" + nouveau + "\u00a0\u00bb \u2714", true);
        reloadPlaylists();
    }

    @Override public void afficherMorceauAjoutePlaylist(String titre, String nomPlaylist) {
        setPlaylistStatus("\u00ab\u00a0" + titre + "\u00a0\u00bb ajout\u00e9 \u00e0 \u00ab\u00a0" + nomPlaylist + "\u00a0\u00bb \u2714", true);
        reloadPlaylists();
    }

    @Override public void afficherMorceauRetire() {
        setPlaylistStatus("Morceau retir\u00e9 de la playlist \u2714", true);
        reloadPlaylists();
    }

    @Override public void afficherPlaylistSupprimee(String nom) {
        playlistSelectionnee = null;
        setPlaylistStatus("Playlist \u00ab\u00a0" + nom + "\u00a0\u00bb supprim\u00e9e.", false);
        reloadPlaylists();
        runOnEdt(() -> playlistsDetailLayout.show(playlistsDetailPanel, "accueil"));
    }

    @Override public void afficherPlaylistIntrouvable() { showError("Playlist introuvable."); }
    @Override public void afficherPlaylistVide()        { showError("Cette playlist est vide."); }
    @Override public void afficherMorceauDejaPresent()  { showError("Ce morceau est d\u00e9j\u00e0 dans la playlist."); }

    @Override public String demanderMail() {
        // Appelé pour admin ET client : on détecte la carte active
        return attendreSaisie(cardCourante, () -> {
            int idx = cardCourante.equals("connexionAdmin") ? 0 : 1;
            showCard(cardCourante);
            activerChampConnexion(idx, true);
        });
    }

    @Override public String demanderMdp() {
        return attendreSaisie(cardCourante, () -> {
            int idx = cardCourante.equals("connexionAdmin") ? 0 : 1;
            activerChampConnexion(idx, false);
        });
    }

    // ========== INSCRIPTION : 4 champs séquentiels ==========

    @Override public String demanderNom() {
        return attendreSaisie("inscription", () -> {
            showCard("inscription");
            activerChampInscription(inscNom);
        });
    }

    @Override public String demanderPrenom() {
        return attendreSaisie("inscription", () -> activerChampInscription(inscPrenom));
    }

    @Override public String demanderEmail() {
        return attendreSaisie("inscription", () -> activerChampInscription(inscEmail));
    }

    @Override public String demanderMotDePasse() {
        return attendreSaisie("inscription", () -> activerChampInscription(inscMdp));
    }

    // ========== FEEDBACK CONNEXION INLINE ==========

    @Override public void afficherConnexionReussie() {
        // Pas de popup : la sidebar se met à jour via setSessionState()
        // appelé par le contrôleur juste après ; on se contente de nettoyer.
        runOnEdt(() -> {
            int idx = cardCourante.equals("connexionAdmin") ? 0 : 1;
            if (idx >= 0 && idx < labelErreurConnexion.length && labelErreurConnexion[idx] != null)
                labelErreurConnexion[idx].setText(" ");
        });
    }

    @Override public void afficherMdpIncorrect() {
        runOnEdt(() -> {
            int idx = cardCourante.equals("connexionAdmin") ? 0 : 1;
            if (idx >= 0 && idx < labelErreurConnexion.length && labelErreurConnexion[idx] != null)
                labelErreurConnexion[idx].setText("Mot de passe incorrect. Veuillez r\u00e9essayer.");
        });
    }

    @Override public void afficherMailIncorrect() {
        runOnEdt(() -> {
            int idx = cardCourante.equals("connexionAdmin") ? 0 : 1;
            if (idx >= 0 && idx < labelErreurConnexion.length && labelErreurConnexion[idx] != null)
                labelErreurConnexion[idx].setText("Adresse e-mail introuvable.");
        });
    }

    @Override public void afficherPasAdmin() {
        runOnEdt(() -> {
            if (labelErreurConnexion[0] != null)
                labelErreurConnexion[0].setText("Ce compte n’est pas un compte administrateur.");
        });
    }

    @Override public void afficherPasClient() {
        runOnEdt(() -> {
            if (labelErreurConnexion[1] != null)
                labelErreurConnexion[1].setText("Ce compte est un compte administrateur. Utilisez la connexion admin.");
        });
    }

    @Override public void afficherInscriptionReussie() {
        runOnEdt(() -> inscErreur.setText("Compte cr\u00e9\u00e9 avec succ\u00e8s ! Vous pouvez maintenant vous connecter."));
        // Afficher le message en vert
        runOnEdt(() -> inscErreur.setForeground(new Color(22, 163, 74)));
    }

    @Override public void afficherMessage(String message) {
        if (message != null && message.startsWith("Merci d'avoir utilise")) {
            runOnEdt(() -> { fenetre.dispose(); System.exit(0); });
            return;
        }
        // Après une annulation : vider les queues pour éviter des résidus
        if ("Action annulee.".equals(message)) {
            formulaireQueue.clear();
            setAdminCatalogueMsg("Action annulée.", false);
            runOnEdt(() -> {
                if (adminFormulaireLayout != null)
                    adminFormulaireLayout.show(adminFormulairePanel, "accueil");
            });
            return;
        }
        if (message != null && !message.isEmpty()) { showInfo(message); }
    }
    @Override public void afficherChoixInvalide() { showError("Choix invalide."); }

    // ==================== NOTIFICATIONS DE SESSION ====================

    @Override public void notifierSessionAdmin(String nom) {
        setSessionState(SessionState.ADMIN, nom);
        // Afficher la page de gestion catalogue par défaut à la connexion
        runOnEdt(() -> showCard("gestionCatalogue"));
    }
    @Override public void notifierSessionClient(String nom) {
        setSessionState(SessionState.CLIENT, nom);
    }

    /** Variante avec ID pour que la vue puisse recharger l'historique autonomement. */
    public void notifierSessionClientAvecId(String nom, int id) {
        this.utilisateurId = id;
        setSessionState(SessionState.CLIENT, nom);
    }
    @Override public void notifierSessionVisiteur() {
        setSessionState(SessionState.VISITEUR, null);
    }

    // ==================== ÉCOUTE ====================

    @Override public String demanderRechercheMusique() {
        // Court-circuit : si on vient du picker playlist, le morceau est déjà choisi —
        // on ne navigue PAS vers Écouter.
        if (contexteAjoutPlaylist) return "__bypass__";

        // 1. Queue propre avant de commencer (élimine les "nav:xxx" résiduels
        //    posés par onSidebarClick pendant la transition vers cette carte).
        ecouteQueue.clear();

        // 2. Affichage SYNCHRONE : showCard + liste des morceaux + focus champ.
        //    Utilise afficherResultatsEcouteDirect (pas de runOnEdt imbriqué).
        //    Garantit qu'au moment où on appelle ecouteQueue.take() ci-dessous,
        //    les boutons ▶ existent et sont cliquables.
        runOnEdtAndWait(() -> {
            showCard("ecoute");
            afficherResultatsEcouteDirect(model.Catalogue.getTousLesMorceaux());
            if (ecouteSearchField != null) {
                ecouteSearchField.setText("");
                ecouteSearchField.requestFocusInWindow();
            }
        });

        // 3. Attente bloquante : clic ▶ (Integer), recherche ("search:..."),
        //    ou signal de navigation ("nav:...") posé par la sidebar.
        try {
            while (true) {
                Object o = ecouteQueue.take();
                if (o instanceof Integer) {
                    // Clic ▶ direct : on remet l'id pour demanderIdMorceauEcoute()
                    // et on retourne "" (= rechercherGlobal("") → tous les morceaux).
                    ecouteQueue.offer(o);
                    return "";
                }
                String s = o.toString();
                if (s.startsWith("search:")) { return s.substring(7); }
                if (s.startsWith("nav:"))    { return "stop"; }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    @Override public void afficherResultatsEcoute(List<Morceau> resultats) {
        if (contexteAjoutPlaylist) return; // le picker gère déjà l'affichage
        runOnEdt(() -> afficherResultatsEcouteDirect(resultats));
    }

    /**
     * Version SYNCHRONE de afficherResultatsEcoute : n'utilise PAS runOnEdt.
     * À appeler UNIQUEMENT depuis un Runnable déjà exécuté sur l'EDT.
     * Utilisée par demanderRechercheMusique() pour éviter le double invokeLater
     * imbriqué (runOnEdt dans un runOnEdt) qui cassait l'affichage initial.
     */
    private void afficherResultatsEcouteDirect(List<Morceau> resultats) {
        ecouteContentArea.removeAll();
        if (resultats.isEmpty()) {
            JLabel vide = Styles.mutedLabel("Aucun morceau trouv\u00e9.");
            vide.setAlignmentX(Component.LEFT_ALIGNMENT);
            ecouteContentArea.add(vide);
        } else {
            JLabel t = Styles.subtitleLabel("R\u00e9sultats (" + resultats.size() + ")");
            t.setAlignmentX(Component.LEFT_ALIGNMENT);
            t.setBorder(new EmptyBorder(Styles.PADDING_MD, 0, Styles.PADDING_SM, 0));
            ecouteContentArea.add(t);
            for (Morceau m : resultats) {
                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(Styles.BG_ALT);
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER),
                        BorderFactory.createEmptyBorder(10, 14, 10, 14)));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { row.setBackground(Styles.TEAL_SURFACE); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e)  { row.setBackground(Styles.BG_ALT); }
                });

                JLabel lbl = Styles.bodyLabel(m.getTitre() + " \u2014 " + m.getNomInterprete()
                        + "  \u2022  " + m.getDureeFormatee());
                row.add(lbl, BorderLayout.CENTER);

                JButton btn = Styles.primaryButton("Ecouter");
                btn.setFont(Styles.FONT_SMALL);
                btn.addActionListener(e -> {
                    try { ecouteQueue.put(m.getId()); } catch (InterruptedException ignored) {}
                });
                row.add(btn, BorderLayout.EAST);
                ecouteContentArea.add(row);
            }
        }
        ecouteContentArea.revalidate();
        ecouteContentArea.repaint();
    }

    @Override public int demanderIdMorceauEcoute() {
        try {
            while (true) {
                Object o = ecouteQueue.take();
                if (o instanceof Integer) return (Integer) o;
                String s = o.toString();
                if (s.startsWith("nav:")) return -1; // navigation sidebar → contrôleur ignorera
                // search: ou autre → reboucle (attend un clic ▶)
            }
        } catch (InterruptedException e) { return -1; }
    }

    /** Simule la lecture : barre de progression animée sur 3 secondes. */
    @Override public void afficherEcoute(Morceau m) {
        runOnEdt(() -> {
            ecouteTitreCourant.setText(m.getTitre());
            ecouteInterpreteLabel.setText(m.getNomInterprete() + "  \u2022  " + m.getDureeFormatee());
            ecouteProgressBar.setValue(0);
            ecoutePlayerPanel.setVisible(true);
            // Reset pause standalone
            ecouteEnPause = false;
            ecouteBtnPause.setText("||");
            if (playlistsLecteurPanel != null) {
                playlistsLecteurTitre.setText(m.getTitre());
                playlistsLecteurArtiste.setText(m.getNomInterprete() + "  \u2022  " + m.getDureeFormatee());
                playlistsLecteurBar.setValue(0);
                lecteurEnPause = false;
                playlistsLecteurPause.setText("||");
            }
        });

        final int STEPS = 60;
        final int DELAY_MS = 50;
        for (int i = 1; i <= STEPS; i++) {
            // Pause (standalone ou playlist)
            while ((ecouteEnPause || lecteurEnPause) &&
                    ecouteControleQueue.isEmpty() && lecteurPlaylistQueue.isEmpty()) {
                try { Thread.sleep(100); } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); break;
                }
            }
            // Interrompre si skip/stop pressé
            if (!ecouteControleQueue.isEmpty() || !lecteurPlaylistQueue.isEmpty()) break;

            final int val = (i * 100) / STEPS;
            SwingUtilities.invokeLater(() -> {
                ecouteProgressBar.setValue(val);
                if (playlistsLecteurBar != null) playlistsLecteurBar.setValue(val);
            });
            try { Thread.sleep(DELAY_MS); } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); break;
            }
        }
        SwingUtilities.invokeLater(() -> {
            ecouteProgressBar.setValue(100);
            if (playlistsLecteurBar != null) playlistsLecteurBar.setValue(100);
        });
    }

    @Override public int afficherMenuApresEcoute(int restantes) {
        if (restantes == 0) return 2; // limite atteinte
        runOnEdt(() -> {
            if (restantes > 0) {
                ecouteLimiteLabel.setText(restantes + " \u00e9coute(s) restante(s) cette session.");
            } else {
                ecouteLimiteLabel.setText("");
            }
        });
        // Consommer un contrôle pressé pendant l'animation
        String ctrl = ecouteControleQueue.poll();
        if (ctrl != null) {
            switch (ctrl) {
                case "stop": return 2; // arrêter
                case "next": return 1; // morceau suivant (continuer)
                case "prev": return 1; // on retourne 1, ecouter() rebouclera
            }
        }
        // Navigation sidebar
        String nav = clientNavQueue.peek();
        if (nav != null && !nav.equals("ecoute")) return 2;
        return 1; // continuer
    }

    @Override public int afficherMenuApresEchecRecherche() {
        return 1; // réessayer
    }

    @Override public void afficherLimiteEcoutesAtteinte() {
        runOnEdt(() -> {
            ecouteLimiteLabel.setText("Limite d'\u00e9coutes atteinte pour cette session.");
            ecouteLimiteLabel.setForeground(new Color(220, 38, 38));
        });
    }

    @Override public void afficherPochette(int index, int total, Morceau m) {
        // Mise à jour du lecteur déjà gérée dans afficherEcoute
    }

    @Override public int afficherControlesLecteur(boolean hasPrev, boolean hasNext) {
        // Mettre à jour l'état des boutons
        runOnEdt(() -> {
            if (playlistsLecteurPrev != null) playlistsLecteurPrev.setEnabled(hasPrev);
            if (playlistsLecteurNext != null) playlistsLecteurNext.setEnabled(hasNext);
        });
        // Consommer l'action déjà posée pendant l'animation (interruption anticipée)
        Integer action = lecteurPlaylistQueue.poll();
        if (action != null) return action;
        // Sinon passer automatiquement au suivant
        return hasNext ? 2 : 3;
    }

    @Override public void afficherLecturePlaylist(String nom) {
        runOnEdtAndWait(() -> {
            playlistsStatusLabel.setForeground(Styles.TEAL);
            playlistsStatusLabel.setText("▶  Lecture de «\u00a0" + nom + "\u00a0» en cours...");
            lecteurPlaylistQueue.clear();
            lecteurEnPause = false;
            playlistsLecteurPause.setText("||");
            playlistsLecteurPanel.setVisible(true);
            playlistsLecteurPanel.revalidate();
            playlistsLecteurPanel.repaint();
            showCard("playlists");
        });
    }

    @Override public void afficherFinPlaylist(String nom) {
        runOnEdt(() -> {
            playlistsStatusLabel.setForeground(Styles.TEXT_MUTED);
            playlistsStatusLabel.setText("Lecture de \u00ab\u00a0" + nom + "\u00a0\u00bb termin\u00e9e.");
            lecteurEnPause = false;
            if (playlistsLecteurPanel != null) {
                playlistsLecteurBar.setValue(0);
                playlistsLecteurPanel.setVisible(false);
            }
        });
    }

    // ==================== HISTORIQUE ====================

    @Override public void afficherHistorique(List<model.Historique> historique) {
        runOnEdt(() -> {
            showCard("historique");
            historiqueContentArea.removeAll();

            if (historique.isEmpty()) {
                JLabel vide = Styles.mutedLabel("Aucun morceau \u00e9cout\u00e9 pour l'instant.");
                vide.setAlignmentX(Component.LEFT_ALIGNMENT);
                vide.setBorder(new EmptyBorder(Styles.PADDING_MD, 0, 0, 0));
                historiqueContentArea.add(vide);
            } else {
                // En-têtes : 5 colonnes (Titre, Interprète, Année, Date, Ma note)
                JPanel header = new JPanel(new GridLayout(1, 5, Styles.PADDING_MD, 0));
                header.setBackground(Styles.TEAL_SURFACE);
                header.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
                header.setPreferredSize(new Dimension(0, 36));
                header.setMinimumSize(new Dimension(0, 36));
                header.setAlignmentX(Component.LEFT_ALIGNMENT);
                for (String col : new String[]{"Titre", "Interpr\u00e8te", "Ann\u00e9e", "Date / heure", "Ma note"}) {
                    JLabel h = new JLabel(col);
                    h.setFont(Styles.FONT_BODY_BOLD);
                    h.setForeground(Styles.TEAL_DARK);
                    header.add(h);
                }
                historiqueContentArea.add(header);

                // Lignes
                for (int i = 0; i < historique.size(); i++) {
                    model.Historique h = historique.get(i);
                    final Color rowBg = (i % 2 == 0) ? Styles.BG_MAIN : Styles.BG_ALT;

                    JPanel row = new JPanel(new GridLayout(1, 5, Styles.PADDING_MD, 0));
                    row.setBackground(rowBg);
                    row.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER),
                            BorderFactory.createEmptyBorder(8, 14, 8, 14)));
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);

                    // Colonnes texte
                    for (String val : new String[]{h.getTitre(), h.getInterprete(),
                            "" + h.getAnnee(), h.getDateHeure()}) {
                        JLabel cell = Styles.bodyLabel(val);
                        row.add(cell);
                    }

                    // Colonne étoiles cliquables
                    int idMorceau = h.getIdMorceau();
                    int noteExistante = utilisateurId > 0
                            ? model.Morceau.getNoteClient(idMorceau, utilisateurId) : 0;
                    JPanel etoilesCell = buildEtoilesInline(idMorceau, noteExistante, row, rowBg);
                    row.add(etoilesCell);

                    // Hover sur la ligne entière
                    row.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                            row.setBackground(Styles.TEAL_SURFACE);
                            etoilesCell.setBackground(Styles.TEAL_SURFACE);
                        }
                        @Override public void mouseExited(java.awt.event.MouseEvent e) {
                            row.setBackground(rowBg);
                            etoilesCell.setBackground(rowBg);
                        }
                    });

                    historiqueContentArea.add(row);
                }
            }
            historiqueContentArea.revalidate();
            historiqueContentArea.repaint();
        });
    }

    /**
     * Construit une cellule de notation simple : un JTextField (1 char) + bouton "OK".
     * Affiche la note actuelle si elle existe, sinon placeholder "1-5".
     */
    private JPanel buildEtoilesInline(int idMorceau, int noteInitiale, JPanel rowParent, Color rowBg) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        panel.setBackground(rowBg);
        panel.setOpaque(true);

        final int[] noteCourante = {noteInitiale};

        // Affichage note actuelle
        JLabel lblNote = new JLabel(noteInitiale > 0 ? noteInitiale + "/5" : "—");
        lblNote.setFont(Styles.FONT_SMALL_BOLD);
        lblNote.setForeground(noteInitiale > 0 ? new Color(202, 138, 4) : Styles.TEXT_MUTED);
        lblNote.setPreferredSize(new Dimension(32, 22));

        // Champ de saisie (1 caractère)
        JTextField field = new JTextField(2);
        field.setFont(Styles.FONT_SMALL);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setToolTipText("Entrez une note de 1 à 5");
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Styles.TEAL, 1, true),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));

        // Bouton OK
        JButton btnOk = new JButton("OK");
        btnOk.setFont(new Font(Styles.FONT_SMALL.getFamily(), Font.BOLD, 11));
        btnOk.setForeground(Color.WHITE);
        btnOk.setBackground(Styles.TEAL);
        btnOk.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Styles.TEAL_DARK, 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        btnOk.setFocusPainted(false);
        btnOk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Action commune : valider la saisie
        Runnable valider = () -> {
            String txt = field.getText().trim();
            try {
                int note = Integer.parseInt(txt);
                if (note < 1 || note > 5) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(220, 38, 38), 1, true),
                            BorderFactory.createEmptyBorder(2, 4, 2, 4)));
                    field.setToolTipText("Note invalide : entrez un chiffre entre 1 et 5");
                    return;
                }
                noteCourante[0] = note;
                if (utilisateurId > 0)
                    model.Morceau.noterMorceau(idMorceau, utilisateurId, note);
                lblNote.setText(note + "/5");
                lblNote.setForeground(new Color(202, 138, 4));
                field.setText("");
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Styles.TEAL, 1, true),
                        BorderFactory.createEmptyBorder(2, 4, 2, 4)));
                // Flash jaune pâle pour confirmer
                Color flash = new Color(254, 249, 195);
                rowParent.setBackground(flash);
                panel.setBackground(flash);
                new javax.swing.Timer(600, ev -> {
                    rowParent.setBackground(rowBg);
                    panel.setBackground(rowBg);
                    ((javax.swing.Timer) ev.getSource()).stop();
                }).start();
            } catch (NumberFormatException ex) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 38, 38), 1, true),
                        BorderFactory.createEmptyBorder(2, 4, 2, 4)));
            }
        };

        btnOk.addActionListener(e -> valider.run());
        field.addActionListener(e -> valider.run()); // Entrée = valider

        panel.add(lblNote);
        panel.add(field);
        panel.add(btnOk);
        return panel;
    }

    // ==================== OVERRIDES DIVERS ====================

    @Override public void afficherErreurId() {
        showError("ID invalide ou introuvable.");
    }

    @Override public void afficherRetourMenuPrincipal() {
        // En graphique, le retour au menu principal est géré par la sidebar (Se déconnecter)
        // On reset simplement la session
        runOnEdt(this::resetToAccueilDeconnecte);
    }

    // Notation (fonctionnalité supplémentaire)
    @Override public void afficherNoteMorceau(double moyenne, int nbVotes) {
        if (nbVotes == 0) return;
        int pleines = (int) Math.round(moyenne);
        StringBuilder etoiles = new StringBuilder();
        for (int i = 1; i <= 5; i++) etoiles.append(i <= pleines ? "\u2605" : "\u2606");
        // Afficher dans la zone de contenu catalogue actuelle
        String txt = etoiles + "  " + String.format("%.1f", moyenne) + "/5  (" + nbVotes + " vote" + (nbVotes > 1 ? "s" : "") + ")";
        runOnEdt(() -> {
            JLabel note = new JLabel(txt);
            note.setFont(Styles.FONT_BODY_BOLD);
            note.setForeground(new Color(202, 138, 4));
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            note.setBorder(new EmptyBorder(Styles.PADDING_SM, 0, 0, 0));
            catalogueContentArea.add(note);
            catalogueContentArea.revalidate();
            catalogueContentArea.repaint();
        });
    }

    @Override public int proposerNotation(int noteActuelle) {
        // Ouvre directement la modale étoiles — retourne 1 si une note a été choisie, 2 sinon.
        final int[] result = {0};
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = showNotationDialog(noteActuelle));
        } catch (Exception e) { Thread.currentThread().interrupt(); }
        return result[0] == 0 ? 2 : 1;
    }

    @Override public int demanderNote() {
        // La note a déjà été choisie dans showNotationDialog — on la retourne.
        return noteCourante;
    }

    /** Note choisie dans la modale étoiles, lue par demanderNote(). */
    private volatile int noteCourante = 0;

    /**
     * Affiche une modale de notation avec 5 étoiles cliquables.
     * Retourne la note choisie (1-5), ou 0 si annulé/fermé.
     */
    private int showNotationDialog(int noteActuelle) {
        JDialog dialog = new JDialog(fenetre, "Noter ce morceau", true);
        dialog.setSize(360, 220);
        dialog.setLocationRelativeTo(fenetre);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        final int[] chosen = {0};

        JPanel root = new JPanel(new BorderLayout(0, Styles.PADDING_MD));
        root.setBackground(Styles.BG_MAIN);
        root.setBorder(new EmptyBorder(Styles.PADDING_LG, Styles.PADDING_LG,
                Styles.PADDING_MD, Styles.PADDING_LG));

        JLabel lblTitre = Styles.subtitleLabel("Votre note");
        lblTitre.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(lblTitre, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        if (noteActuelle > 0) {
            JLabel lblActuelle = Styles.mutedLabel("Note actuelle : " + noteActuelle + "/5 — cliquez pour modifier");
            lblActuelle.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(lblActuelle);
            center.add(Box.createVerticalStrut(Styles.PADDING_SM));
        }

        // 5 étoiles cliquables avec hover
        JPanel etoilesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        etoilesPanel.setOpaque(false);
        JButton[] etoiles = new JButton[5];
        final int[] survol = {noteActuelle > 0 ? noteActuelle : 0};

        for (int i = 1; i <= 5; i++) {
            final int val = i;
            JButton btn = new JButton(i <= survol[0] ? "★" : "☆");
            btn.setFont(new Font(Styles.FONT_BODY.getFamily(), Font.PLAIN, 30));
            btn.setForeground(new Color(202, 138, 4));
            btn.setBackground(Styles.BG_MAIN);
            btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            etoiles[i - 1] = btn;

            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    for (int j = 0; j < 5; j++)
                        etoiles[j].setText(j < val ? "★" : "☆");
                }
                public void mouseExited(java.awt.event.MouseEvent e) {
                    for (int j = 0; j < 5; j++)
                        etoiles[j].setText(j < survol[0] ? "★" : "☆");
                }
            });
            btn.addActionListener(e -> {
                survol[0] = val;
                chosen[0] = val;
                for (int j = 0; j < 5; j++)
                    etoiles[j].setText(j < val ? "★" : "☆");
                dialog.dispose();
            });
            etoilesPanel.add(btn);
        }
        center.add(etoilesPanel);
        root.add(center, BorderLayout.CENTER);

        JButton btnPasser = Styles.secondaryButton("Passer");
        btnPasser.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setBackground(Styles.BG_MAIN);
        south.add(btnPasser);
        root.add(south, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);

        noteCourante = chosen[0];
        return chosen[0];
    }

    @Override public void afficherNoteEnregistree(int note) {
        StringBuilder etoiles = new StringBuilder();
        for (int i = 1; i <= 5; i++) etoiles.append(i <= note ? "★" : "☆");
        runOnEdt(() -> {
            playlistsStatusLabel.setForeground(new Color(202, 138, 4));
            playlistsStatusLabel.setText(etoiles + "  Note " + note + "/5 enregistree");
        });
    }


    /**
     * JPanel qui implémente Scrollable pour forcer le JScrollPane à l'étirer
     * sur toute la largeur du viewport — corrige le bug de troncature lors du défilement.
     */
    private static class ScrollablePanel extends JPanel implements javax.swing.Scrollable {
        ScrollablePanel() { super(); }
        ScrollablePanel(java.awt.LayoutManager lm) { super(lm); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth()  { return true;  }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

}