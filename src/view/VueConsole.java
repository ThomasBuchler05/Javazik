package view;

import model.Musique;
import model.Historique;
import java.util.List;
import java.util.Scanner;

public class VueConsole {

    private Scanner clavier = new Scanner(System.in);

    // ==================== MENU PRINCIPAL ====================

    public void afficherBienvenue() {
        System.out.println("BIENVENUE SUR JAVAZIK");
        System.out.println("Chargement de l'application !");
        for (int i = 0; i < 50; i++) {
            System.out.print("-");
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println();
    }

    public int afficherMenuPrincipal() {
        System.out.println("\n1. Se connecter en tant qu'administrateur");
        System.out.println("2. Se connecter en tant que client");
        System.out.println("3. Créer un compte client");
        System.out.println("4. Continuer en tant que simple visiteur");
        System.out.println("5. Quitter");
        int choix;
        do {
            System.out.println("Entrez votre choix: ");
            choix = clavier.nextInt();
            if (choix < 1 || choix > 5) {
                System.out.println("Votre valeur entré est incorrecte !");
            }
        } while (choix < 1 || choix > 5);
        return choix;
    }

    // ==================== MENU ADMIN ====================

    public int afficherMenuAdmin() {
        System.out.println("\n  MENU ADMINISTRATEUR ");
        System.out.println("1. Ajouter une musique");
        System.out.println("2. Supprimer une musique");
        System.out.println("3. Quitter le menu admin");
        System.out.print("Votre choix : ");
        int choix = clavier.nextInt();
        clavier.nextLine(); // vider le buffer
        return choix;
    }

    // ==================== MENU CLIENT ====================

    public int afficherMenuClient() {
        System.out.println("\n  MENU Client ");
        System.out.println("1. Créer et gérer une playlist");
        System.out.println("2. Ecouter une musique");
        System.out.println("3. Consulter l'historique d'écoute");
        System.out.println("4. Revenir au menu principal");
        System.out.print("Votre choix : ");
        int choix = clavier.nextInt();
        clavier.nextLine();
        return choix;
    }

    // ==================== CONNEXION ====================

    public void afficherConnexionAdmin() {
        System.out.println("Veuillez êtes en connexion admin");
    }

    public String demanderMail() {
        System.out.print("Veuillez entrer votre mail : ");
        return clavier.nextLine();
    }


    public String demanderMdp() {
        System.out.println("Veuillez entrer votre mot de passe :");
        return clavier.nextLine();
    }

    public void afficherMdpIncorrect() {
        System.out.println("Mot de passe incorrecte!");
    }

    public void afficherMailIncorrect() {

        System.out.println("Mail incorecte !");
    }

    public void afficherPasAdmin() {
        System.out.println("Vous n'êtes pas admin");
    }

    public void afficherConnexionReussie() {
        System.out.println("Connexion réussie !");
    }

    // ==================== INSCRIPTION ====================

    public String demanderNom() {
        System.out.println("Entrez votre nom : ");
        return clavier.nextLine();
    }

    public String demanderPrenom() {
        System.out.print("Entrez votre prénom : ");
        return clavier.nextLine();
    }

    public String demanderEmail() {
        System.out.print("Entrez votre email : ");
        return clavier.nextLine();
    }

    public String demanderMotDePasse() {
        System.out.print("Entrez votre mot de passe : ");
        return clavier.nextLine();
    }

    public void afficherInscriptionReussie() {
        System.out.println("Inscription réussie !");
        System.out.println("Écriture réussie !");
    }

    // ==================== MUSIQUE (ADMIN) ====================

    public String demanderTitreMusique() {
        System.out.println("Veuillez saisir le Titre de la musique");
        return clavier.nextLine();
    }

    public String demanderArtisteMusique() {
        System.out.println("Veuillez saisir l'artiste de la musique");
        return clavier.nextLine();
    }

    public int demanderAnneeMusique() {
        System.out.println("Veuillez saisir l'année de la musique");
        int annee = clavier.nextInt();
        clavier.nextLine();
        return annee;
    }

    public void afficherMusiqueAjoutee(int id) {
        System.out.println("Musique ajoutée ! (ID : " + id + ")");
    }

    public int demanderIdSuppression() {
        System.out.println("Veuillez saisir l'ID de la musique à supprimer");
        int id = clavier.nextInt();
        clavier.nextLine();
        return id;
    }

    public void afficherMusiqueSupprimee() {
        System.out.println("Musique supprimée !");
    }

    public void afficherMusiqueNonTrouvee(int id) {
        System.out.println("Aucune musique trouvée avec l'ID " + id);
    }

    // ==================== VISITEUR / ECOUTE ====================

    public String demanderRechercheMusique() {
        System.out.print("\nQuelle musique voulez-vous ecouter ? (titre ou artiste) : ");
        return clavier.nextLine();
    }

    /**
     * Sous-menu affiché quand la recherche n'a rien donné.
     * Retourne 1 = réessayer, 2 = retour menu
     */
    public int afficherMenuApresEchecRecherche() {
        System.out.println("1. Rechercher un autre morceau");
        System.out.println("2. Retour au menu");
        System.out.print("Votre choix : ");
        try {
            int choix = Integer.parseInt(clavier.nextLine().trim());
            return choix;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * Sous-menu affiché après chaque écoute réussie.
     * @param ecoutesRestantes -1 si illimité, sinon le nombre restant
     * Retourne 1 = écouter un autre, 2 = retour menu
     */
    public int afficherMenuApresEcoute(int ecoutesRestantes) {
        System.out.println();
        if (ecoutesRestantes >= 0) {
            System.out.println("Il vous reste " + ecoutesRestantes + " ecoute(s) disponible(s).");
        }
        System.out.println("1. Ecouter un autre morceau");
        System.out.println("2. Retour au menu");
        System.out.print("Votre choix : ");
        try {
            int choix = Integer.parseInt(clavier.nextLine().trim());
            return choix;
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    public void afficherLimiteEcoutesAtteinte() {
        System.out.println("\nVous avez atteint la limite d'ecoutes pour cette session.");
        System.out.println("Creez un compte pour profiter d'ecoutes illimitees !");
    }

    public void afficherMusique(Musique m) {
        System.out.println("ID : " + m.getId());
        System.out.println("Titre : " + m.getTitre());
        System.out.println("Artiste : " + m.getArtiste());
        System.out.println("Année : " + m.getAnnee());
    }

    public void afficherEcoute() {
        for (int i = 0; i < 15; i++) {
            System.out.print("-");
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println();
    }

    public void afficherAucuneMusiqueTrouvee() {
        System.out.println("Aucune musique trouvée avec ce titre.");
    }

    public void afficherEcoutesRestantes(int restantes) {
        System.out.println("\nIl vous reste " + restantes + " musiques que vous pouvez écouter, pour plus d'essais prenez un abonnements à 1900 euros par semaines.");
    }

    // ==================== PLAYLISTS ====================

    public int afficherMenuPlaylist() {
        System.out.println("\n  GESTION DES PLAYLISTS ");
        System.out.println("1. Creer une nouvelle playlist");
        System.out.println("2. Voir mes playlists");
        System.out.println("3. Ajouter une musique a une playlist");
        System.out.println("4. Retirer une musique d'une playlist");
        System.out.println("5. Ecouter une playlist");
        System.out.println("6. Supprimer une playlist");
        System.out.println("7. Retour au menu client");
        System.out.print("Votre choix : ");
        int choix = clavier.nextInt();
        clavier.nextLine();
        return choix;
    }

    public String demanderNomPlaylist() {
        System.out.print("Nom de la playlist : ");
        return clavier.nextLine();
    }

    public int demanderIdPlaylist() {
        System.out.print("ID de la playlist : ");
        int id = clavier.nextInt();
        clavier.nextLine();
        return id;
    }

    public int demanderIdMusique() {
        System.out.print("ID de la musique : ");
        int id = clavier.nextInt();
        clavier.nextLine();
        return id;
    }

    public void afficherPlaylistCreee(int id, String nom) {
        System.out.println("Playlist \"" + nom + "\" creee avec succes (ID : " + id + ") !");
    }

    public void afficherListePlaylists(java.util.List<model.Playlist> playlists) {
        if (playlists.isEmpty()) {
            System.out.println("Vous n'avez aucune playlist pour le moment.");
            return;
        }
        System.out.println("\n--- Vos playlists ---");
        for (model.Playlist p : playlists) {
            System.out.println(p);
        }
    }

    public void afficherContenuPlaylist(model.Playlist playlist) {
        System.out.println("\n--- Playlist : " + playlist.getNom() + " ---");
        java.util.List<model.Musique> musiques = playlist.getMusiques();
        if (musiques.isEmpty()) {
            System.out.println("  (aucune musique dans cette playlist)");
        } else {
            for (model.Musique m : musiques) {
                System.out.println("  [" + m.getId() + "] " + m.getTitre() + " - " + m.getArtiste() + " (" + m.getAnnee() + ")");
            }
        }
    }

    public void afficherMusiqueAjouteePlaylist(String titre, String nomPlaylist) {
        System.out.println("\"" + titre + "\" ajoutee a la playlist \"" + nomPlaylist + "\" !");
    }

    public void afficherMusiqueDejaPresente() {
        System.out.println("Cette musique est deja dans la playlist.");
    }

    public void afficherMusiqueRetirée() {
        System.out.println("Musique retiree de la playlist.");
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

    /**
     * Affiche le morceau en cours avec sa position dans la playlist.
     */
    public void afficherPochette(int position, int total, Musique m) {
        System.out.println("\n  ♪ Morceau " + position + "/" + total);
        System.out.println("  Titre   : " + m.getTitre());
        System.out.println("  Artiste : " + m.getArtiste());
        System.out.println("  Annee   : " + m.getAnnee());
    }

    /**
     * Affiche les contrôles du lecteur après un morceau.
     * @param peutReculer  true si un morceau précédent existe
     * @param peutAvancer  true si un morceau suivant existe
     * @return choix de l'utilisateur (1=précédent, 2=suivant, 3=stop)
     */
    public int afficherControlesLecteur(boolean peutReculer, boolean peutAvancer) {
        System.out.println();
        if (peutReculer)  System.out.println("1. Morceau precedent");
        if (peutAvancer)  System.out.println("2. Morceau suivant");
        System.out.println("3. Arreter la playlist");
        System.out.print("Votre choix : ");
        try {
            int choix = Integer.parseInt(clavier.nextLine().trim());
            // Si l'utilisateur choisit précédent alors qu'il n'y en a pas, on ignore
            if (choix == 1 && !peutReculer) return 2;
            // Si l'utilisateur choisit suivant alors qu'il n'y en a pas, on passe à fin
            if (choix == 2 && !peutAvancer) return 3;
            return choix;
        } catch (NumberFormatException e) {
            return 2; // par défaut : morceau suivant
        }
    }

    public void afficherPlaylistVide() {
        System.out.println("Cette playlist est vide, ajoutez des musiques d'abord !");
    }

    public void afficherErreurId() {
        System.out.println("ID invalide ou introuvable.");
    }

    // ==================== HISTORIQUE ====================

    /**
     * Affiche l'historique d'écoute d'un abonné (du plus récent au plus ancien).
     *
     * @param historique liste des entrées d'historique
     */
    public void afficherHistorique(List<Historique> historique) {
        System.out.println("\n========== HISTORIQUE D'ECOUTE ==========");
        if (historique.isEmpty()) {
            System.out.println("Vous n'avez encore ecouté aucun morceau.");
        } else {
            int num = 1;
            for (Historique h : historique) {
                System.out.println(num + ". " + h);
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
        System.out.println("Retour au menu principal");
    }
}