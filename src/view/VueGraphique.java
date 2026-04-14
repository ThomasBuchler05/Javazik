package view;

import model.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Vue graphique Swing — étend VueConsole pour hériter de toutes ses méthodes
 * et surcharger uniquement celles qui ont un rendu graphique.
 */
public class VueGraphique extends VueConsole {

    private JFrame fenetre;
    private JTextArea zoneAffichage;
    private JTextField champSaisie;

    // ActionListener courant (on le remplace à chaque appel de lireLigne)
    private java.awt.event.ActionListener listenerActuel = null;

    public VueGraphique() {
        fenetre = new JFrame("JavaZik 🎵");
        fenetre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fenetre.setSize(700, 500);
        fenetre.setLayout(new BorderLayout());

        // Zone d'affichage
        zoneAffichage = new JTextArea();
        zoneAffichage.setEditable(false);
        zoneAffichage.setFont(new Font("Monospaced", Font.PLAIN, 13));
        zoneAffichage.setBackground(new Color(30, 30, 30));
        zoneAffichage.setForeground(new Color(200, 230, 200));
        zoneAffichage.setMargin(new Insets(10, 10, 10, 10));
        fenetre.add(new JScrollPane(zoneAffichage), BorderLayout.CENTER);

        // Champ de saisie en bas
        champSaisie = new JTextField();
        champSaisie.setFont(new Font("Monospaced", Font.PLAIN, 13));
        champSaisie.setBackground(new Color(50, 50, 50));
        champSaisie.setForeground(Color.WHITE);
        champSaisie.setCaretColor(Color.WHITE);
        JLabel labelPrompt = new JLabel("  > ");
        labelPrompt.setForeground(new Color(100, 200, 100));
        labelPrompt.setFont(new Font("Monospaced", Font.BOLD, 13));
        JPanel panneauBas = new JPanel(new BorderLayout());
        panneauBas.setBackground(new Color(40, 40, 40));
        panneauBas.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(80, 80, 80)));
        panneauBas.add(labelPrompt, BorderLayout.WEST);
        panneauBas.add(champSaisie, BorderLayout.CENTER);
        fenetre.add(panneauBas, BorderLayout.SOUTH);

        fenetre.setLocationRelativeTo(null);
        fenetre.setVisible(true);
        champSaisie.requestFocus();
    }

    // ==================== UTILITAIRES INTERNES ====================

    private void afficher(String texte) {
        SwingUtilities.invokeLater(() -> {
            zoneAffichage.append(texte + "\n");
            zoneAffichage.setCaretPosition(zoneAffichage.getDocument().getLength());
        });
    }

    /**
     * Bloque jusqu'à ce que l'utilisateur appuie sur Entrée.
     * On supprime l'ancien listener avant d'en ajouter un nouveau
     * pour éviter les accumulations.
     */
    private String lireLigne() {
        final String[] resultat = {null};

        // Retirer l'ancien listener s'il existe
        if (listenerActuel != null) {
            champSaisie.removeActionListener(listenerActuel);
        }

        listenerActuel = e -> {
            resultat[0] = champSaisie.getText().trim();
            champSaisie.setText("");
        };
        champSaisie.addActionListener(listenerActuel);

        while (resultat[0] == null) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        final String valeur = resultat[0];
        afficher("  → " + valeur);
        return valeur;
    }

    private int lireEntier() {
        while (true) {
            try {
                return Integer.parseInt(lireLigne());
            } catch (NumberFormatException e) {
                afficher("⚠ Saisie invalide, entrez un nombre.");
            }
        }
    }

    /**
     * Affiche une boîte de dialogue modale avec des boutons numérotés.
     * Doit être appelable depuis n'importe quel thread : utilise invokeAndWait
     * pour garantir que la création et l'affichage se font sur l'EDT.
     */
    private int afficherMenuBoutons(String titre, String... options) {
        final int[] choix = {-1};
        try {
            SwingUtilities.invokeAndWait(() -> {
                JDialog dialog = new JDialog(fenetre, titre, true);
                dialog.setLayout(new GridLayout(options.length, 1, 5, 5));
                dialog.setSize(350, 50 + options.length * 45);
                dialog.setLocationRelativeTo(fenetre);

                for (int i = 0; i < options.length; i++) {
                    final int num = i + 1;
                    JButton btn = new JButton(num + ". " + options[i]);
                    btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
                    btn.addActionListener(e -> {
                        choix[0] = num;
                        dialog.dispose();
                    });
                    dialog.add(btn);
                }
                dialog.setVisible(true); // boucle d'événements imbriquée jusqu'à dispose()
            });
        } catch (InterruptedException | java.lang.reflect.InvocationTargetException ignored) {}
        return choix[0];
    }

    // ==================== MENU PRINCIPAL ====================

    @Override
    public void afficherBienvenue() {
        afficher("====================================");
        afficher("     BIENVENUE SUR JAVAZIK 🎵");
        afficher("====================================\n");
    }

    @Override
    public int afficherMenuPrincipal() {
        return afficherMenuBoutons("Menu Principal",
                "Se connecter en tant qu'administrateur",
                "Se connecter en tant que client",
                "Créer un compte client",
                "Continuer en tant que visiteur",
                "Quitter");
    }

    // ==================== MENU ADMIN ====================

    @Override
    public int afficherMenuAdmin() {
        return afficherMenuBoutons("Menu Administrateur",
                "Ajouter un morceau", "Supprimer un morceau",
                "Ajouter un album", "Supprimer un album",
                "Ajouter un morceau dans un album",
                "Ajouter un artiste", "Supprimer un artiste",
                "Ajouter un groupe", "Supprimer un groupe",
                "Ajouter un membre à un groupe",
                "Gérer les comptes abonnés",
                "Consulter les statistiques",
                "Retour au menu principal");
    }

    // ==================== MENU CLIENT ====================

    @Override
    public int afficherMenuClient() {
        return afficherMenuBoutons("Menu Client",
                "Consulter le catalogue",
                "Créer et gérer une playlist",
                "Écouter un morceau",
                "Consulter l'historique d'écoute",
                "Revenir au menu principal");
    }

    // ==================== MENU VISITEUR ====================

    @Override
    public int afficherMenuVisiteur() {
        return afficherMenuBoutons("Menu Visiteur",
                "Consulter le catalogue",
                "Écouter un morceau (5 écoutes max)",
                "Retour au menu principal");
    }

    // ==================== MENU CATALOGUE ====================

    @Override
    public int afficherMenuCatalogue() {
        return afficherMenuBoutons("Catalogue",
                "Rechercher (morceau, album, artiste, groupe)",
                "Voir tous les morceaux",
                "Voir tous les albums",
                "Voir tous les artistes",
                "Voir tous les groupes",
                "Morceaux par genre",
                "Retour");
    }

    @Override
    public int afficherMenuNavigation() {
        return afficherMenuBoutons("Navigation",
                "Détails d'un morceau (par ID)",
                "Détails d'un album (par ID)",
                "Détails d'un artiste (par ID)",
                "Détails d'un groupe (par ID)",
                "Retour");
    }

    // ==================== CONNEXION ====================

    @Override
    public void afficherConnexionAdmin() { afficher("\n--- Connexion Administrateur ---"); }

    @Override
    public String demanderMail() { afficher("Email :"); return lireLigne(); }

    @Override
    public String demanderMdp() {
        final String[] mdp = {""};
        try {
            SwingUtilities.invokeAndWait(() -> {
                String saisie = JOptionPane.showInputDialog(fenetre, "Mot de passe :", "Connexion", JOptionPane.QUESTION_MESSAGE);
                mdp[0] = saisie != null ? saisie : "";
            });
        } catch (InterruptedException | java.lang.reflect.InvocationTargetException ignored) {}
        return mdp[0];
    }

    @Override
    public void afficherConnexionReussie() { afficher("✅ Connexion réussie !"); }

    @Override
    public void afficherMdpIncorrect() { afficher("❌ Mot de passe incorrect."); }

    @Override
    public void afficherMailIncorrect() { afficher("❌ Email introuvable."); }

    @Override
    public void afficherPasAdmin() { afficher("❌ Ce compte n'est pas administrateur."); }

    // ==================== INSCRIPTION ====================

    @Override
    public String demanderNom() { afficher("Nom :"); return lireLigne(); }

    @Override
    public String demanderPrenom() { afficher("Prénom :"); return lireLigne(); }

    @Override
    public String demanderEmail() { afficher("Email :"); return lireLigne(); }

    @Override
    public String demanderMotDePasse() {
        final String[] mdp = {""};
        try {
            SwingUtilities.invokeAndWait(() -> {
                String saisie = JOptionPane.showInputDialog(fenetre, "Mot de passe :", "Inscription", JOptionPane.QUESTION_MESSAGE);
                mdp[0] = saisie != null ? saisie : "";
            });
        } catch (InterruptedException | java.lang.reflect.InvocationTargetException ignored) {}
        return mdp[0];
    }

    @Override
    public void afficherInscriptionReussie() { afficher("✅ Inscription réussie !"); }

    // ==================== CATALOGUE ====================

    @Override
    public String demanderRecherche() { afficher("Recherche :"); return lireLigne(); }

    @Override
    public void afficherResultatsRecherche(Catalogue.ResultatRecherche r) {
        if (r.estVide()) { afficher("Aucun résultat trouvé."); return; }
        afficher("\n" + r.getNombreTotal() + " résultat(s) trouvé(s) :");
        if (!r.getMorceaux().isEmpty()) {
            afficher("  -- Morceaux --");
            for (Morceau m : r.getMorceaux()) afficher("  " + m);
        }
        if (!r.getAlbums().isEmpty()) {
            afficher("  -- Albums --");
            for (Album a : r.getAlbums()) afficher("  " + a);
        }
        if (!r.getArtistes().isEmpty()) {
            afficher("  -- Artistes --");
            for (Artiste a : r.getArtistes()) afficher("  " + a);
        }
        if (!r.getGroupes().isEmpty()) {
            afficher("  -- Groupes --");
            for (Groupe g : r.getGroupes()) afficher("  " + g);
        }
    }

    @Override
    public int demanderIdElement() { afficher("ID :"); return lireEntier(); }

    @Override
    public void afficherGenresDisponibles(List<String> genres) {
        afficher("\n--- Genres disponibles ---");
        for (int i = 0; i < genres.size(); i++)
            afficher("  " + (i + 1) + ". " + genres.get(i));
        afficher("Choisissez un genre (numéro) :");
    }

    @Override
    public void afficherListeMorceaux(List<Morceau> morceaux) {
        afficher("\n--- Morceaux ---");
        if (morceaux.isEmpty()) { afficher("  (aucun morceau)"); return; }
        for (Morceau m : morceaux) afficher("  " + m);
    }

    @Override
    public void afficherListeAlbums(List<Album> albums) {
        afficher("\n--- Albums ---");
        if (albums.isEmpty()) { afficher("  (aucun album)"); return; }
        for (Album a : albums) afficher("  " + a);
    }

    @Override
    public void afficherListeArtistes(List<Artiste> artistes) {
        afficher("\n--- Artistes ---");
        if (artistes.isEmpty()) { afficher("  (aucun artiste)"); return; }
        for (Artiste a : artistes) afficher("  " + a);
    }

    @Override
    public void afficherListeGroupes(List<Groupe> groupes) {
        afficher("\n--- Groupes ---");
        if (groupes.isEmpty()) { afficher("  (aucun groupe)"); return; }
        for (Groupe g : groupes) afficher("  " + g);
    }

    // ==================== DÉTAILS ====================

    @Override
    public void afficherDetailsMorceau(Morceau m) {
        afficher("\n====== MORCEAU ======");
        afficher("  ID        : " + m.getId());
        afficher("  Titre     : " + m.getTitre());
        afficher("  Interprète: " + m.getNomInterprete());
        afficher("  Genre     : " + m.getGenre());
        afficher("  Durée     : " + m.getDureeFormatee());
        afficher("  Année     : " + m.getAnnee());
    }

    @Override
    public void afficherAlbumsDuMorceau(List<Album> albums) {
        if (albums.isEmpty()) { afficher("  Albums : (aucun)"); return; }
        afficher("  Présent dans :");
        for (Album a : albums) afficher("    - " + a.getTitre() + " [ID:" + a.getId() + "]");
    }

    @Override
    public void afficherAutresMorceauxInterprete(List<Morceau> autres) {
        if (!autres.isEmpty()) {
            afficher("  Autres morceaux du même interprète :");
            for (Morceau m : autres) afficher("    - " + m.getTitre() + " (" + m.getAnnee() + ") [ID:" + m.getId() + "]");
        }
    }

    @Override
    public void afficherDetailsAlbum(Album a) {
        afficher("\n====== ALBUM ======");
        afficher("  ID        : " + a.getId());
        afficher("  Titre     : " + a.getTitre());
        afficher("  Interprète: " + a.getNomInterprete());
        afficher("  Année     : " + a.getAnnee());
        List<Morceau> morceaux = a.getMorceaux();
        if (!morceaux.isEmpty()) {
            afficher("  Morceaux :");
            for (Morceau m : morceaux) afficher("    - " + m.getTitre() + " [ID:" + m.getId() + "]");
        }
    }

    @Override
    public void afficherAutresAlbumsInterprete(List<Album> autres) {
        if (!autres.isEmpty()) {
            afficher("  Autres albums du même interprète :");
            for (Album a : autres) afficher("    - " + a.getTitre() + " (" + a.getAnnee() + ") [ID:" + a.getId() + "]");
        }
    }

    @Override
    public void afficherDetailsArtiste(Artiste a) {
        afficher("\n====== ARTISTE ======");
        afficher("  ID          : " + a.getId());
        afficher("  Nom         : " + a.getNomComplet());
        afficher("  Nationalité : " + a.getNationalite());
    }

    @Override
    public void afficherGroupesDeLArtiste(List<Groupe> groupes) {
        if (!groupes.isEmpty()) {
            afficher("  Membre de :");
            for (Groupe g : groupes) afficher("    - " + g.getNom() + " [ID:" + g.getId() + "]");
        }
    }

    @Override
    public void afficherMorceauxArtiste(List<Morceau> morceaux) {
        if (!morceaux.isEmpty()) {
            afficher("  Morceaux :");
            for (Morceau m : morceaux) afficher("    - " + m.getTitre() + " (" + m.getAnnee() + ") [ID:" + m.getId() + "]");
        }
    }

    @Override
    public void afficherAlbumsArtiste(List<Album> albums) {
        if (!albums.isEmpty()) {
            afficher("  Albums :");
            for (Album a : albums) afficher("    - " + a.getTitre() + " (" + a.getAnnee() + ") [ID:" + a.getId() + "]");
        }
    }

    @Override
    public void afficherDetailsGroupe(Groupe g) {
        afficher("\n====== GROUPE ======");
        afficher("  ID          : " + g.getId());
        afficher("  Nom         : " + g.getNom());
        afficher("  Créé en     : " + g.getDateCreation());
        afficher("  Nationalité : " + g.getNationalite());
    }

    @Override
    public void afficherMorceauxGroupe(List<Morceau> morceaux) {
        if (!morceaux.isEmpty()) {
            afficher("  Morceaux :");
            for (Morceau m : morceaux) afficher("    - " + m.getTitre() + " (" + m.getAnnee() + ") [ID:" + m.getId() + "]");
        }
    }

    @Override
    public void afficherAlbumsGroupe(List<Album> albums) {
        if (!albums.isEmpty()) {
            afficher("  Albums :");
            for (Album a : albums) afficher("    - " + a.getTitre() + " (" + a.getAnnee() + ") [ID:" + a.getId() + "]");
        }
    }

    // ==================== ADMIN : AJOUTS / SUPPRESSIONS ====================

    @Override
    public String demanderTitreMorceau() { afficher("Titre du morceau :"); return lireLigne(); }
    @Override
    public int demanderDureeMorceau() { afficher("Durée (secondes) :"); return lireEntier(); }
    @Override
    public String demanderGenreMorceau() { afficher("Genre :"); return lireLigne(); }
    @Override
    public int demanderAnneeMorceau() { afficher("Année :"); return lireEntier(); }
    @Override
    public int demanderIdArtisteMorceau() { afficher("ID de l'artiste (0 si groupe) :"); return lireEntier(); }
    @Override
    public int demanderIdGroupeMorceau() { afficher("ID du groupe (0 si artiste solo) :"); return lireEntier(); }
    @Override
    public void afficherMorceauAjoute(int id) { afficher("✅ Morceau ajouté (ID:" + id + ")"); }

    @Override
    public int demanderIdSuppression() { afficher("ID de l'élément à supprimer :"); return lireEntier(); }
    @Override
    public void afficherElementSupprime(String type) { afficher("✅ " + type + " supprimé."); }
    @Override
    public void afficherElementNonTrouve(String type, int id) { afficher("❌ " + type + " ID:" + id + " introuvable."); }

    @Override
    public String demanderTitreAlbum() { afficher("Titre de l'album :"); return lireLigne(); }
    @Override
    public int demanderAnneeAlbum() { afficher("Année de sortie :"); return lireEntier(); }
    @Override
    public int demanderIdArtisteAlbum() { afficher("ID de l'artiste (0 si groupe) :"); return lireEntier(); }
    @Override
    public int demanderIdGroupeAlbum() { afficher("ID du groupe (0 si artiste solo) :"); return lireEntier(); }
    @Override
    public void afficherAlbumAjoute(int id) { afficher("✅ Album ajouté (ID:" + id + ")"); }

    @Override
    public String demanderNomArtiste() { afficher("Nom de l'artiste :"); return lireLigne(); }
    @Override
    public String demanderPrenomArtiste() { afficher("Prénom de l'artiste :"); return lireLigne(); }
    @Override
    public String demanderNationaliteArtiste() { afficher("Nationalité :"); return lireLigne(); }
    @Override
    public void afficherArtisteAjoute(int id) { afficher("✅ Artiste ajouté (ID:" + id + ")"); }

    @Override
    public String demanderNomGroupe() { afficher("Nom du groupe :"); return lireLigne(); }
    @Override
    public int demanderDateCreationGroupe() { afficher("Année de création :"); return lireEntier(); }
    @Override
    public String demanderNationaliteGroupe() { afficher("Nationalité :"); return lireLigne(); }
    @Override
    public void afficherGroupeAjoute(int id) { afficher("✅ Groupe ajouté (ID:" + id + ")"); }

    @Override
    public int demanderIdAlbumAssociation() { afficher("ID de l'album :"); return lireEntier(); }
    @Override
    public int demanderIdMorceauAssociation() { afficher("ID du morceau :"); return lireEntier(); }
    @Override
    public int demanderNumeroPiste() { afficher("Numéro de piste :"); return lireEntier(); }
    @Override
    public void afficherMorceauAjouteDansAlbum(String titreMorceau, String titreAlbum) {
        afficher("✅ \"" + titreMorceau + "\" ajouté dans \"" + titreAlbum + "\"");
    }

    @Override
    public int demanderIdGroupeAssociation() { afficher("ID du groupe :"); return lireEntier(); }
    @Override
    public int demanderIdArtisteAssociation() { afficher("ID de l'artiste :"); return lireEntier(); }
    @Override
    public void afficherMembreAjouteDansGroupe(String nomArtiste, String nomGroupe) {
        afficher("✅ \"" + nomArtiste + "\" ajouté dans \"" + nomGroupe + "\"");
    }

    // ==================== COMPTES ABONNÉS ====================

    @Override
    public void afficherListeAbonnes(List<String[]> abonnes) {
        afficher("\n--- Abonnés ---");
        if (abonnes.isEmpty()) { afficher("Aucun abonné."); return; }
        for (String[] a : abonnes) {
            boolean suspendu = false;
            for (String part : a) {
                if (part.equals("SUSPENDU")) { suspendu = true; break; }
            }
            String statut = suspendu ? " [SUSPENDU]" : " [ACTIF]";
            afficher("  [" + a[0] + "] " + a[1] + " " + a[2] + " - " + a[4] + statut);
        }
    }

    @Override
    public int afficherMenuGestionComptes() {
        return afficherMenuBoutons("Gestion comptes",
                "Supprimer un compte abonné",
                "Suspendre un compte abonné",
                "Réactiver un compte abonné",
                "Retour");
    }

    @Override
    public int demanderIdAbonne() { afficher("ID de l'abonné :"); return lireEntier(); }
    @Override
    public void afficherAbonneSupprime() { afficher("✅ Compte supprimé."); }
    @Override
    public void afficherAbonneNonTrouve() { afficher("❌ Abonné introuvable."); }
    @Override
    public void afficherAbonneSuspendu() { afficher("✅ Compte suspendu."); }
    @Override
    public void afficherAbonneReactive() { afficher("✅ Compte réactivé."); }
    @Override
    public void afficherAbonneNonTrouveOuDejaEtat(String etat) {
        afficher("❌ Abonné introuvable ou déjà " + etat + ".");
    }

    // ==================== STATISTIQUES ====================

    @Override
    public void afficherStatistiques(int nbMorceaux, int nbAlbums, int nbArtistes, int nbGroupes, int nbUtilisateurs, int nbEcoutes) {
        afficher("\n--- Statistiques ---");
        afficher("  Morceaux      : " + nbMorceaux);
        afficher("  Albums        : " + nbAlbums);
        afficher("  Artistes      : " + nbArtistes);
        afficher("  Groupes       : " + nbGroupes);
        afficher("  Utilisateurs  : " + nbUtilisateurs);
        afficher("  Écoutes total : " + nbEcoutes);
    }

    // ==================== ÉCOUTE ====================

    @Override
    public String demanderRechercheMusique() { afficher("\nRechercher (titre, artiste, genre) :"); return lireLigne(); }

    @Override
    public void afficherResultatsEcoute(List<Morceau> resultats) {
        if (resultats.isEmpty()) { afficher("Aucun morceau trouvé."); return; }
        afficher("\nRésultats :");
        for (Morceau m : resultats) afficher("  " + m);
    }

    @Override
    public int demanderIdMorceauEcoute() { afficher("ID du morceau à écouter :"); return lireEntier(); }

    @Override
    public int afficherMenuApresEchecRecherche() {
        return afficherMenuBoutons("Aucun résultat", "Rechercher autre chose", "Retour");
    }

    @Override
    public int afficherMenuApresEcoute(int ecoutesRestantes) {
        if (ecoutesRestantes >= 0) afficher("Écoutes restantes : " + ecoutesRestantes);
        return afficherMenuBoutons("Après écoute", "Écouter un autre morceau", "Retour");
    }

    @Override
    public void afficherLimiteEcoutesAtteinte() {
        afficher("\n⚠ Limite d'écoutes atteinte. Créez un compte pour continuer !");
    }

    /**
     * Simule la lecture d'un morceau avec une barre de progression animée.
     * La durée de simulation est proportionnelle à la durée réelle (entre 2 et 5 secondes).
     * Utilise invokeAndWait pour créer et afficher le dialogue sur l'EDT.
     */
    @Override
    public void afficherEcoute(Morceau m) {
        afficher("\n  ♪ Lecture : " + m.getTitre() + " — " + m.getNomInterprete()
                + "  (" + m.getDureeFormatee() + ")");

        // Durée simulée proportionnelle à la durée réelle, plafonnée entre 2s et 5s
        int dureeReelle = m.getDuree();
        int dureeSimuleeMs = Math.max(2000, Math.min(5000, dureeReelle * 50));
        int delaiParTick = Math.max(20, dureeSimuleeMs / 100);

        try {
            SwingUtilities.invokeAndWait(() -> {
                JDialog dialog = new JDialog(fenetre, "♪ Lecture en cours", true);
                dialog.setLayout(new BorderLayout(10, 10));
                dialog.setSize(420, 130);
                dialog.setLocationRelativeTo(fenetre);
                dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                dialog.getContentPane().setBackground(new Color(40, 40, 40));

                // Titre et interprète du morceau
                JLabel labelInfo = new JLabel(
                        "  " + m.getTitre() + "  —  " + m.getNomInterprete(), SwingConstants.CENTER);
                labelInfo.setForeground(new Color(200, 230, 200));
                labelInfo.setFont(new Font("Monospaced", Font.BOLD, 12));
                dialog.add(labelInfo, BorderLayout.NORTH);

                // Barre de progression
                JProgressBar barre = new JProgressBar(0, 100);
                barre.setStringPainted(true);
                barre.setForeground(new Color(100, 200, 100));
                barre.setBackground(new Color(50, 50, 50));
                barre.setFont(new Font("Monospaced", Font.PLAIN, 11));
                JPanel panneauBarre = new JPanel(new BorderLayout());
                panneauBarre.setBackground(new Color(40, 40, 40));
                panneauBarre.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 15));
                panneauBarre.add(barre, BorderLayout.CENTER);
                dialog.add(panneauBarre, BorderLayout.CENTER);

                // Timer : 100 ticks, chacun espacé de delaiParTick millisecondes
                final int[] avancement = {0};
                Timer timer = new Timer(delaiParTick, null);
                timer.addActionListener(e -> {
                    avancement[0]++;
                    barre.setValue(avancement[0]);
                    if (avancement[0] >= 100) {
                        timer.stop();
                        dialog.dispose();
                    }
                });
                timer.start();
                dialog.setVisible(true); // boucle imbriquée sur l'EDT jusqu'à dispose()
            });
        } catch (InterruptedException | java.lang.reflect.InvocationTargetException ignored) {}
    }

    // ==================== NOTES ====================

    @Override
    public void afficherNoteMorceau(double moyenne, int nbVotes) {
        if (nbVotes == 0) {
            afficher("  Note : Aucune note pour l'instant.");
        } else {
            StringBuilder etoiles = new StringBuilder();
            int pleines = (int) Math.round(moyenne);
            for (int i = 1; i <= 5; i++) etoiles.append(i <= pleines ? "★" : "☆");
            afficher(String.format("  Note : %s  %.1f/5 (%d vote%s)",
                    etoiles, moyenne, nbVotes, nbVotes > 1 ? "s" : ""));
        }
    }

    @Override
    public int proposerNotation(int noteActuelle) {
        if (noteActuelle > 0) {
            return afficherMenuBoutons("Notation",
                    "Modifier ma note (" + noteActuelle + "/5)", "Passer");
        } else {
            return afficherMenuBoutons("Notation", "Noter ce morceau (1 à 5)", "Passer");
        }
    }

    @Override
    public int demanderNote() {
        afficher("Votre note (1 à 5) :");
        while (true) {
            int note = lireEntier();
            if (note >= 1 && note <= 5) return note;
            afficher("⚠ Note invalide, entrez un chiffre entre 1 et 5.");
        }
    }

    @Override
    public void afficherNoteEnregistree(int note) {
        StringBuilder etoiles = new StringBuilder();
        for (int i = 1; i <= 5; i++) etoiles.append(i <= note ? "★" : "☆");
        afficher("  Note enregistrée : " + etoiles + " (" + note + "/5)");
    }

    // ==================== PLAYLISTS ====================

    @Override
    public int afficherMenuPlaylist() {
        return afficherMenuBoutons("Playlists",
                "Créer une playlist", "Voir mes playlists",
                "Ajouter un morceau", "Retirer un morceau",
                "Écouter une playlist", "Renommer une playlist",
                "Supprimer une playlist", "Retour");
    }

    @Override
    public String demanderNomPlaylist() { afficher("Nom de la playlist :"); return lireLigne(); }
    @Override
    public String demanderNouveauNomPlaylist() { afficher("Nouveau nom :"); return lireLigne(); }
    @Override
    public int demanderIdPlaylist() { afficher("ID de la playlist :"); return lireEntier(); }
    @Override
    public int demanderIdMorceau() { afficher("ID du morceau :"); return lireEntier(); }

    @Override
    public void afficherPlaylistCreee(int id, String nom) { afficher("✅ Playlist \"" + nom + "\" créée (ID:" + id + ")"); }
    @Override
    public void afficherPlaylistRenommee(String ancienNom, String nouveauNom) { afficher("✅ \"" + ancienNom + "\" → \"" + nouveauNom + "\""); }

    @Override
    public void afficherListePlaylists(List<Playlist> playlists) {
        if (playlists.isEmpty()) { afficher("Aucune playlist."); return; }
        afficher("\n--- Vos playlists ---");
        for (Playlist p : playlists) afficher("  " + p);
    }

    @Override
    public void afficherContenuPlaylist(Playlist playlist) {
        afficher("\n--- " + playlist.getNom() + " ---");
        List<Morceau> morceaux = playlist.getMorceaux();
        if (morceaux.isEmpty()) { afficher("  (vide)"); return; }
        int n = 1;
        for (Morceau m : morceaux)
            afficher("  " + n++ + ". " + m.getTitre() + " - " + m.getNomInterprete() + " (" + m.getDureeFormatee() + ")");
        afficher("  Durée totale : " + playlist.getDureeTotaleFormatee());
    }

    @Override
    public void afficherMorceauAjoutePlaylist(String titre, String nomPlaylist) { afficher("✅ \"" + titre + "\" ajouté à \"" + nomPlaylist + "\""); }
    @Override
    public void afficherMorceauDejaPresent() { afficher("⚠ Déjà dans la playlist."); }
    @Override
    public void afficherMorceauRetire() { afficher("✅ Morceau retiré."); }
    @Override
    public void afficherPlaylistSupprimee(String nom) { afficher("✅ Playlist \"" + nom + "\" supprimée."); }
    @Override
    public void afficherPlaylistIntrouvable() { afficher("❌ Playlist introuvable."); }
    @Override
    public void afficherLecturePlaylist(String nom) { afficher("\n▶ Lecture : " + nom); }
    @Override
    public void afficherFinPlaylist(String nom) { afficher("⏹ Fin : " + nom); }
    @Override
    public void afficherPlaylistVide() { afficher("⚠ Playlist vide."); }

    @Override
    public void afficherPochette(int position, int total, Morceau m) {
        afficher("\n  ♪ " + position + "/" + total + " — " + m.getTitre() + " - " + m.getNomInterprete()
                + " | " + m.getGenre() + " | " + m.getDureeFormatee());
    }

    /**
     * Affiche les contrôles du lecteur de playlist.
     * Construit dynamiquement la liste des boutons visibles et retourne le code
     * correspondant attendu par le contrôleur : 1=Précédent, 2=Suivant, 3=Arrêter.
     */
    @Override
    public int afficherControlesLecteur(boolean peutReculer, boolean peutAvancer) {
        List<String> opts  = new ArrayList<>();
        List<Integer> codes = new ArrayList<>();

        if (peutReculer) { opts.add("◀ Précédent"); codes.add(1); }
        if (peutAvancer) { opts.add("▶ Suivant");   codes.add(2); }
        opts.add("⏹ Arrêter");                       codes.add(3);

        int idx = afficherMenuBoutons("Lecteur", opts.toArray(new String[0]));
        return codes.get(idx - 1);
    }

    // ==================== HISTORIQUE ====================

    @Override
    public void afficherHistorique(List<Historique> historique) {
        afficher("\n========== HISTORIQUE ==========");
        if (historique.isEmpty()) { afficher("  (aucun)"); }
        else { int n = 1; for (Historique h : historique) afficher("  " + n++ + ". " + h); }
        afficher("=================================");
    }

    // ==================== DIVERS ====================

    @Override
    public void afficherChoixInvalide() { afficher("⚠ Choix invalide."); }
    @Override
    public void afficherRetourMenuPrincipal() { afficher("↩ Retour au menu principal."); }
    @Override
    public void afficherErreurId() { afficher("❌ ID invalide ou introuvable."); }
    @Override
    public void afficherMessage(String message) { afficher(message); }
}
