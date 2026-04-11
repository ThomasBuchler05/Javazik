package controller;

import model.Client;
import model.Musique;
import model.Playlist;
import model.Utilisateur;
import view.VueConsole;

import java.util.List;

public class ControleurPrincipal {

    private VueConsole vue;
    private Utilisateur utilisateur;

    public ControleurPrincipal() {
        this.vue = new VueConsole();
        this.utilisateur = new Client();
    }

    public void lancer() {
        while (true) {
            vue.afficherBienvenue();
            int choix = vue.afficherMenuPrincipal();

            switch (choix) {
                case 1:
                    connexionAdmin();
                    break;
                case 2:
                    connexionClient();
                    break;
                case 3:
                    inscription();
                    break;
                case 4:
                    menuVisiteur();
                    break;
                case 5:
                    System.exit(0);
            }
        }
    }

    // ==================== CONNEXION ADMIN ====================

    private void connexionAdmin() {
        boolean connexion = false;
        while (!connexion) {
            vue.afficherConnexionAdmin();
            String mail = vue.demanderMail();
            String resultat = utilisateur.verifierMailAdmin(mail);

            if (resultat.equals("MAIL_FOUND")) {
                String mdpSaisi = vue.demanderMdp();
                while (!utilisateur.verifierMdp(mdpSaisi)) {
                    vue.afficherMdpIncorrect();
                    mdpSaisi = vue.demanderMdp();
                }
                vue.afficherConnexionReussie();
                connexion = true;
                menuAdmin();
            } else if (resultat.equals("NOT_ADMIN")) {
                vue.afficherPasAdmin();
            } else {
                vue.afficherMailIncorrect();
            }
        }
    }

    // ==================== CONNEXION CLIENT ====================

    private void connexionClient() {
        boolean connexion = false;
        while (!connexion) {
            String mail = vue.demanderMail();
            String resultat = utilisateur.verifierMailClient(mail);

            if (resultat.equals("MAIL_FOUND")) {
                String mdpSaisi = vue.demanderMdp();
                while (!utilisateur.verifierMdp(mdpSaisi)) {
                    vue.afficherMdpIncorrect();
                    mdpSaisi = vue.demanderMdp();
                }
                vue.afficherConnexionReussie();
                connexion = true;
                menuClient();
            } else {
                vue.afficherMailIncorrect();
            }
        }
    }

    // ==================== INSCRIPTION ====================

    private void inscription() {
        String nom = vue.demanderNom();
        String prenom = vue.demanderPrenom();
        String email = vue.demanderEmail();
        String mdp = vue.demanderMotDePasse();
        utilisateur.inscrire(nom, prenom, email, mdp);
        vue.afficherInscriptionReussie();
    }

    // ==================== MENU ADMIN ====================

    private void menuAdmin() {
        int choixAdmin;
        do {
            choixAdmin = vue.afficherMenuAdmin();

            switch (choixAdmin) {
                case 1:
                    String titre = vue.demanderTitreMusique();
                    String artiste = vue.demanderArtisteMusique();
                    int annee = vue.demanderAnneeMusique();
                    int nouvelId = Musique.ajouterMusique(titre, artiste, annee);
                    vue.afficherMusiqueAjoutee(nouvelId);
                    break;
                case 2:
                    int idCible = vue.demanderIdSuppression();
                    boolean supprime = Musique.supprimerMusique(idCible);
                    if (supprime) {
                        vue.afficherMusiqueSupprimee();
                    } else {
                        vue.afficherMusiqueNonTrouvee(idCible);
                    }
                    break;
                case 3:
                    vue.afficherRetourMenuPrincipal();
                    break;
                default:
                    vue.afficherChoixInvalide();
            }
        } while (choixAdmin != 3);
    }

    // ==================== MENU CLIENT ====================

    private void menuClient() {
        int choixClient;
        do {
            choixClient = vue.afficherMenuClient();

            switch (choixClient) {
                case 1:
                    menuPlaylist();
                    break;
                case 2:
                    ecouter(Integer.MAX_VALUE);
                    break;
                case 3:
                    // consulter historique (pas encore implémenté)
                    break;
                case 4:
                    vue.afficherRetourMenuPrincipal();
                    break;
                default:
                    vue.afficherChoixInvalide();
            }
        } while (choixClient != 4);
    }

    // ==================== MENU PLAYLIST ====================

    private void menuPlaylist() {
        int choix;
        do {
            choix = vue.afficherMenuPlaylist();
            switch (choix) {
                case 1:
                    creerPlaylist();
                    break;
                case 2:
                    voirPlaylists();
                    break;
                case 3:
                    ajouterMusiquePlaylist();
                    break;
                case 4:
                    retirerMusiquePlaylist();
                    break;
                case 5:
                    ecouterPlaylist();
                    break;
                case 6:
                    supprimerPlaylist();
                    break;
                case 7:
                    break;
                default:
                    vue.afficherChoixInvalide();
            }
        } while (choix != 7);
    }

    private void creerPlaylist() {
        String nom = vue.demanderNomPlaylist();
        Playlist p = Playlist.creer(nom, utilisateur.getID());
        vue.afficherPlaylistCreee(p.getId(), p.getNom());
    }

    private void voirPlaylists() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        for (Playlist p : playlists) {
            vue.afficherContenuPlaylist(p);
        }
    }

    private void ajouterMusiquePlaylist() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        if (playlists.isEmpty()) return;

        int idPlaylist = vue.demanderIdPlaylist();
        Playlist cible = trouverPlaylist(playlists, idPlaylist);
        if (cible == null) {
            vue.afficherPlaylistIntrouvable();
            return;
        }

        // Rechercher la musique par titre/artiste pour afficher l'ID
        String recherche = vue.demanderRecherche();
        Musique m = Musique.rechercher(recherche);
        if (m == null) {
            vue.afficherAucuneMusiqueTrouvee();
            return;
        }
        vue.afficherMusique(m);

        boolean ok = Playlist.ajouterMusique(idPlaylist, m.getId());
        if (ok) {
            vue.afficherMusiqueAjouteePlaylist(m.getTitre(), cible.getNom());
        } else {
            vue.afficherMusiqueDejaPresente();
        }
    }

    private void retirerMusiquePlaylist() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        if (playlists.isEmpty()) return;

        int idPlaylist = vue.demanderIdPlaylist();
        Playlist cible = trouverPlaylist(playlists, idPlaylist);
        if (cible == null) {
            vue.afficherPlaylistIntrouvable();
            return;
        }
        vue.afficherContenuPlaylist(cible);

        int idMusique = vue.demanderIdMusique();
        boolean ok = Playlist.retirerMusique(idPlaylist, idMusique);
        if (ok) {
            vue.afficherMusiqueRetirée();
        } else {
            vue.afficherErreurId();
        }
    }

    private void ecouterPlaylist() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        if (playlists.isEmpty()) return;

        int idPlaylist = vue.demanderIdPlaylist();
        Playlist cible = trouverPlaylist(playlists, idPlaylist);
        if (cible == null) {
            vue.afficherPlaylistIntrouvable();
            return;
        }

        List<Musique> musiques = cible.getMusiques();
        if (musiques.isEmpty()) {
            vue.afficherPlaylistVide();
            return;
        }

        vue.afficherLecturePlaylist(cible.getNom());
        for (Musique m : musiques) {
            vue.afficherMusique(m);
            vue.afficherEcoute();
        }
        vue.afficherFinPlaylist(cible.getNom());
    }

    private void supprimerPlaylist() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        if (playlists.isEmpty()) return;

        int idPlaylist = vue.demanderIdPlaylist();
        Playlist cible = trouverPlaylist(playlists, idPlaylist);
        if (cible == null) {
            vue.afficherPlaylistIntrouvable();
            return;
        }
        boolean ok = Playlist.supprimer(idPlaylist, utilisateur.getID());
        if (ok) {
            vue.afficherPlaylistSupprimee(cible.getNom());
        } else {
            vue.afficherPlaylistIntrouvable();
        }
    }

    private Playlist trouverPlaylist(List<Playlist> playlists, int id) {
        for (Playlist p : playlists) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    // ==================== VISITEUR ====================

    private void menuVisiteur() {
        ecouter(5);
    }

    // ==================== ECOUTE ====================

    private void ecouter(int maxEcoutes) {
        int compteur = 0;
        boolean stop = false;
        while (!stop && compteur < maxEcoutes) {
            String recherche = vue.demanderRecherche();
            if (recherche.equalsIgnoreCase("stop")) {
                stop = true;
                break;
            }
            Musique m = Musique.rechercher(recherche);
            if (m != null) {
                vue.afficherMusique(m);
                vue.afficherEcoute();
                compteur++;
            } else {
                vue.afficherAucuneMusiqueTrouvee();
            }
            vue.afficherEcoutesRestantes(maxEcoutes - compteur);
        }
    }
}
