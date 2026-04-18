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
            if (actionKey.equals("catalogue")) {
                showCard("catalogue");
                try { adminQueue.put("catalogue"); } catch (InterruptedException ignored) {}
                return;
            }
            if (actionKey.equals("gestionCatalogue")) {
                showCard("gestionCatalogue");
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

        // Bannière hero avec dégradé teal simulé
        JPanel hero = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, Styles.TEAL, getWidth(), getHeight(), Styles.TEAL_DARK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Note musicales déco semi-transparentes
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 80));
                g2.setColor(new Color(255, 255, 255, 25));
                g2.drawString("\u266B", getWidth() - 160, 100);
                g2.drawString("\u266A", getWidth() - 280, 60);
                g2.drawString("\u2669", getWidth() - 60, 140);
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

    // index 0 = admin, index 1 = client
    private int indexCarteActive = -1;

    private JPanel buildConnexionCard(String titreStr, String sousTitreStr, String cardKey) {
        int idx = cardKey.equals("connexionAdmin") ? 0 : 1;

        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Styles.BG_MAIN);

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
            // On determine quel champ est actif pour pousser la bonne valeur
            String val;
            if (fieldEmail.isEnabled() && !fieldMdp.isEnabled()) {
                val = fieldEmail.getText().trim();
            } else {
                val = new String(fieldMdp.getPassword()).trim();
            }
            lblErreur.setText(" ");
            try { saisieCourante.put(val); } catch (InterruptedException ignored) {}
        };

        fieldEmail.addActionListener(e -> submitAction.run());
        fieldMdp.addActionListener(e -> submitAction.run());
        btnValider.addActionListener(e -> submitAction.run());

        outer.add(form);
        return outer;
    }

    // ==================== CARTE INSCRIPTION ====================

    private final JTextField     inscNom    = buildTextField(320);
    private final JTextField     inscPrenom = buildTextField(320);
    private final JTextField     inscEmail  = buildTextField(320);
    private final JPasswordField inscMdp    = buildPasswordField(320);
    private final JLabel         inscErreur = new JLabel(" ");

    private JPanel buildInscriptionCard() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Styles.BG_MAIN);

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
            String val = determinerValeurInscription();
            inscErreur.setText(" ");
            try { saisieCourante.put(val); } catch (InterruptedException ignored) {}
        };

        inscNom.addActionListener(e -> submitInscription.run());
        inscPrenom.addActionListener(e -> submitInscription.run());
        inscEmail.addActionListener(e -> submitInscription.run());
        inscMdp.addActionListener(e -> submitInscription.run());
        btnValider.addActionListener(e -> submitInscription.run());

        outer.add(form);
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
     */
    private void activerChampConnexion(int idx, boolean emailActif) {
        JTextField email = champEmailConnexion[idx];
        JPasswordField mdp = champMdpConnexion[idx];
        if (emailActif) {
            email.setEnabled(true);
            email.setText("");
            email.setBackground(Styles.BG_ALT);
            mdp.setEnabled(false);
            mdp.setText("");
            mdp.setBackground(new Color(240, 240, 240));
            SwingUtilities.invokeLater(email::requestFocusInWindow);
        } else {
            email.setEnabled(false);
            email.setBackground(new Color(240, 240, 240));
            mdp.setEnabled(true);
            mdp.setText("");
            mdp.setBackground(Styles.BG_ALT);
            SwingUtilities.invokeLater(mdp::requestFocusInWindow);
        }
        labelErreurConnexion[idx].setText(" ");
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
     */
    private String attendreSaisie(String cardKey, Runnable setupEdt) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                setupEdt.run();
            } else {
                SwingUtilities.invokeAndWait(setupEdt);
            }
            return saisieCourante.take();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return "";
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

        JButton btnSearch = Styles.primaryButton("\uD83D\uDD0D Rechercher");
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
        for (int i = 0; i < filtres.length; i++) {
            final int filtreId = filtreIds[i];
            JButton fb = Styles.secondaryButton(filtres[i]);
            fb.setFont(Styles.FONT_SMALL);
            fb.addActionListener(e -> {
                try { catalogueIntQueue.put(filtreId); } catch (InterruptedException ignored) {}
            });
            filterBar.add(fb);
        }
        topBar.add(filterBar);

        card.add(topBar, BorderLayout.NORTH);

        // ---- Zone de contenu scrollable ----
        catalogueContentArea = new JPanel();
        catalogueContentArea.setBackground(Styles.BG_MAIN);
        catalogueContentArea.setLayout(new BoxLayout(catalogueContentArea, BoxLayout.Y_AXIS));
        catalogueContentArea.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2,
                Styles.PADDING_LG, Styles.PADDING_LG * 2));

        // Wrapper BorderLayout.NORTH pour que BoxLayout calcule la preferredSize correctement
        JPanel catalogueWrapper = new JPanel(new BorderLayout());
        catalogueWrapper.setBackground(Styles.BG_MAIN);
        catalogueWrapper.add(catalogueContentArea, BorderLayout.NORTH);

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
     */
    private <T> JPanel buildListPanel(String sectionTitle, List<T> items,
                                      java.util.function.Function<T, String> toLabel,
                                      java.util.function.Function<T, Integer> toId,
                                      int typeCode) {
        JPanel p = new JPanel();
        p.setBackground(Styles.BG_MAIN);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        if (!items.isEmpty()) {
            JLabel sec = Styles.subtitleLabel(sectionTitle);
            sec.setAlignmentX(Component.LEFT_ALIGNMENT);
            sec.setBorder(new EmptyBorder(Styles.PADDING_MD, 0, Styles.PADDING_SM, 0));
            p.add(sec);
        }

        for (T item : items) {
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(Styles.BG_ALT);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lbl = Styles.bodyLabel(toLabel.apply(item));
            row.add(lbl, BorderLayout.CENTER);

            if (typeCode > 0) {
                JButton btnDetail = Styles.secondaryButton("D\u00e9tails");
                btnDetail.setFont(Styles.FONT_SMALL);
                int encoded = typeCode * 1_000_000 + toId.apply(item);
                btnDetail.addActionListener(e -> {
                    try { catalogueIntQueue.put(encoded); } catch (InterruptedException ignored) {}
                });
                row.add(btnDetail, BorderLayout.EAST);
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

        JLabel t = Styles.titleLabel(titre);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setBorder(new EmptyBorder(0, 0, Styles.PADDING_MD, 0));
        p.add(t);

        JPanel card = Styles.cardPanel();
        card.setLayout(new GridBagLayout());
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(600, Integer.MAX_VALUE));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 8, 4, 16);

        int row = 0;
        for (String[] kv : lignes) {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
            JLabel key = new JLabel(kv[0] + " :");
            key.setFont(Styles.FONT_BODY_BOLD);
            key.setForeground(Styles.TEXT_MUTED);
            card.add(key, gbc);

            gbc.gridx = 1; gbc.weightx = 1;
            JLabel val = new JLabel(kv[1]);
            val.setFont(Styles.FONT_BODY);
            val.setForeground(Styles.TEXT);
            card.add(val, gbc);
            row++;
        }
        p.add(card);
        return p;
    }

    // ==================== CARTE PLAYLISTS ====================

    private JPanel          playlistsContentArea;
    private JLabel          playlistsStatusLabel;
    private final java.util.concurrent.SynchronousQueue<Object> playlistsQueue =
            new java.util.concurrent.SynchronousQueue<>();
    // Stocke la liste courante de playlists pour les clics
    private List<model.Playlist> playlistsCourantes = new java.util.ArrayList<>();

    private JPanel buildPlaylistsCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Styles.BG_MAIN);

        // ---- Barre haute ----
        JPanel topBar = new JPanel();
        topBar.setBackground(Styles.BG_MAIN);
        topBar.setLayout(new BoxLayout(topBar, BoxLayout.Y_AXIS));
        topBar.setBorder(new EmptyBorder(Styles.PADDING_LG * 2, Styles.PADDING_LG * 2,
                Styles.PADDING_MD, Styles.PADDING_LG * 2));

        JLabel titre = Styles.titleLabel("Mes playlists");
        titre.setAlignmentX(Component.LEFT_ALIGNMENT);
        topBar.add(titre);
        topBar.add(Box.createVerticalStrut(Styles.PADDING_SM));

        // Bouton Nouvelle playlist
        JButton btnNew = Styles.primaryButton("+ Nouvelle playlist");
        btnNew.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnNew.setMaximumSize(new Dimension(200, 40));
        btnNew.addActionListener(e -> {
            try { playlistsQueue.put("new"); } catch (InterruptedException ignored) {}
        });
        topBar.add(btnNew);
        topBar.add(Box.createVerticalStrut(Styles.PADDING_SM));

        playlistsStatusLabel = new JLabel(" ");
        playlistsStatusLabel.setFont(Styles.FONT_SMALL);
        playlistsStatusLabel.setForeground(new Color(22, 163, 74));
        playlistsStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topBar.add(playlistsStatusLabel);

        card.add(topBar, BorderLayout.NORTH);

        // ---- Zone scrollable ----
        playlistsContentArea = new JPanel();
        playlistsContentArea.setBackground(Styles.BG_MAIN);
        playlistsContentArea.setLayout(new BoxLayout(playlistsContentArea, BoxLayout.Y_AXIS));
        playlistsContentArea.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2,
                Styles.PADDING_LG, Styles.PADDING_LG * 2));

        JPanel playlistsWrapper = new JPanel(new BorderLayout());
        playlistsWrapper.setBackground(Styles.BG_MAIN);
        playlistsWrapper.add(playlistsContentArea, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(playlistsWrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Styles.BG_MAIN);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    /**
     * Repeuple la zone playlists avec les playlists fournies.
     * Chaque carte playlist a 3 boutons : Voir/Écouter, Ajouter morceau, Supprimer.
     */
    private void refreshPlaylistsUI(List<model.Playlist> playlists) {
        playlistsCourantes = playlists;
        runOnEdt(() -> {
            playlistsContentArea.removeAll();

            if (playlists.isEmpty()) {
                JLabel vide = Styles.mutedLabel("Aucune playlist pour le moment. Cliquez sur \u00ab\u00a0+ Nouvelle playlist\u00a0\u00bb pour commencer.");
                vide.setAlignmentX(Component.LEFT_ALIGNMENT);
                vide.setBorder(new EmptyBorder(Styles.PADDING_MD, 0, 0, 0));
                playlistsContentArea.add(vide);
            } else {
                for (model.Playlist p : playlists) {
                    playlistsContentArea.add(buildPlaylistRow(p));
                    playlistsContentArea.add(Box.createVerticalStrut(Styles.PADDING_SM));
                }
            }
            playlistsContentArea.revalidate();
            playlistsContentArea.repaint();
        });
    }

    private JPanel buildPlaylistRow(model.Playlist p) {
        JPanel row = Styles.cardPanel();
        row.setLayout(new BorderLayout(Styles.PADDING_MD, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Infos
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel nom = new JLabel(p.getNom());
        nom.setFont(Styles.FONT_BODY_BOLD);
        nom.setForeground(Styles.TEXT);
        JLabel details = new JLabel(p.getMorceaux().size() + " morceau(x)  \u2022  " + p.getDureeTotaleFormatee());
        details.setFont(Styles.FONT_SMALL);
        details.setForeground(Styles.TEXT_MUTED);
        info.add(nom);
        info.add(details);
        row.add(info, BorderLayout.CENTER);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Styles.PADDING_SM, 0));
        actions.setOpaque(false);

        JButton btnVoir = Styles.secondaryButton("Voir");
        btnVoir.setFont(Styles.FONT_SMALL);
        btnVoir.addActionListener(e -> {
            try { playlistsQueue.put("voir:" + p.getId()); } catch (InterruptedException ignored) {}
        });

        JButton btnAjouter = Styles.primaryButton("+ Morceau");
        btnAjouter.setFont(Styles.FONT_SMALL);
        btnAjouter.addActionListener(e -> {
            try { playlistsQueue.put("ajouter:" + p.getId()); } catch (InterruptedException ignored) {}
        });

        JButton btnSuppr = Styles.dangerButton("Supprimer");
        btnSuppr.setFont(Styles.FONT_SMALL);
        btnSuppr.addActionListener(e -> {
            try { playlistsQueue.put("supprimer:" + p.getId()); } catch (InterruptedException ignored) {}
        });

        actions.add(btnVoir);
        actions.add(btnAjouter);
        actions.add(btnSuppr);
        row.add(actions, BorderLayout.EAST);

        return row;
    }

    // ==================== CARTE ÉCOUTE ====================

    private JTextField      ecouteSearchField;
    private JPanel          ecouteContentArea;
    private JProgressBar    ecouteProgressBar;
    private JLabel          ecouteTitreCourant;
    private JLabel          ecouteInterpreteLabel;
    private JLabel          ecouteLimiteLabel;
    private JPanel          ecoutePlayerPanel;
    private final java.util.concurrent.SynchronousQueue<Object> ecouteQueue =
            new java.util.concurrent.SynchronousQueue<>();

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

        JButton btnSearch = Styles.primaryButton("\uD83D\uDD0D Rechercher");
        btnSearch.setPreferredSize(new Dimension(130, 40));
        searchBar.add(btnSearch, BorderLayout.EAST);
        topBar.add(searchBar);
        card.add(topBar, BorderLayout.NORTH);

        // ---- Zone résultats ----
        ecouteContentArea = new JPanel();
        ecouteContentArea.setBackground(Styles.BG_MAIN);
        ecouteContentArea.setLayout(new BoxLayout(ecouteContentArea, BoxLayout.Y_AXIS));
        ecouteContentArea.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2,
                Styles.PADDING_MD, Styles.PADDING_LG * 2));

        JPanel ecouteWrapper = new JPanel(new BorderLayout());
        ecouteWrapper.setBackground(Styles.BG_MAIN);
        ecouteWrapper.add(ecouteContentArea, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(ecouteWrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Styles.BG_MAIN);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        // ---- Lecteur en bas ----
        ecoutePlayerPanel = new JPanel();
        ecoutePlayerPanel.setBackground(Styles.BG_ALT);
        ecoutePlayerPanel.setLayout(new BoxLayout(ecoutePlayerPanel, BoxLayout.Y_AXIS));
        ecoutePlayerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Styles.BORDER),
                BorderFactory.createEmptyBorder(Styles.PADDING_MD, Styles.PADDING_LG * 2,
                        Styles.PADDING_MD, Styles.PADDING_LG * 2)));
        ecoutePlayerPanel.setVisible(false);

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

        ecoutePlayerPanel.add(ecouteTitreCourant);
        ecoutePlayerPanel.add(Box.createVerticalStrut(2));
        ecoutePlayerPanel.add(ecouteInterpreteLabel);
        ecoutePlayerPanel.add(Box.createVerticalStrut(Styles.PADDING_SM));
        ecoutePlayerPanel.add(ecouteProgressBar);

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

        JLabel titre = Styles.titleLabel("Mon historique d'\u00e9coute");
        titre.setAlignmentX(Component.LEFT_ALIGNMENT);
        topBar.add(titre);
        topBar.add(Box.createVerticalStrut(Styles.PADDING_SM));

        JLabel sub = Styles.mutedLabel("Les morceaux que vous avez \u00e9cout\u00e9s r\u00e9cemment.");
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        topBar.add(sub);

        card.add(topBar, BorderLayout.NORTH);

        historiqueContentArea = new JPanel();
        historiqueContentArea.setBackground(Styles.BG_MAIN);
        historiqueContentArea.setLayout(new BoxLayout(historiqueContentArea, BoxLayout.Y_AXIS));
        historiqueContentArea.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2,
                Styles.PADDING_LG, Styles.PADDING_LG * 2));

        JPanel historiqueWrapper = new JPanel(new BorderLayout());
        historiqueWrapper.setBackground(Styles.BG_MAIN);
        historiqueWrapper.add(historiqueContentArea, BorderLayout.NORTH);

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
    private final java.util.concurrent.SynchronousQueue<Object> adminQueue =
            new java.util.concurrent.SynchronousQueue<>();

    // Références aux composants mis à jour dynamiquement
    private JPanel adminCatalogueContent;
    private JPanel adminComptesContent;
    private JLabel adminComptesStatus;
    private JPanel adminStatsCard;   // le JPanel complet de la carte statistiques

    // ---- Carte : Gérer le catalogue ----
    private JPanel buildAdminCatalogueCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Styles.BG_MAIN);

        JPanel top = new JPanel();
        top.setBackground(Styles.BG_MAIN);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(Styles.PADDING_LG * 2, Styles.PADDING_LG * 2, Styles.PADDING_MD, Styles.PADDING_LG * 2));

        JLabel titre = Styles.titleLabel("G\u00e9rer le catalogue");
        titre.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(titre);
        top.add(Box.createVerticalStrut(Styles.PADDING_MD));

        // Boutons d'action organisés en grille 2 colonnes
        JPanel grid = new JPanel(new GridLayout(0, 2, Styles.PADDING_MD, Styles.PADDING_SM));
        grid.setBackground(Styles.BG_MAIN);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[][] actions = {
                {"+ Ajouter morceau",          "ajouterMorceau"},
                {"\u2212 Supprimer morceau",   "supprimerMorceau"},
                {"+ Ajouter album",            "ajouterAlbum"},
                {"\u2212 Supprimer album",     "supprimerAlbum"},
                {"+ Morceau \u2192 Album",     "ajouterMorceauAlbum"},
                {"+ Ajouter artiste",          "ajouterArtiste"},
                {"\u2212 Supprimer artiste",   "supprimerArtiste"},
                {"+ Ajouter groupe",           "ajouterGroupe"},
                {"\u2212 Supprimer groupe",    "supprimerGroupe"},
                {"+ Membre \u2192 Groupe",     "ajouterMembreGroupe"},
        };
        for (String[] a : actions) {
            boolean isDanger = a[0].startsWith("\u2212");
            JButton b = isDanger ? Styles.dangerButton(a[0]) : Styles.primaryButton(a[0]);
            b.setFont(Styles.FONT_SMALL);
            final String key = a[1];
            b.addActionListener(e -> { try { adminQueue.put(key); } catch (InterruptedException ignored) {} });
            grid.add(b);
        }
        top.add(grid);
        card.add(top, BorderLayout.NORTH);

        // Zone de feedback scrollable
        adminCatalogueContent = new JPanel();
        adminCatalogueContent.setBackground(Styles.BG_MAIN);
        adminCatalogueContent.setLayout(new BoxLayout(adminCatalogueContent, BoxLayout.Y_AXIS));
        adminCatalogueContent.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2, Styles.PADDING_LG, Styles.PADDING_LG * 2));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Styles.BG_MAIN);
        wrapper.add(adminCatalogueContent, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Styles.BG_MAIN);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        return card;
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
        card.add(top, BorderLayout.NORTH);

        adminComptesContent = new JPanel();
        adminComptesContent.setBackground(Styles.BG_MAIN);
        adminComptesContent.setLayout(new BoxLayout(adminComptesContent, BoxLayout.Y_AXIS));
        adminComptesContent.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2, Styles.PADDING_LG, Styles.PADDING_LG * 2));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Styles.BG_MAIN);
        wrapper.add(adminComptesContent, BorderLayout.NORTH);
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

    /** Met à jour la zone feedback du catalogue admin. */
    private void setAdminCatalogueMsg(String msg, boolean success) {
        runOnEdt(() -> {
            adminCatalogueContent.removeAll();
            JLabel l = new JLabel(msg);
            l.setFont(Styles.FONT_BODY);
            l.setForeground(success ? new Color(22, 163, 74) : new Color(220, 38, 38));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            l.setBorder(new EmptyBorder(Styles.PADDING_MD, 0, 0, 0));
            adminCatalogueContent.add(l);
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
                header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
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
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);

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

    /** Navigue vers la carte connexion admin avant que le contrôleur demande les saisies. */
    @Override public void afficherConnexionAdmin() {
        if (SwingUtilities.isEventDispatchThread()) {
            showCard("connexionAdmin");
        } else {
            try { SwingUtilities.invokeAndWait(() -> showCard("connexionAdmin")); }
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
                        runOnEdt(() -> showCard("gestionCatalogue"));
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

    // Saisies via dialog pour les formulaires admin
    @Override public String demanderTitreMorceau()     { return promptText("Nouveau morceau (1/5)", "Titre du morceau :"); }
    @Override public int    demanderDureeMorceau()     { return parseIntPrompt("Nouveau morceau (2/5)", "Dur\u00e9e en secondes :", 0); }
    @Override public String demanderGenreMorceau()     { return promptText("Nouveau morceau (3/5)", "Genre :"); }
    @Override public int    demanderAnneeMorceau()     { return parseIntPrompt("Nouveau morceau (4/5)", "Ann\u00e9e de sortie :", 2000); }
    @Override public int    demanderIdArtisteMorceau() { return parseIntPrompt("Nouveau morceau (5/5)", "ID de l'artiste (0 si groupe) :", 0); }
    @Override public int    demanderIdGroupeMorceau()  { return parseIntPrompt("Nouveau morceau (5/5)", "ID du groupe (0 si artiste solo) :", 0); }
    @Override public void   afficherMorceauAjoute(int id) { setAdminCatalogueMsg("Morceau ajout\u00e9 avec succ\u00e8s (ID : " + id + ") \u2714", true); }

    @Override public String demanderTitreAlbum()       { return promptText("Nouvel album (1/4)", "Titre de l'album :"); }
    @Override public int    demanderAnneeAlbum()       { return parseIntPrompt("Nouvel album (2/4)", "Ann\u00e9e de sortie :", 2000); }
    @Override public int    demanderIdArtisteAlbum()   { return parseIntPrompt("Nouvel album (3/4)", "ID de l'artiste (0 si groupe) :", 0); }
    @Override public int    demanderIdGroupeAlbum()    { return parseIntPrompt("Nouvel album (4/4)", "ID du groupe (0 si artiste solo) :", 0); }
    @Override public void   afficherAlbumAjoute(int id){ setAdminCatalogueMsg("Album ajout\u00e9 avec succ\u00e8s (ID : " + id + ") \u2714", true); }

    @Override public String demanderNomArtiste()       { return promptText("Nouvel artiste (1/3)", "Nom :"); }
    @Override public String demanderPrenomArtiste()    { return promptText("Nouvel artiste (2/3)", "Pr\u00e9nom :"); }
    @Override public String demanderNationaliteArtiste(){ return promptText("Nouvel artiste (3/3)", "Nationalit\u00e9 :"); }
    @Override public void   afficherArtisteAjoute(int id){ setAdminCatalogueMsg("Artiste ajout\u00e9 avec succ\u00e8s (ID : " + id + ") \u2714", true); }

    @Override public String demanderNomGroupe()        { return promptText("Nouveau groupe (1/3)", "Nom du groupe :"); }
    @Override public int    demanderDateCreationGroupe(){ return parseIntPrompt("Nouveau groupe (2/3)", "Ann\u00e9e de cr\u00e9ation :", 2000); }
    @Override public String demanderNationaliteGroupe(){ return promptText("Nouveau groupe (3/3)", "Nationalit\u00e9 :"); }
    @Override public void   afficherGroupeAjoute(int id){ setAdminCatalogueMsg("Groupe ajout\u00e9 avec succ\u00e8s (ID : " + id + ") \u2714", true); }

    @Override public int    demanderIdAlbumAssociation()  { return parseIntPrompt("Associer morceau \u2192 album (1/3)", "ID de l'album :", -1); }
    @Override public int    demanderIdMorceauAssociation(){ return parseIntPrompt("Associer morceau \u2192 album (2/3)", "ID du morceau :", -1); }
    @Override public int    demanderNumeroPiste()          { return parseIntPrompt("Associer morceau \u2192 album (3/3)", "Num\u00e9ro de piste :", 1); }
    @Override public void   afficherMorceauAjouteDansAlbum(String tm, String ta){ setAdminCatalogueMsg("\u00ab " + tm + " \u00bb ajout\u00e9 dans \u00ab " + ta + " \u00bb \u2714", true); }

    @Override public int    demanderIdGroupeAssociation() { return parseIntPrompt("Associer artiste \u2192 groupe (1/2)", "ID du groupe :", -1); }
    @Override public int    demanderIdArtisteAssociation(){ return parseIntPrompt("Associer artiste \u2192 groupe (2/2)", "ID de l'artiste :", -1); }
    @Override public void   afficherMembreAjouteDansGroupe(String na, String ng){ setAdminCatalogueMsg("\u00ab " + na + " \u00bb ajout\u00e9 dans \u00ab " + ng + " \u00bb \u2714", true); }

    @Override public int    demanderIdSuppression() { return parseIntPrompt("Suppression", "ID de l'\u00e9l\u00e9ment \u00e0 supprimer :", -1); }
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

            // Reconstruction complète de la carte
            adminStatsCard.removeAll();

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

            // Grille de tuiles
            JPanel grid = new JPanel(new GridLayout(0, 3, Styles.PADDING_LG, Styles.PADDING_LG));
            grid.setBackground(Styles.BG_MAIN);
            grid.setBorder(new EmptyBorder(0, Styles.PADDING_LG * 2, Styles.PADDING_LG * 2, Styles.PADDING_LG * 2));

            Object[][] stats = {
                    {"\uD83C\uDFB5 Morceaux",    nbM,  Styles.TEAL},
                    {"\uD83D\uDCBF Albums",       nbA,  new Color(99, 102, 241)},
                    {"\uD83C\uDFA4 Artistes",     nbAr, new Color(16, 185, 129)},
                    {"\uD83C\uDFB8 Groupes",      nbG,  new Color(245, 158, 11)},
                    {"\uD83D\uDC64 Utilisateurs", nbU,  new Color(59, 130, 246)},
                    {"\u25B6 \u00c9coutes totales",nbE, new Color(239, 68, 68)},
            };
            for (Object[] s : stats) {
                JPanel tile = Styles.cardPanel();
                tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
                tile.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Styles.BORDER, 1, true),
                        BorderFactory.createEmptyBorder(Styles.PADDING_LG, Styles.PADDING_LG, Styles.PADDING_MD, Styles.PADDING_LG)
                ));

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

            adminStatsCard.add(grid, BorderLayout.CENTER);
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
        return awaitSidebarChoice(new java.util.HashMap<String,Integer>(){{
            put("catalogue",1); put("playlists",2); put("ecoute",3); put("historique",4);
            put("deconnexion",5);
        }}, 5);
    }

    @Override public int afficherMenuVisiteur() {
        return awaitSidebarChoice(new java.util.HashMap<String,Integer>(){{
            put("catalogue",1); put("ecoute",2); put("deconnexion",3);
        }}, 3);
    }

    @Override public int afficherMenuCatalogue() {
        try {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(() -> showCard("catalogue"));
            }
        } catch (Exception ignored) {}

        // Si un filtre a été intercepté pendant naviguer(), on le consomme directement
        Integer enAttente = catalogueFiltreEnAttente;
        if (enAttente != null) {
            catalogueFiltreEnAttente = null;
            catalogueIntQueue.clear(); // vider les éventuels doublons
            if (enAttente == 100) {
                SwingUtilities.invokeLater(() -> { if (catalogueSearchField != null) catalogueSearchField.setText(""); });
                return 1;
            }
            return enAttente;
        }

        // Vider les valeurs parasites restantes dans la queue avant d'attendre
        catalogueIntQueue.clear();

        try {
            while (true) {
                int v = catalogueIntQueue.take();
                if (v == 100) {
                    SwingUtilities.invokeLater(() -> { if (catalogueSearchField != null) catalogueSearchField.setText(""); });
                    return 1;
                }
                return v;
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
                int v = catalogueIntQueue.take();
                if (v >= 1_000_000) {
                    // Encodage : type*1000000 + id → clic "Détails"
                    dernierTypeNav = v / 1_000_000;
                    dernierIdNav   = v % 1_000_000;
                    return dernierTypeNav;
                }
                // Valeur filtre (1-7) ou "Tout" (100) : l'utilisateur a cliqué
                // un bouton de filtre pendant la navigation → mémoriser et sortir
                // proprement sans remettre dans la queue (évite la boucle infinie)
                catalogueFiltreEnAttente = v;
                catalogueIntQueue.clear(); // vider les doublons éventuels
                return 5; // retour = sortir de naviguer()
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
            p.add(buildListPanel("Morceaux", r.morceaux,
                    m -> m.getTitre() + " \u2014 " + m.getNomInterprete() + " (" + m.getAnnee() + ")",
                    Morceau::getId));
            p.add(Box.createVerticalStrut(Styles.PADDING_MD));
            p.add(buildListPanel("Albums", r.albums,
                    a -> a.getTitre() + " \u2014 " + a.getNomInterprete() + " (" + a.getAnnee() + ")",
                    Album::getId));
            p.add(Box.createVerticalStrut(Styles.PADDING_MD));
            p.add(buildListPanel("Artistes (" + r.artistes.size() + ")", r.artistes,
                    a -> a.getNomComplet(), Artiste::getId, 3));
            p.add(Box.createVerticalStrut(Styles.PADDING_MD));
            p.add(buildListPanel("Groupes (" + r.groupes.size() + ")", r.groupes,
                    g -> g.getNom(), Groupe::getId, 4));
            setCatalogueContent(p);
        });
    }

    @Override public void afficherListeMorceaux(List<Morceau> l) {
        runOnEdt(() -> setCatalogueContent(buildListPanel("Morceaux (" + l.size() + ")", l,
                m -> m.getTitre() + " \u2014 " + m.getNomInterprete() + " (" + m.getDureeFormatee() + ")",
                Morceau::getId)));
    }

    @Override public void afficherListeAlbums(List<Album> l) {
        runOnEdt(() -> setCatalogueContent(buildListPanel("Albums (" + l.size() + ")", l,
                a -> a.getTitre() + " \u2014 " + a.getNomInterprete() + " (" + a.getAnnee() + ")",
                Album::getId)));
    }

    @Override public void afficherListeArtistes(List<Artiste> l) {
        runOnEdt(() -> setCatalogueContent(buildListPanel("Artistes (" + l.size() + ")", l,
                a -> a.getNomComplet() + "  \u2022  " + a.getNationalite(), Artiste::getId)));
    }

    @Override public void afficherListeGroupes(List<Groupe> l) {
        runOnEdt(() -> setCatalogueContent(buildListPanel("Groupes (" + l.size() + ")", l,
                g -> g.getNom() + "  \u2022  " + g.getNationalite(), Groupe::getId)));
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
        runOnEdt(() -> showCard("playlists"));
        try {
            Object o = playlistsQueue.take();
            String s = o.toString();
            if (s.equals("new"))          return 1;
            if (s.startsWith("voir:"))    return 5;
            if (s.startsWith("ajouter:")) return 3;
            if (s.startsWith("supprimer:")) return 7;
            if (s.equals("retour"))       return 8;
            return 8;
        } catch (InterruptedException e) { return 8; }
    }

    @Override public void afficherListePlaylists(List<Playlist> l) { refreshPlaylistsUI(l); }

    @Override public void afficherContenuPlaylist(Playlist p) {
        // Contenu affiché inline dans la carte via refreshPlaylistsUI
    }

    @Override public String demanderNomPlaylist() {
        return promptText("Nouvelle playlist", "Nom de la playlist :");
    }

    @Override public String demanderNouveauNomPlaylist() {
        return promptText("Renommer la playlist", "Nouveau nom :");
    }

    @Override public int demanderIdPlaylist() {
        // L'id a déjà été transmis via playlistsQueue par le bouton cliqué
        try {
            Object o = playlistsQueue.take();
            String s = o.toString();
            for (String prefix : new String[]{"voir:", "ajouter:", "supprimer:"}) {
                if (s.startsWith(prefix)) {
                    return Integer.parseInt(s.substring(prefix.length()));
                }
            }
            return -1;
        } catch (InterruptedException e) { return -1; }
    }

    @Override public int demanderIdMorceau() {
        String val = promptText("ID du morceau", "Entrez l'ID du morceau :");
        try { return Integer.parseInt(val.trim()); } catch (NumberFormatException e) { return -1; }
    }

    @Override public void afficherPlaylistCreee(int id, String nom) {
        runOnEdt(() -> {
            playlistsStatusLabel.setForeground(new Color(22, 163, 74));
            playlistsStatusLabel.setText("Playlist \u00ab\u00a0" + nom + "\u00a0\u00bb cr\u00e9\u00e9e avec succ\u00e8s \u2714");
        });
    }

    @Override public void afficherPlaylistRenommee(String ancien, String nouveau) {
        runOnEdt(() -> {
            playlistsStatusLabel.setForeground(new Color(22, 163, 74));
            playlistsStatusLabel.setText("\u00ab\u00a0" + ancien + "\u00a0\u00bb renomm\u00e9e en \u00ab\u00a0" + nouveau + "\u00a0\u00bb \u2714");
        });
    }

    @Override public void afficherMorceauAjoutePlaylist(String titre, String nomPlaylist) {
        runOnEdt(() -> {
            playlistsStatusLabel.setForeground(new Color(22, 163, 74));
            playlistsStatusLabel.setText("\u00ab\u00a0" + titre + "\u00a0\u00bb ajout\u00e9 \u00e0 \u00ab\u00a0" + nomPlaylist + "\u00a0\u00bb \u2714");
        });
    }

    @Override public void afficherMorceauRetire() {
        runOnEdt(() -> {
            playlistsStatusLabel.setForeground(new Color(22, 163, 74));
            playlistsStatusLabel.setText("Morceau retir\u00e9 de la playlist \u2714");
        });
    }

    @Override public void afficherPlaylistSupprimee(String nom) {
        runOnEdt(() -> {
            playlistsStatusLabel.setForeground(new Color(220, 38, 38));
            playlistsStatusLabel.setText("Playlist \u00ab\u00a0" + nom + "\u00a0\u00bb supprim\u00e9e.");
        });
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
                labelErreurConnexion[0].setText("Ce compte n'est pas un compte administrateur.");
        });
    }

    @Override public void afficherInscriptionReussie() {
        runOnEdt(() -> inscErreur.setText("Compte cr\u00e9\u00e9 avec succ\u00e8s ! Vous pouvez maintenant vous connecter."));
        // Afficher le message en vert
        runOnEdt(() -> inscErreur.setForeground(new Color(22, 163, 74)));
    }

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
    @Override public void notifierSessionVisiteur() {
        setSessionState(SessionState.VISITEUR, null);
    }

    // ==================== ÉCOUTE ====================

    @Override public String demanderRechercheMusique() {
        // Afficher la carte écoute et attendre que l'utilisateur tape + clique Rechercher
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                showCard("ecoute");
                if (ecouteSearchField != null) {
                    ecouteSearchField.setText("");
                    ecouteSearchField.requestFocusInWindow();
                }
            } else {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        showCard("ecoute");
                        if (ecouteSearchField != null) {
                            ecouteSearchField.setText("");
                            ecouteSearchField.requestFocusInWindow();
                        }
                    });
                } catch (java.lang.reflect.InvocationTargetException ex) { /* ignore */ }
            }
            // Attendre la valeur "search:..." poussée par le bouton Rechercher
            while (true) {
                Object o = ecouteQueue.take();
                if (o instanceof String && o.toString().startsWith("search:")) {
                    return o.toString().substring(7); // texte après "search:"
                }
                // Si c'est un Integer (clic ▶ avant recherche) on remet dans la queue
                // et on retourne vide pour que le contrôleur relance
                ecouteQueue.put(o);
                return "";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    @Override public void afficherResultatsEcoute(List<Morceau> resultats) {
        runOnEdt(() -> {
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

                    JLabel lbl = Styles.bodyLabel(m.getTitre() + " \u2014 " + m.getNomInterprete()
                            + "  \u2022  " + m.getDureeFormatee());
                    row.add(lbl, BorderLayout.CENTER);

                    JButton btn = Styles.primaryButton("\u25B6 \u00c9couter");
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
        });
    }

    @Override public int demanderIdMorceauEcoute() {
        try {
            Object o = ecouteQueue.take();
            if (o instanceof Integer) return (Integer) o;
            // search: prefix → on ignore, le contrôleur reposera la question
            return -1;
        } catch (InterruptedException e) { return -1; }
    }

    /** Simule la lecture : barre de progression animée sur 3 secondes. */
    @Override public void afficherEcoute(Morceau m) {
        SwingUtilities.invokeLater(() -> {
            ecouteTitreCourant.setText(m.getTitre());
            ecouteInterpreteLabel.setText(m.getNomInterprete() + "  \u2022  " + m.getDureeFormatee());
            ecouteProgressBar.setValue(0);
            ecoutePlayerPanel.setVisible(true);
        });

        // Animation 3 s sur le thread contrôleur — met à jour la barre via invokeLater
        final int STEPS = 60;
        final int DELAY_MS = 50;
        for (int i = 1; i <= STEPS; i++) {
            final int val = (i * 100) / STEPS;
            SwingUtilities.invokeLater(() -> ecouteProgressBar.setValue(val));
            try { Thread.sleep(DELAY_MS); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        SwingUtilities.invokeLater(() -> ecouteProgressBar.setValue(100));
    }

    @Override public int afficherMenuApresEcoute(int restantes) {
        // En mode graphique on propose immédiatement une nouvelle recherche
        // (la barre de progression s'est terminée) → retourner 1 = continuer
        if (restantes == 0) return 2; // limite atteinte : arrêter
        runOnEdt(() -> {
            if (restantes > 0) {
                ecouteLimiteLabel.setText(restantes + " \u00e9coute(s) restante(s) cette session.");
            } else {
                ecouteLimiteLabel.setText(""); // abonné : illimité
            }
        });
        return 1; // continuer l'écoute
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
        return 2; // toujours passer au suivant en mode graphique
    }

    @Override public void afficherLecturePlaylist(String nom) {
        runOnEdt(() -> {
            playlistsStatusLabel.setForeground(Styles.TEAL);
            playlistsStatusLabel.setText("Lecture de \u00ab\u00a0" + nom + "\u00a0\u00bb en cours...");
        });
    }

    @Override public void afficherFinPlaylist(String nom) {
        runOnEdt(() -> {
            playlistsStatusLabel.setForeground(Styles.TEXT_MUTED);
            playlistsStatusLabel.setText("Lecture de \u00ab\u00a0" + nom + "\u00a0\u00bb termin\u00e9e.");
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
                // En-têtes du tableau
                JPanel header = new JPanel(new GridLayout(1, 4, Styles.PADDING_MD, 0));
                header.setBackground(Styles.TEAL_SURFACE);
                header.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
                header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
                header.setAlignmentX(Component.LEFT_ALIGNMENT);
                for (String col : new String[]{"Titre", "Interpr\u00e8te", "Ann\u00e9e", "Date / heure"}) {
                    JLabel h = new JLabel(col);
                    h.setFont(Styles.FONT_BODY_BOLD);
                    h.setForeground(Styles.TEAL_DARK);
                    header.add(h);
                }
                historiqueContentArea.add(header);

                // Lignes
                for (int i = 0; i < historique.size(); i++) {
                    model.Historique h = historique.get(i);
                    JPanel row = new JPanel(new GridLayout(1, 4, Styles.PADDING_MD, 0));
                    row.setBackground(i % 2 == 0 ? Styles.BG_MAIN : Styles.BG_ALT);
                    row.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER),
                            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);

                    for (String val : new String[]{h.getTitre(), h.getInterprete(),
                            "" + h.getAnnee(), h.getDateHeure()}) {
                        JLabel cell = Styles.bodyLabel(val);
                        row.add(cell);
                    }
                    historiqueContentArea.add(row);
                }
            }
            historiqueContentArea.revalidate();
            historiqueContentArea.repaint();
        });
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
        String msg = noteActuelle > 0
                ? "Votre note actuelle : " + noteActuelle + "/5\n1. Modifier ma note\n2. Passer"
                : "1. Noter ce morceau (1 à 5)\n2. Passer";
        String[] options = {"Noter", "Passer"};
        int rep = JOptionPane.showOptionDialog(fenetre, msg, "Notation",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        return (rep == 0) ? 1 : 2;
    }

    @Override public int demanderNote() {
        return parseIntPrompt("Note", "Votre note (1 à 5) :", 0);
    }

    @Override public void afficherNoteEnregistree(int note) {
        StringBuilder etoiles = new StringBuilder();
        for (int i = 1; i <= 5; i++) etoiles.append(i <= note ? "\u2605" : "\u2606");
        showInfo("Note enregistr\u00e9e : " + etoiles + " (" + note + "/5)");
    }

}