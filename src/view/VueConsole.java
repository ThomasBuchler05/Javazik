package view;

import model.Musique;
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

    public String demanderRecherche() {
        System.out.println("Quelle musique recherchez-vous ? Titre ou Artiste que vous cherchez : ");
        return clavier.nextLine();
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

    // ==================== DIVERS ====================

    public void afficherChoixInvalide() {
        System.out.println("Choix invalide !");
    }

    public void afficherRetourMenuPrincipal() {
        System.out.println("Retour au menu principal");
    }
}
