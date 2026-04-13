package view;

import model.*;
import java.util.List;
import java.util.Scanner;

/**
 * Vue console : gère tout l'affichage et la saisie utilisateur.
 * Ne contient aucune logique métier.
 */
public class VueConsole {

    private Scanner clavier = new Scanner(System.in);

    // ==================== UTILITAIRE SAISIE ====================

    /**
     * Lit un entier de manière sécurisée (gère les erreurs de saisie).
     */
    private int lireEntier() {
        while (true) {
            try {
                int val = Integer.parseInt(clavier.nextLine().trim());
                return val;
            } catch (NumberFormatException e) {
                System.out.print("Saisie invalide, entrez un nombre : ");
            }
        }
    }

    // ==================== MENU PRINCIPAL ====================

    public void afficherBienvenue() {
        System.out.println("\n====================================");
        System.out.println("     BIENVENUE SUR JAVAZIK");
        System.out.println("====================================");
    }

    public int afficherMenuPrincipal() {
        System.out.println("\n--- MENU PRINCIPAL ---");
        System.out.println("1. Se connecter en tant qu'administrateur");
        System.out.println("2. Se connecter en tant que client");
        System.out.println("3. Creer un compte client");
        System.out.println("4. Continuer en tant que simple visiteur");
        System.out.println("5. Quitter");
        System.out.print("Votre choix : ");
        int choix = lireEntier();
        while (choix < 1 || choix > 5) {
            System.out.print("Choix invalide (1-5) : ");
            choix = lireEntier();
        }
        return choix;
    }

    // ==================== MENU ADMIN ====================

    public int afficherMenuAdmin() {
        System.out.println("\n--- MENU ADMINISTRATEUR ---");
        System.out.println("1. Ajouter un morceau");
        System.out.println("2. Supprimer un morceau");
        System.out.println("3. Ajouter un album");
        System.out.println("4. Supprimer un album");
        System.out.println("5. Ajouter un artiste");
        System.out.println("6. Supprimer un artiste");
        System.out.println("7. Ajouter un groupe");
        System.out.println("8. Supprimer un groupe");
        System.out.println("9. Gerer les comptes abonnes");
        System.out.println("10. Consulter les statistiques");
        System.out.println("11. Retour au menu principal");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    // ==================== MENU CLIENT ====================

    public int afficherMenuClient() {
        System.out.println("\n--- MENU CLIENT ---");
        System.out.println("1. Consulter le catalogue");
        System.out.println("2. Creer et gerer une playlist");
        System.out.println("3. Ecouter un morceau");
        System.out.println("4. Consulter l'historique d'ecoute");
        System.out.println("5. Revenir au menu principal");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    // ==================== MENU VISITEUR ====================

    public int afficherMenuVisiteur() {
        System.out.println("\n--- MENU VISITEUR ---");
        System.out.println("1. Consulter le catalogue");
        System.out.println("2. Ecouter un morceau (5 ecoutes max)");
        System.out.println("3. Retour au menu principal");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    // ==================== CATALOGUE : MENU NAVIGATION ====================

    public int afficherMenuCatalogue() {
        System.out.println("\n--- CATALOGUE MUSICAL ---");
        System.out.println("1. Rechercher (morceau, album, artiste, groupe)");
        System.out.println("2. Voir tous les morceaux");
        System.out.println("3. Voir tous les albums");
        System.out.println("4. Voir tous les artistes");
        System.out.println("5. Voir tous les groupes");
        System.out.println("6. Parcourir par genre");
        System.out.println("7. Retour");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    public String demanderRecherche() {
        System.out.print("\nRecherche : ");
        return clavier.nextLine();
    }

    // ==================== AFFICHAGE RESULTATS RECHERCHE ====================

    public void afficherResultatsRecherche(Catalogue.ResultatRecherche r) {
        if (r.estVide()) {
            System.out.println("Aucun resultat trouve.");
            return;
        }
        System.out.println("\n" + r.getNombreTotal() + " resultat(s) trouve(s) :");

        if (!r.morceaux.isEmpty()) {
            System.out.println("\n  -- Morceaux --");
            for (Morceau m : r.morceaux) {
                System.out.println("  " + m);
            }
        }
        if (!r.albums.isEmpty()) {
            System.out.println("\n  -- Albums --");
            for (Album a : r.albums) {
                System.out.println("  " + a);
            }
        }
        if (!r.artistes.isEmpty()) {
            System.out.println("\n  -- Artistes --");
            for (Artiste a : r.artistes) {
                System.out.println("  " + a);
            }
        }
        if (!r.groupes.isEmpty()) {
            System.out.println("\n  -- Groupes --");
            for (Groupe g : r.groupes) {
                System.out.println("  " + g);
            }
        }
    }

    // ==================== NAVIGATION DÉTAILLÉE ====================

    /**
     * Menu de navigation après affichage des résultats.
     */
    public int afficherMenuNavigation() {
        System.out.println("\n--- NAVIGATION ---");
        System.out.println("1. Voir les details d'un morceau (par ID)");
        System.out.println("2. Voir les details d'un album (par ID)");
        System.out.println("3. Voir les details d'un artiste (par ID)");
        System.out.println("4. Voir les details d'un groupe (par ID)");
        System.out.println("5. Retour");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    public int demanderIdElement() {
        System.out.print("ID : ");
        return lireEntier();
    }

    // --- Détails morceau ---
    public void afficherDetailsMorceau(Morceau m) {
        System.out.println("\n====== MORCEAU ======");
        System.out.println("  ID        : " + m.getId());
        System.out.println("  Titre     : " + m.getTitre());
        System.out.println("  Interprete: " + m.getNomInterprete());
        System.out.println("  Genre     : " + m.getGenre());
        System.out.println("  Annee     : " + m.getAnnee());
        System.out.println("  Duree     : " + m.getDureeFormatee());
    }

    public void afficherAlbumsDuMorceau(List<Album> albums) {
        if (albums.isEmpty()) {
            System.out.println("  Albums    : (aucun)");
        } else {
            System.out.println("  Present dans :");
            for (Album a : albums) {
                System.out.println("    - " + a.getTitre() + " (" + a.getAnnee() + ") [ID:" + a.getId() + "]");
            }
        }
    }

    public void afficherAutresMorceauxInterprete(List<Morceau> autres) {
        if (!autres.isEmpty()) {
            System.out.println("  Autres morceaux du meme interprete :");
            for (Morceau m : autres) {
                System.out.println("    - " + m.getTitre() + " (" + m.getAnnee() + ") [ID:" + m.getId() + "]");
            }
        }
    }

    // --- Détails album ---
    public void afficherDetailsAlbum(Album a) {
        System.out.println("\n====== ALBUM ======");
        System.out.println("  ID        : " + a.getId());
        System.out.println("  Titre     : " + a.getTitre());
        System.out.println("  Interprete: " + a.getNomInterprete());
        System.out.println("  Annee     : " + a.getAnnee());
        System.out.println("  Duree     : " + a.getDureeTotaleFormatee());
        System.out.println("  Pistes    :");
        int num = 1;
        for (Morceau m : a.getMorceaux()) {
            System.out.println("    " + num + ". " + m.getTitre() + " (" + m.getDureeFormatee() + ") [ID:" + m.getId() + "]");
            num++;
        }
    }

    public void afficherAutresAlbumsInterprete(List<Album> autres) {
        if (!autres.isEmpty()) {
            System.out.println("  Autres albums du meme interprete :");
            for (Album a : autres) {
                System.out.println("    - " + a.getTitre() + " (" + a.getAnnee() + ") [ID:" + a.getId() + "]");
            }
        }
    }

    // --- Détails artiste ---
    public void afficherDetailsArtiste(Artiste a) {
        System.out.println("\n====== ARTISTE ======");
        System.out.println("  ID          : " + a.getId());
        System.out.println("  Nom         : " + a.getNomComplet());
        System.out.println("  Nationalite : " + a.getNationalite());
    }

    public void afficherGroupesDeLArtiste(List<Groupe> groupes) {
        if (!groupes.isEmpty()) {
            System.out.println("  Membre de :");
            for (Groupe g : groupes) {
                System.out.println("    - " + g.getNom() + " [ID:" + g.getId() + "]");
            }
        }
    }

    public void afficherMorceauxArtiste(List<Morceau> morceaux) {
        if (!morceaux.isEmpty()) {
            System.out.println("  Morceaux :");
            for (Morceau m : morceaux) {
                System.out.println("    - " + m.getTitre() + " (" + m.getAnnee() + ") [ID:" + m.getId() + "]");
            }
        }
    }

    public void afficherAlbumsArtiste(List<Album> albums) {
        if (!albums.isEmpty()) {
            System.out.println("  Albums :");
            for (Album a : albums) {
                System.out.println("    - " + a.getTitre() + " (" + a.getAnnee() + ") [ID:" + a.getId() + "]");
            }
        }
    }

    // --- Détails groupe ---
    public void afficherDetailsGroupe(Groupe g) {
        System.out.println("\n====== GROUPE ======");
        System.out.println("  ID          : " + g.getId());
        System.out.println("  Nom         : " + g.getNom());
        System.out.println("  Cree en     : " + g.getDateCreation());
        System.out.println("  Nationalite : " + g.getNationalite());
        if (!g.getMembres().isEmpty()) {
            System.out.println("  Membres :");
            for (Artiste a : g.getMembres()) {
                System.out.println("    - " + a.getNomComplet() + " [ID:" + a.getId() + "]");
            }
        }
    }

    public void afficherMorceauxGroupe(List<Morceau> morceaux) {
        if (!morceaux.isEmpty()) {
            System.out.println("  Morceaux :");
            for (Morceau m : morceaux) {
                System.out.println("    - " + m.getTitre() + " (" + m.getAnnee() + ") [ID:" + m.getId() + "]");
            }
        }
    }

    public void afficherAlbumsGroupe(List<Album> albums) {
        if (!albums.isEmpty()) {
            System.out.println("  Albums :");
            for (Album a : albums) {
                System.out.println("    - " + a.getTitre() + " (" + a.getAnnee() + ") [ID:" + a.getId() + "]");
            }
        }
    }

    // --- Listes complètes ---
    public void afficherListeMorceaux(List<Morceau> morceaux) {
        System.out.println("\n--- TOUS LES MORCEAUX (" + morceaux.size() + ") ---");
        for (Morceau m : morceaux) {
            System.out.println("  " + m);
        }
    }

    public void afficherListeAlbums(List<Album> albums) {
        System.out.println("\n--- TOUS LES ALBUMS (" + albums.size() + ") ---");
        for (Album a : albums) {
            System.out.println("  " + a);
        }
    }

    public void afficherListeArtistes(List<Artiste> artistes) {
        System.out.println("\n--- TOUS LES ARTISTES (" + artistes.size() + ") ---");
        for (Artiste a : artistes) {
            System.out.println("  " + a);
        }
    }

    public void afficherListeGroupes(List<Groupe> groupes) {
        System.out.println("\n--- TOUS LES GROUPES (" + groupes.size() + ") ---");
        for (Groupe g : groupes) {
            System.out.println("  " + g);
        }
    }

    // --- Genre ---
    public void afficherGenresDisponibles(List<String> genres) {
        System.out.println("\n--- GENRES DISPONIBLES ---");
        for (int i = 0; i < genres.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + genres.get(i));
        }
        System.out.print("Choisissez un genre (numero) : ");
    }

    // ==================== CONNEXION ====================

    public void afficherConnexionAdmin() {
        System.out.println("\n--- Connexion administrateur ---");
    }

    public String demanderMail() {
        System.out.print("Votre mail : ");
        return clavier.nextLine();
    }

    public String demanderMdp() {
        System.out.print("Votre mot de passe : ");
        return clavier.nextLine();
    }

    public void afficherMdpIncorrect() {
        System.out.println("Mot de passe incorrect !");
    }

    public void afficherMailIncorrect() {
        System.out.println("Mail incorrect !");
    }

    public void afficherPasAdmin() {
        System.out.println("Ce compte n'est pas administrateur.");
    }

    public void afficherConnexionReussie() {
        System.out.println("Connexion reussie !");
    }

    // ==================== INSCRIPTION ====================

    public String demanderNom() {
        System.out.print("Nom : ");
        return clavier.nextLine();
    }

    public String demanderPrenom() {
        System.out.print("Prenom : ");
        return clavier.nextLine();
    }

    public String demanderEmail() {
        System.out.print("Email : ");
        return clavier.nextLine();
    }

    public String demanderMotDePasse() {
        System.out.print("Mot de passe : ");
        return clavier.nextLine();
    }

    public void afficherInscriptionReussie() {
        System.out.println("Inscription reussie !");
    }

    // ==================== ADMIN : MORCEAUX ====================

    public String demanderTitreMorceau() {
        System.out.print("Titre du morceau : ");
        return clavier.nextLine();
    }

    public int demanderDureeMorceau() {
        System.out.print("Duree (en secondes) : ");
        return lireEntier();
    }

    public String demanderGenreMorceau() {
        System.out.print("Genre : ");
        return clavier.nextLine();
    }

    public int demanderAnneeMorceau() {
        System.out.print("Annee : ");
        return lireEntier();
    }

    public int demanderIdArtisteMorceau() {
        System.out.print("ID de l'artiste (0 si c'est un groupe) : ");
        return lireEntier();
    }

    public int demanderIdGroupeMorceau() {
        System.out.print("ID du groupe (0 si c'est un artiste solo) : ");
        return lireEntier();
    }

    public void afficherMorceauAjoute(int id) {
        System.out.println("Morceau ajoute avec succes (ID : " + id + ")");
    }

    public int demanderIdSuppression() {
        System.out.print("ID de l'element a supprimer : ");
        return lireEntier();
    }

    public void afficherElementSupprime(String type) {
        System.out.println(type + " supprime(e) avec succes !");
    }

    public void afficherElementNonTrouve(String type, int id) {
        System.out.println("Aucun(e) " + type + " trouve(e) avec l'ID " + id);
    }

    // ==================== ADMIN : ALBUMS ====================

    public String demanderTitreAlbum() {
        System.out.print("Titre de l'album : ");
        return clavier.nextLine();
    }

    public int demanderAnneeAlbum() {
        System.out.print("Annee de sortie : ");
        return lireEntier();
    }

    public int demanderIdArtisteAlbum() {
        System.out.print("ID de l'artiste (0 si c'est un groupe) : ");
        return lireEntier();
    }

    public int demanderIdGroupeAlbum() {
        System.out.print("ID du groupe (0 si c'est un artiste solo) : ");
        return lireEntier();
    }

    public void afficherAlbumAjoute(int id) {
        System.out.println("Album ajoute avec succes (ID : " + id + ")");
    }

    // ==================== ADMIN : ARTISTES ====================

    public String demanderNomArtiste() {
        System.out.print("Nom de l'artiste : ");
        return clavier.nextLine();
    }

    public String demanderPrenomArtiste() {
        System.out.print("Prenom de l'artiste : ");
        return clavier.nextLine();
    }

    public String demanderNationaliteArtiste() {
        System.out.print("Nationalite : ");
        return clavier.nextLine();
    }

    public void afficherArtisteAjoute(int id) {
        System.out.println("Artiste ajoute avec succes (ID : " + id + ")");
    }

    // ==================== ADMIN : GROUPES ====================

    public String demanderNomGroupe() {
        System.out.print("Nom du groupe : ");
        return clavier.nextLine();
    }

    public int demanderDateCreationGroupe() {
        System.out.print("Annee de creation : ");
        return lireEntier();
    }

    public String demanderNationaliteGroupe() {
        System.out.print("Nationalite : ");
        return clavier.nextLine();
    }

    public void afficherGroupeAjoute(int id) {
        System.out.println("Groupe ajoute avec succes (ID : " + id + ")");
    }

    // ==================== ADMIN : GESTION COMPTES ====================

    public void afficherListeAbonnes(List<String[]> abonnes) {
        System.out.println("\n--- COMPTES ABONNES ---");
        if (abonnes.isEmpty()) {
            System.out.println("Aucun abonne.");
            return;
        }
        for (String[] a : abonnes) {
            System.out.println("  [" + a[0] + "] " + a[1] + " " + a[2] + " - " + a[4]);
        }
    }

    public int afficherMenuGestionComptes() {
        System.out.println("\n1. Supprimer un compte abonne");
        System.out.println("2. Retour");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    public int demanderIdAbonne() {
        System.out.print("ID de l'abonne : ");
        return lireEntier();
    }

    public void afficherAbonneSupprime() {
        System.out.println("Compte abonne supprime.");
    }

    public void afficherAbonneNonTrouve() {
        System.out.println("Abonne non trouve.");
    }

    // ==================== ADMIN : STATISTIQUES ====================

    public void afficherStatistiques(int nbMorceaux, int nbAlbums, int nbArtistes,
                                      int nbGroupes, int nbUtilisateurs, int nbEcoutes) {
        System.out.println("\n--- STATISTIQUES ---");
        System.out.println("  Morceaux      : " + nbMorceaux);
        System.out.println("  Albums        : " + nbAlbums);
        System.out.println("  Artistes      : " + nbArtistes);
        System.out.println("  Groupes       : " + nbGroupes);
        System.out.println("  Utilisateurs  : " + nbUtilisateurs);
        System.out.println("  Ecoutes total : " + nbEcoutes);
    }

    // ==================== ECOUTE ====================

    public String demanderRechercheMusique() {
        System.out.print("\nRecherchez un morceau (titre, artiste ou genre) : ");
        return clavier.nextLine();
    }

    public void afficherResultatsEcoute(List<Morceau> resultats) {
        if (resultats.isEmpty()) {
            System.out.println("Aucun morceau trouve.");
            return;
        }
        System.out.println("\nResultats :");
        for (Morceau m : resultats) {
            System.out.println("  " + m);
        }
    }

    public int demanderIdMorceauEcoute() {
        System.out.print("ID du morceau a ecouter : ");
        return lireEntier();
    }

    public int afficherMenuApresEchecRecherche() {
        System.out.println("1. Rechercher un autre morceau");
        System.out.println("2. Retour au menu");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    public int afficherMenuApresEcoute(int ecoutesRestantes) {
        System.out.println();
        if (ecoutesRestantes >= 0) {
            System.out.println("Il vous reste " + ecoutesRestantes + " ecoute(s) disponible(s).");
        }
        System.out.println("1. Ecouter un autre morceau");
        System.out.println("2. Retour au menu");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    public void afficherLimiteEcoutesAtteinte() {
        System.out.println("\nVous avez atteint la limite d'ecoutes pour cette session.");
        System.out.println("Creez un compte pour profiter d'ecoutes illimitees !");
    }

    public void afficherEcoute(Morceau m) {
        System.out.println("\n  ♪ Lecture : " + m.getTitre() + " - " + m.getNomInterprete());
        System.out.print("  ");
        int ticks = Math.min(m.getDuree() / 20, 20); // simulation proportionnelle, max 20
        if (ticks < 5) ticks = 5;
        for (int i = 0; i < ticks; i++) {
            System.out.print("▓");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
        System.out.println(" (" + m.getDureeFormatee() + ")");
    }

    // ==================== PLAYLISTS ====================

    public int afficherMenuPlaylist() {
        System.out.println("\n--- GESTION DES PLAYLISTS ---");
        System.out.println("1. Creer une nouvelle playlist");
        System.out.println("2. Voir mes playlists");
        System.out.println("3. Ajouter un morceau a une playlist");
        System.out.println("4. Retirer un morceau d'une playlist");
        System.out.println("5. Ecouter une playlist");
        System.out.println("6. Renommer une playlist");
        System.out.println("7. Supprimer une playlist");
        System.out.println("8. Retour au menu client");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    public String demanderNomPlaylist() {
        System.out.print("Nom de la playlist : ");
        return clavier.nextLine();
    }

    public String demanderNouveauNomPlaylist() {
        System.out.print("Nouveau nom de la playlist : ");
        return clavier.nextLine();
    }

    public int demanderIdPlaylist() {
        System.out.print("ID de la playlist : ");
        return lireEntier();
    }

    public int demanderIdMorceau() {
        System.out.print("ID du morceau : ");
        return lireEntier();
    }

    public void afficherPlaylistCreee(int id, String nom) {
        System.out.println("Playlist \"" + nom + "\" creee avec succes (ID : " + id + ")");
    }

    public void afficherPlaylistRenommee(String ancienNom, String nouveauNom) {
        System.out.println("Playlist \"" + ancienNom + "\" renommee en \"" + nouveauNom + "\"");
    }

    public void afficherListePlaylists(List<Playlist> playlists) {
        if (playlists.isEmpty()) {
            System.out.println("Vous n'avez aucune playlist pour le moment.");
            return;
        }
        System.out.println("\n--- Vos playlists ---");
        for (Playlist p : playlists) {
            System.out.println("  " + p);
        }
    }

    public void afficherContenuPlaylist(Playlist playlist) {
        System.out.println("\n--- Playlist : " + playlist.getNom() + " ---");
        List<Morceau> morceaux = playlist.getMorceaux();
        if (morceaux.isEmpty()) {
            System.out.println("  (aucun morceau dans cette playlist)");
        } else {
            int num = 1;
            for (Morceau m : morceaux) {
                System.out.println("  " + num + ". " + m.getTitre() + " - " + m.getNomInterprete()
                                   + " (" + m.getDureeFormatee() + ") [ID:" + m.getId() + "]");
                num++;
            }
            System.out.println("  Duree totale : " + playlist.getDureeTotaleFormatee());
        }
    }

    public void afficherMorceauAjoutePlaylist(String titre, String nomPlaylist) {
        System.out.println("\"" + titre + "\" ajoute a la playlist \"" + nomPlaylist + "\"");
    }

    public void afficherMorceauDejaPresent() {
        System.out.println("Ce morceau est deja dans la playlist.");
    }

    public void afficherMorceauRetire() {
        System.out.println("Morceau retire de la playlist.");
    }

    public void afficherPlaylistSupprimee(String nom) {
        System.out.println("Playlist \"" + nom + "\" supprimee.");
    }

    public void afficherPlaylistIntrouvable() {
        System.out.println("Playlist introuvable ou vous n'en etes pas le proprietaire.");
    }

    public void afficherLecturePlaylist(String nom) {
        System.out.println("\n>>> Lecture de la playlist : " + nom + " <<<");
    }

    public void afficherFinPlaylist(String nom) {
        System.out.println(">>> Fin de la playlist : " + nom + " <<<");
    }

    public void afficherPochette(int position, int total, Morceau m) {
        System.out.println("\n  ♪ Morceau " + position + "/" + total);
        System.out.println("  Titre     : " + m.getTitre());
        System.out.println("  Interprete: " + m.getNomInterprete());
        System.out.println("  Genre     : " + m.getGenre());
        System.out.println("  Duree     : " + m.getDureeFormatee());
    }

    public int afficherControlesLecteur(boolean peutReculer, boolean peutAvancer) {
        System.out.println();
        if (peutReculer)  System.out.println("1. Morceau precedent");
        if (peutAvancer)  System.out.println("2. Morceau suivant");
        System.out.println("3. Arreter la playlist");
        System.out.print("Votre choix : ");
        int choix = lireEntier();
        if (choix == 1 && !peutReculer) return 2;
        if (choix == 2 && !peutAvancer) return 3;
        return choix;
    }

    public void afficherPlaylistVide() {
        System.out.println("Cette playlist est vide, ajoutez des morceaux d'abord !");
    }

    // ==================== HISTORIQUE ====================

    public void afficherHistorique(List<Historique> historique) {
        System.out.println("\n========== HISTORIQUE D'ECOUTE ==========");
        if (historique.isEmpty()) {
            System.out.println("Vous n'avez encore ecoute aucun morceau.");
        } else {
            int num = 1;
            for (Historique h : historique) {
                System.out.println("  " + num + ". " + h);
                num++;
            }
        }
        System.out.println("=========================================");
    }

    // ==================== DIVERS ====================

    public void afficherChoixInvalide() {
        System.out.println("Choix invalide !");
    }

    public void afficherRetourMenuPrincipal() {
        System.out.println("Retour au menu principal.");
    }

    public void afficherErreurId() {
        System.out.println("ID invalide ou introuvable.");
    }

    public void afficherMessage(String message) {
        System.out.println(message);
    }
}
