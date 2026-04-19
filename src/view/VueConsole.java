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

    /**
     * Affiche le message de bienvenue de l'application au démarrage.
     */
    public void afficherBienvenue() {
        System.out.println("\n====================================");
        System.out.println("     BIENVENUE SUR JAVAZIK");
        System.out.println("====================================");
    }

    /**
     * Affiche le menu principal et retourne le choix de l'utilisateur.
     *
     * @return un entier entre 1 et 5 représentant l'option choisie :
     *         1 = connexion admin, 2 = connexion client, 3 = créer un compte,
     *         4 = visiteur, 5 = quitter
     */
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

    /**
     * Affiche le menu administrateur et retourne le choix de l'utilisateur.
     *
     * @return un entier entre 1 et 13 représentant l'action choisie
     */
    public int afficherMenuAdmin() {
        System.out.println("\n--- MENU ADMINISTRATEUR ---");
        System.out.println("1. Ajouter un morceau");
        System.out.println("2. Supprimer un morceau");
        System.out.println("3. Ajouter un album");
        System.out.println("4. Supprimer un album");
        System.out.println("5. Ajouter un morceau dans un album");
        System.out.println("6. Ajouter un artiste");
        System.out.println("7. Supprimer un artiste");
        System.out.println("8. Ajouter un groupe");
        System.out.println("9. Supprimer un groupe");
        System.out.println("10. Ajouter un membre a un groupe");
        System.out.println("11. Gerer les comptes abonnes");
        System.out.println("12. Consulter les statistiques");
        System.out.println("13. Retour au menu principal");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    // ==================== MENU CLIENT ====================

    /**
     * Affiche le menu client et retourne le choix de l'utilisateur.
     *
     * @return un entier entre 1 et 5 représentant l'action choisie
     */
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

    /**
     * Affiche le menu visiteur et retourne le choix de l'utilisateur.
     *
     * @return un entier entre 1 et 3 représentant l'action choisie
     */
    public int afficherMenuVisiteur() {
        System.out.println("\n--- MENU VISITEUR ---");
        System.out.println("1. Consulter le catalogue");
        System.out.println("2. Ecouter un morceau (5 ecoutes max)");
        System.out.println("3. Retour au menu principal");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    // ==================== CATALOGUE : MENU NAVIGATION ====================

    /**
     * Affiche le menu de navigation du catalogue musical et retourne le choix.
     *
     * @return un entier entre 1 et 7 représentant l'option de navigation choisie
     */
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

    /**
     * Demande à l'utilisateur de saisir un terme de recherche.
     *
     * @return la chaîne saisie par l'utilisateur
     */
    public String demanderRecherche() {
        System.out.print("\nRecherche : ");
        return clavier.nextLine();
    }

    // ==================== AFFICHAGE RESULTATS RECHERCHE ====================

    /**
     * Affiche les résultats d'une recherche dans le catalogue.
     * Affiche séparément les morceaux, albums, artistes et groupes trouvés.
     * Si aucun résultat n'est trouvé, affiche un message d'information.
     *
     * @param r le résultat de recherche à afficher
     */
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

    /**
     * Demande à l'utilisateur de saisir un identifiant numérique.
     *
     * @return l'identifiant saisi
     */
    public int demanderIdElement() {
        System.out.print("ID : ");
        return lireEntier();
    }

    /**
     * Affiche les détails complets d'un morceau (ID, titre, interprète, genre, année, durée).
     *
     * @param m le morceau à afficher
     */
    public void afficherDetailsMorceau(Morceau m) {
        System.out.println("\n====== MORCEAU ======");
        System.out.println("  ID        : " + m.getId());
        System.out.println("  Titre     : " + m.getTitre());
        System.out.println("  Interprete: " + m.getNomInterprete());
        System.out.println("  Genre     : " + m.getGenre());
        System.out.println("  Annee     : " + m.getAnnee());
        System.out.println("  Duree     : " + m.getDureeFormatee());
    }

    /**
     * Affiche la liste des albums contenant un morceau donné.
     *
     * @param albums la liste des albums dans lesquels figure le morceau
     */
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

    /**
     * Affiche d'autres morceaux du même interprète que celui consulté.
     *
     * @param autres la liste des autres morceaux de l'interprète
     */
    public void afficherAutresMorceauxInterprete(List<Morceau> autres) {
        if (!autres.isEmpty()) {
            System.out.println("  Autres morceaux du meme interprete :");
            for (Morceau m : autres) {
                System.out.println("    - " + m.getTitre() + " (" + m.getAnnee() + ") [ID:" + m.getId() + "]");
            }
        }
    }

    /**
     * Affiche les détails d'un album : ID, titre, interprète, année, durée totale et pistes.
     *
     * @param a l'album à afficher
     */
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

    /**
     * Affiche d'autres albums du même interprète que celui consulté.
     *
     * @param autres la liste des autres albums de l'interprète
     */
    public void afficherAutresAlbumsInterprete(List<Album> autres) {
        if (!autres.isEmpty()) {
            System.out.println("  Autres albums du meme interprete :");
            for (Album a : autres) {
                System.out.println("    - " + a.getTitre() + " (" + a.getAnnee() + ") [ID:" + a.getId() + "]");
            }
        }
    }

    // --- Détails artiste ---
    /**
     * Affiche les détails d'un artiste : ID, nom complet et nationalité.
     *
     * @param a l'artiste à afficher
     */
    public void afficherDetailsArtiste(Artiste a) {
        System.out.println("\n====== ARTISTE ======");
        System.out.println("  ID          : " + a.getId());
        System.out.println("  Nom         : " + a.getNomComplet());
        System.out.println("  Nationalite : " + a.getNationalite());
    }

    /**
     * Affiche les groupes dont l'artiste est membre.
     *
     * @param groupes la liste des groupes de l'artiste
     */
    public void afficherGroupesDeLArtiste(List<Groupe> groupes) {
        if (!groupes.isEmpty()) {
            System.out.println("  Membre de :");
            for (Groupe g : groupes) {
                System.out.println("    - " + g.getNom() + " [ID:" + g.getId() + "]");
            }
        }
    }

    /**
     * Affiche la discographie (morceaux) d'un artiste.
     *
     * @param morceaux la liste des morceaux de l'artiste
     */
    public void afficherMorceauxArtiste(List<Morceau> morceaux) {
        if (!morceaux.isEmpty()) {
            System.out.println("  Morceaux :");
            for (Morceau m : morceaux) {
                System.out.println("    - " + m.getTitre() + " (" + m.getAnnee() + ") [ID:" + m.getId() + "]");
            }
        }
    }

    /**
     * Affiche les albums d'un artiste.
     *
     * @param albums la liste des albums de l'artiste
     */
    public void afficherAlbumsArtiste(List<Album> albums) {
        if (!albums.isEmpty()) {
            System.out.println("  Albums :");
            for (Album a : albums) {
                System.out.println("    - " + a.getTitre() + " (" + a.getAnnee() + ") [ID:" + a.getId() + "]");
            }
        }
    }

    // --- Détails groupe ---
    /**
     * Affiche les détails d'un groupe : ID, nom, année de création, nationalité et membres.
     *
     * @param g le groupe à afficher
     */
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

    /**
     * Affiche les morceaux produits par un groupe.
     *
     * @param morceaux la liste des morceaux du groupe
     */
    public void afficherMorceauxGroupe(List<Morceau> morceaux) {
        if (!morceaux.isEmpty()) {
            System.out.println("  Morceaux :");
            for (Morceau m : morceaux) {
                System.out.println("    - " + m.getTitre() + " (" + m.getAnnee() + ") [ID:" + m.getId() + "]");
            }
        }
    }

    /**
     * Affiche les albums produits par un groupe.
     *
     * @param albums la liste des albums du groupe
     */
    public void afficherAlbumsGroupe(List<Album> albums) {
        if (!albums.isEmpty()) {
            System.out.println("  Albums :");
            for (Album a : albums) {
                System.out.println("    - " + a.getTitre() + " (" + a.getAnnee() + ") [ID:" + a.getId() + "]");
            }
        }
    }

    // --- Listes complètes ---
    /**
     * Affiche la liste complète des morceaux du catalogue.
     *
     * @param morceaux la liste de tous les morceaux
     */
    public void afficherListeMorceaux(List<Morceau> morceaux) {
        System.out.println("\n--- TOUS LES MORCEAUX (" + morceaux.size() + ") ---");
        for (Morceau m : morceaux) {
            System.out.println("  " + m);
        }
    }

    /**
     * Affiche la liste complète des albums du catalogue.
     *
     * @param albums la liste de tous les albums
     */
    public void afficherListeAlbums(List<Album> albums) {
        System.out.println("\n--- TOUS LES ALBUMS (" + albums.size() + ") ---");
        for (Album a : albums) {
            System.out.println("  " + a);
        }
    }

    /**
     * Affiche la liste complète des artistes du catalogue.
     *
     * @param artistes la liste de tous les artistes
     */
    public void afficherListeArtistes(List<Artiste> artistes) {
        System.out.println("\n--- TOUS LES ARTISTES (" + artistes.size() + ") ---");
        for (Artiste a : artistes) {
            System.out.println("  " + a);
        }
    }

    /**
     * Affiche la liste complète des groupes du catalogue.
     *
     * @param groupes la liste de tous les groupes
     */
    public void afficherListeGroupes(List<Groupe> groupes) {
        System.out.println("\n--- TOUS LES GROUPES (" + groupes.size() + ") ---");
        for (Groupe g : groupes) {
            System.out.println("  " + g);
        }
    }

    // --- Genre ---
    /**
     * Affiche la liste numérotée des genres musicaux disponibles et invite
     * l'utilisateur à en choisir un par son numéro.
     *
     * @param genres la liste ordonnée des genres disponibles
     */
    public void afficherGenresDisponibles(List<String> genres) {
        System.out.println("\n--- GENRES DISPONIBLES ---");
        for (int i = 0; i < genres.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + genres.get(i));
        }
        System.out.print("Choisissez un genre (numero) : ");
    }

    // ==================== CONNEXION ====================

    /** Affiche le bandeau de connexion administrateur. */
    public void afficherConnexionAdmin() {
        System.out.println("\n--- Connexion administrateur ---");
    }

    /** Affiche le bandeau de connexion client. */
    public void afficherConnexionClient() {
        System.out.println("\n--- Connexion client ---");
    }

    /**
     * Demande à l'utilisateur de saisir son adresse mail.
     *
     * @return l'adresse mail saisie
     */
    public String demanderMail() {
        System.out.print("Votre mail : ");
        return clavier.nextLine();
    }

    /**
     * Demande à l'utilisateur de saisir son mot de passe.
     *
     * @return le mot de passe saisi (en clair, côté console)
     */
    public String demanderMdp() {
        System.out.print("Votre mot de passe : ");
        return clavier.nextLine();
    }

    /** Informe l'utilisateur que le mot de passe saisi est incorrect. */
    public void afficherMdpIncorrect() {
        System.out.println("Mot de passe incorrect !");
    }

    /** Informe l'utilisateur que l'adresse mail saisie est introuvable. */
    public void afficherMailIncorrect() {
        System.out.println("Mail incorrect !");
    }

    /** Informe l'utilisateur que le compte trouvé n'a pas les droits administrateur. */
    public void afficherPasAdmin() {
        System.out.println("Ce compte n'est pas administrateur.");
    }

    /** Informe l'utilisateur que le compte trouvé est un compte administrateur
     *  et doit être utilisé via la connexion administrateur. */
    public void afficherPasClient() {
        System.out.println("Ce compte est un compte administrateur. Utilisez la connexion administrateur.");
    }

    /** Confirme que la connexion s'est déroulée avec succès. */
    public void afficherConnexionReussie() {
        System.out.println("Connexion reussie !");
    }

    // ==================== INSCRIPTION ====================

    /**
     * Demande le nom de famille du nouvel utilisateur.
     *
     * @return le nom saisi
     */
    public String demanderNom() {
        System.out.print("Nom : ");
        return clavier.nextLine();
    }

    /**
     * Demande le prénom du nouvel utilisateur.
     *
     * @return le prénom saisi
     */
    public String demanderPrenom() {
        System.out.print("Prenom : ");
        return clavier.nextLine();
    }

    /**
     * Demande l'adresse email du nouvel utilisateur lors de l'inscription.
     *
     * @return l'email saisi
     */
    public String demanderEmail() {
        System.out.print("Email : ");
        return clavier.nextLine();
    }

    /**
     * Demande le mot de passe choisi lors de l'inscription.
     *
     * @return le mot de passe saisi
     */
    public String demanderMotDePasse() {
        System.out.print("Mot de passe : ");
        return clavier.nextLine();
    }

    /** Confirme que l'inscription du nouveau compte a bien été effectuée. */
    public void afficherInscriptionReussie() {
        System.out.println("Inscription reussie !");
    }

    // ==================== ADMIN : MORCEAUX ====================

    /**
     * Demande le titre du nouveau morceau.
     *
     * @return le titre saisi
     */
    public String demanderTitreMorceau() {
        System.out.print("Titre du morceau : ");
        return clavier.nextLine();
    }

    /**
     * Demande la durée du morceau en secondes.
     *
     * @return la durée saisie, en secondes
     */
    public int demanderDureeMorceau() {
        System.out.print("Duree (en secondes) : ");
        return lireEntier();
    }

    /**
     * Demande le genre musical du morceau.
     *
     * @return le genre saisi
     */
    public String demanderGenreMorceau() {
        System.out.print("Genre : ");
        return clavier.nextLine();
    }

    /** @return l'année de sortie du morceau saisie */
    public int demanderAnneeMorceau() {
        System.out.print("Annee : ");
        return lireEntier();
    }

    /** @return l'ID de l'artiste interprète (0 si groupe) */
    public int demanderIdArtisteMorceau() {
        System.out.print("ID de l'artiste (0 si c'est un groupe) : ");
        return lireEntier();
    }

    /** @return l'ID du groupe interprète (0 si artiste solo) */
    public int demanderIdGroupeMorceau() {
        System.out.print("ID du groupe (0 si c'est un artiste solo) : ");
        return lireEntier();
    }

    /** Confirme l'ajout du morceau. @param id l'ID généré */
    public void afficherMorceauAjoute(int id) {
        System.out.println("Morceau ajoute avec succes (ID : " + id + ")");
    }

    /** @return l'ID de l'élément à supprimer */
    public int demanderIdSuppression() {
        System.out.print("ID de l'element a supprimer : ");
        return lireEntier();
    }

    /** Confirme la suppression. @param type type d'élément */
    public void afficherElementSupprime(String type) {
        System.out.println(type + " supprime(e) avec succes !");
    }

    /** Informe qu'aucun élément ne correspond. @param type type @param id identifiant recherché */
    public void afficherElementNonTrouve(String type, int id) {
        System.out.println("Aucun(e) " + type + " trouve(e) avec l'ID " + id);
    }

    // ==================== ADMIN : ALBUMS ====================

    /** @return le titre de l'album saisi */
    public String demanderTitreAlbum() {
        System.out.print("Titre de l'album : ");
        return clavier.nextLine();
    }

    /** @return l'année de sortie de l'album saisie */
    public int demanderAnneeAlbum() {
        System.out.print("Annee de sortie : ");
        return lireEntier();
    }

    /** @return l'ID de l'artiste de l'album (0 si groupe) */
    public int demanderIdArtisteAlbum() {
        System.out.print("ID de l'artiste (0 si c'est un groupe) : ");
        return lireEntier();
    }

    /** @return l'ID du groupe de l'album (0 si artiste) */
    public int demanderIdGroupeAlbum() {
        System.out.print("ID du groupe (0 si c'est un artiste solo) : ");
        return lireEntier();
    }

    /** Confirme l'ajout de l'album. @param id l'ID généré */
    public void afficherAlbumAjoute(int id) {
        System.out.println("Album ajoute avec succes (ID : " + id + ")");
    }

    // ==================== ADMIN : ARTISTES ====================

    /** @return le nom de famille de l'artiste saisi */
    public String demanderNomArtiste() {
        System.out.print("Nom de l'artiste : ");
        return clavier.nextLine();
    }

    /** @return le prénom de l'artiste saisi */
    public String demanderPrenomArtiste() {
        System.out.print("Prenom de l'artiste : ");
        return clavier.nextLine();
    }

    /** @return la nationalité de l'artiste saisie */
    public String demanderNationaliteArtiste() {
        System.out.print("Nationalite : ");
        return clavier.nextLine();
    }

    /** Confirme l'ajout de l'artiste. @param id l'ID généré */
    public void afficherArtisteAjoute(int id) {
        System.out.println("Artiste ajoute avec succes (ID : " + id + ")");
    }

    // ==================== ADMIN : GROUPES ====================

    /** @return le nom du groupe saisi */
    public String demanderNomGroupe() {
        System.out.print("Nom du groupe : ");
        return clavier.nextLine();
    }

    /** @return l'année de création du groupe saisie */
    public int demanderDateCreationGroupe() {
        System.out.print("Annee de creation : ");
        return lireEntier();
    }

    /** @return la nationalité du groupe saisie */
    public String demanderNationaliteGroupe() {
        System.out.print("Nationalite : ");
        return clavier.nextLine();
    }

    /** Confirme l'ajout du groupe. @param id l'ID généré */
    public void afficherGroupeAjoute(int id) {
        System.out.println("Groupe ajoute avec succes (ID : " + id + ")");
    }

    // ==================== ADMIN : ASSOCIATIONS MORCEAU <-> ALBUM ====================

    /** @return l'ID de l'album cible pour l'association */
    public int demanderIdAlbumAssociation() {
        System.out.print("ID de l'album : ");
        return lireEntier();
    }

    /** @return l'ID du morceau à associer à l'album */
    public int demanderIdMorceauAssociation() {
        System.out.print("ID du morceau a ajouter : ");
        return lireEntier();
    }

    /** @return le numéro de piste dans l'album saisi */
    public int demanderNumeroPiste() {
        System.out.print("Numero de piste dans l'album : ");
        return lireEntier();
    }

    /** Confirme l'ajout d'un morceau dans un album.
     *  @param titreMorceau titre du morceau  @param titreAlbum titre de l'album */
    public void afficherMorceauAjouteDansAlbum(String titreMorceau, String titreAlbum) {
        System.out.println("\"" + titreMorceau + "\" ajoute dans l'album \"" + titreAlbum + "\" !");
    }

    // ==================== ADMIN : ASSOCIATIONS ARTISTE <-> GROUPE ====================

    /** @return l'ID du groupe cible pour l'association */
    public int demanderIdGroupeAssociation() {
        System.out.print("ID du groupe : ");
        return lireEntier();
    }

    /** @return l'ID de l'artiste à ajouter au groupe */
    public int demanderIdArtisteAssociation() {
        System.out.print("ID de l'artiste a ajouter : ");
        return lireEntier();
    }

    /** Confirme l'ajout d'un artiste dans un groupe.
     *  @param nomArtiste nom de l'artiste  @param nomGroupe nom du groupe */
    public void afficherMembreAjouteDansGroupe(String nomArtiste, String nomGroupe) {
        System.out.println("\"" + nomArtiste + "\" ajoute comme membre de \"" + nomGroupe + "\" !");
    }

    // ==================== ADMIN : GESTION COMPTES ====================

    /** Affiche la liste des abonnés avec leur statut (ACTIF/SUSPENDU).
     *  @param abonnes liste de tableaux de champs par abonné */
    public void afficherListeAbonnes(List<String[]> abonnes) {
        System.out.println("\n--- COMPTES ABONNES ---");
        if (abonnes.isEmpty()) {
            System.out.println("Aucun abonne.");
            return;
        }
        for (String[] a : abonnes) {
            boolean suspendu = false;
            for (String part : a) {
                if (part.equals("SUSPENDU")) { suspendu = true; break; }
            }
            String statut = suspendu ? " [SUSPENDU]" : " [ACTIF]";
            System.out.println("  [" + a[0] + "] " + a[1] + " " + a[2] + " - " + a[4] + statut);
        }
    }

    /** Affiche le menu de gestion des comptes abonnés et retourne le choix. */
    public int afficherMenuGestionComptes() {
        System.out.println("\n1. Supprimer un compte abonne");
        System.out.println("2. Suspendre un compte abonne");
        System.out.println("3. Reactiver un compte abonne");
        System.out.println("4. Retour");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    /** @return l'ID de l'abonné saisi */
    public int demanderIdAbonne() {
        System.out.print("ID de l'abonne : ");
        return lireEntier();
    }

    /** Confirme la suppression d'un compte abonné. */
    public void afficherAbonneSupprime() {
        System.out.println("Compte abonne supprime.");
    }

    /** Informe que l'abonné recherché est introuvable. */
    public void afficherAbonneNonTrouve() {
        System.out.println("Abonne non trouve.");
    }

    /** Confirme la suspension d'un compte abonné. */
    public void afficherAbonneSuspendu() {
        System.out.println("Compte abonne suspendu avec succes.");
    }

    /** Confirme la réactivation d'un compte abonné. */
    public void afficherAbonneReactive() {
        System.out.println("Compte abonne reactive avec succes.");
    }

    /** Informe que l'abonné est introuvable ou déjà dans l'état demandé.
     *  @param etat l'état concerné (ex. "suspendu") */
    public void afficherAbonneNonTrouveOuDejaEtat(String etat) {
        System.out.println("Abonne non trouve ou compte deja " + etat + ".");
    }

    // ==================== ADMIN : STATISTIQUES ====================

    /** Affiche les statistiques globales du catalogue.
     *  @param nbMorceaux @param nbAlbums @param nbArtistes @param nbGroupes @param nbUtilisateurs @param nbEcoutes */
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

    /** @return le terme de recherche de morceau saisi */
    public String demanderRechercheMusique() {
        System.out.print("\nRecherchez un morceau (titre, artiste ou genre) : ");
        return clavier.nextLine();
    }

    /** Affiche les morceaux trouvés lors d'une recherche d'écoute.
     *  @param resultats la liste des morceaux correspondants */
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

    /** @return l'ID du morceau à écouter saisi */
    public int demanderIdMorceauEcoute() {
        System.out.print("ID du morceau a ecouter : ");
        return lireEntier();
    }

    /** Propose de relancer une recherche ou de retourner au menu. @return 1=nouvelle recherche 2=retour */
    public int afficherMenuApresEchecRecherche() {
        System.out.println("1. Rechercher un autre morceau");
        System.out.println("2. Retour au menu");
        System.out.print("Votre choix : ");
        return lireEntier();
    }

    /** Affiche le menu post-écoute avec le quota restant.
     *  @param ecoutesRestantes écoutes restantes (-1 si illimité) @return 1=réécouter 2=retour */
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

    /** Informe le visiteur qu'il a atteint sa limite d'écoutes de session. */
    public void afficherLimiteEcoutesAtteinte() {
        System.out.println("\nVous avez atteint la limite d'ecoutes pour cette session.");
        System.out.println("Creez un compte pour profiter d'ecoutes illimitees !");
    }

    /** Simule la lecture d'un morceau avec une barre de progression ASCII.
     *  @param m le morceau à jouer */
    public void afficherEcoute(Morceau m) {
        System.out.println("\n  ♪ Lecture : " + m.getTitre() + " - " + m.getNomInterprete());

        int dureeReelle = m.getDuree(); // durée en secondes
        // Simulation proportionnelle : 1s réelle = 1s de chanson, plafonné à 20s max
        int tempsSimule = Math.min(dureeReelle, 10);
        int ticks = tempsSimule; // 1 tick = 1 seconde simulée (100ms réelle)
        if (ticks < 3) ticks = 3;

        System.out.print("  [");
        for (int i = 0; i < ticks; i++) {
            System.out.print("▓");
            try { Thread.sleep(100 * dureeReelle / Math.max(ticks, 1)); } catch (InterruptedException e) {}
        }
        System.out.println("] (" + m.getDureeFormatee() + ")");
    }

    // ==================== NOTES ====================

    /**
     * Affiche la note moyenne d'un morceau avec étoiles visuelles.
     */
    public void afficherNoteMorceau(double moyenne, int nbVotes) {
        if (nbVotes == 0) {
            System.out.println("  Note      : Aucune note pour l'instant.");
        } else {
            // Affichage étoiles : ★ pour étoile pleine, ☆ pour étoile vide
            StringBuilder etoiles = new StringBuilder();
            int pleines = (int) Math.round(moyenne);
            for (int i = 1; i <= 5; i++) {
                etoiles.append(i <= pleines ? "★" : "☆");
            }
            System.out.printf("  Note      : %s  %.1f/5 (%d vote%s)%n",
                    etoiles, moyenne, nbVotes, nbVotes > 1 ? "s" : "");
        }
    }

    /**
     * Propose à l'abonné de noter le morceau qu'il vient d'écouter.
     * Affiche sa note actuelle si elle existe.
     * Retourne 1 pour noter, 2 pour passer.
     */
    public int proposerNotation(int noteActuelle) {
        System.out.println();
        if (noteActuelle > 0) {
            System.out.println("  Votre note actuelle pour ce morceau : " + noteActuelle + "/5");
            System.out.println("  1. Modifier ma note");
        } else {
            System.out.println("  1. Noter ce morceau (1 a 5)");
        }
        System.out.println("  2. Passer");
        System.out.print("  Votre choix : ");
        return lireEntier();
    }

    /**
     * Demande une note entre 1 et 5.
     */
    public int demanderNote() {
        int note = 0;
        while (note < 1 || note > 5) {
            System.out.print("  Votre note (1 a 5) : ");
            note = lireEntier();
            if (note < 1 || note > 5) System.out.println("  Note invalide, entrez un chiffre entre 1 et 5.");
        }
        return note;
    }

    /**
     * Confirme que la note a bien été enregistrée.
     */
    public void afficherNoteEnregistree(int note) {
        StringBuilder etoiles = new StringBuilder();
        for (int i = 1; i <= 5; i++) etoiles.append(i <= note ? "★" : "☆");
        System.out.println("  Note enregistree : " + etoiles + " (" + note + "/5)");
    }

    // ==================== PLAYLISTS ====================

    /** Affiche le menu de gestion des playlists et retourne le choix (1-8). */
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

    /** @return le nom de la nouvelle playlist saisi */
    public String demanderNomPlaylist() {
        System.out.print("Nom de la playlist : ");
        return clavier.nextLine();
    }

    /** @return le nouveau nom de la playlist saisi */
    public String demanderNouveauNomPlaylist() {
        System.out.print("Nouveau nom de la playlist : ");
        return clavier.nextLine();
    }

    /** @return l'ID de la playlist saisie */
    public int demanderIdPlaylist() {
        System.out.print("ID de la playlist : ");
        return lireEntier();
    }

    /** @return l'ID du morceau saisi */
    public int demanderIdMorceau() {
        System.out.print("ID du morceau : ");
        return lireEntier();
    }

    /** Confirme la création d'une playlist. @param id l'ID généré @param nom le nom choisi */
    public void afficherPlaylistCreee(int id, String nom) {
        System.out.println("Playlist \"" + nom + "\" creee avec succes (ID : " + id + ")");
    }

    /** Confirme le renommage d'une playlist. @param ancienNom nom précédent @param nouveauNom nouveau nom */
    public void afficherPlaylistRenommee(String ancienNom, String nouveauNom) {
        System.out.println("Playlist \"" + ancienNom + "\" renommee en \"" + nouveauNom + "\"");
    }

    /** Affiche la liste des playlists de l'utilisateur. @param playlists la liste à afficher */
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

    /** Affiche le contenu détaillé d'une playlist (morceaux et durée totale).
     *  @param playlist la playlist à afficher */
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

    /** Confirme l'ajout d'un morceau à une playlist.
     *  @param titre titre du morceau @param nomPlaylist nom de la playlist */
    public void afficherMorceauAjoutePlaylist(String titre, String nomPlaylist) {
        System.out.println("\"" + titre + "\" ajoute a la playlist \"" + nomPlaylist + "\"");
    }

    /** Informe que le morceau est déjà présent dans la playlist. */
    public void afficherMorceauDejaPresent() {
        System.out.println("Ce morceau est deja dans la playlist.");
    }

    /** Confirme le retrait d'un morceau de la playlist. */
    public void afficherMorceauRetire() {
        System.out.println("Morceau retire de la playlist.");
    }

    /** Confirme la suppression d'une playlist. @param nom le nom de la playlist supprimée */
    public void afficherPlaylistSupprimee(String nom) {
        System.out.println("Playlist \"" + nom + "\" supprimee.");
    }

    /** Informe que la playlist est introuvable ou n'appartient pas à l'utilisateur. */
    public void afficherPlaylistIntrouvable() {
        System.out.println("Playlist introuvable ou vous n'en etes pas le proprietaire.");
    }

    /** Annonce le début de la lecture d'une playlist. @param nom nom de la playlist */
    public void afficherLecturePlaylist(String nom) {
        System.out.println("\n>>> Lecture de la playlist : " + nom + " <<<");
    }

    /** Annonce la fin de la lecture d'une playlist. @param nom nom de la playlist */
    public void afficherFinPlaylist(String nom) {
        System.out.println(">>> Fin de la playlist : " + nom + " <<<");
    }

    /** Affiche les informations du morceau en cours de lecture dans la playlist.
     *  @param position position actuelle @param total nombre total de morceaux @param m le morceau */
    public void afficherPochette(int position, int total, Morceau m) {
        System.out.println("\n  ♪ Morceau " + position + "/" + total);
        System.out.println("  Titre     : " + m.getTitre());
        System.out.println("  Interprete: " + m.getNomInterprete());
        System.out.println("  Genre     : " + m.getGenre());
        System.out.println("  Duree     : " + m.getDureeFormatee());
    }

    /** Affiche les contrôles de navigation du lecteur et retourne l'action choisie.
     *  @param peutReculer true si un morceau précédent existe
     *  @param peutAvancer true si un morceau suivant existe
     *  @return 1=précédent 2=suivant 3=arrêter */
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

    /** Informe que la playlist est vide et ne peut pas être lue. */
    public void afficherPlaylistVide() {
        System.out.println("Cette playlist est vide, ajoutez des morceaux d'abord !");
    }

    // ==================== HISTORIQUE ====================

    /** Affiche l'historique d'écoute du client connecté. @param historique la liste des entrées */
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

    /** Informe l'utilisateur que son choix de menu est invalide. */
    public void afficherChoixInvalide() {
        System.out.println("Choix invalide !");
    }

    /** Affiche un message de retour au menu principal. */
    public void afficherRetourMenuPrincipal() {
        System.out.println("Retour au menu principal.");
    }

    /** Informe que l'ID saisi est invalide ou introuvable. */
    public void afficherErreurId() {
        System.out.println("ID invalide ou introuvable.");
    }

    /** Affiche un message générique à l'utilisateur. @param message le texte à afficher */
    public void afficherMessage(String message) {
        System.out.println(message);
    }

    // ==================== NOTIFICATIONS DE SESSION (override dans VueGraphique) ====================

    /** No-op en console — notifie la vue graphique d'une session admin ouverte. @param nom nom de l'admin */
    public void notifierSessionAdmin(String nom)  { /* no-op console */ }
    /** No-op en console — notifie la vue graphique d'une session client ouverte. @param nom nom du client */
    public void notifierSessionClient(String nom) { /* no-op console */ }
    /** No-op en console — notifie la vue graphique d'une session visiteur ouverte. */
    public void notifierSessionVisiteur()         { /* no-op console */ }
}