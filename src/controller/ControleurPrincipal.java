package controller;

import model.Client;
import model.Musique;
import model.Utilisateur;
import view.VueConsole;

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
                    // créer et gérer une playlist (pas encore implémenté)
                    break;
                case 2:
                    ecouter(5); // limite de 5 pour visiteur, illimité pour client si vous changez
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
