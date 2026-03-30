import java.util.concurrent.*;
import java.util.*;
public class menu {

    public static void afficher(utilisateur teste) {
        System.out.println("BIENVENUE SUR JAVAZIK");
        Scanner clavier = new Scanner(System.in);
        for(int i=0;i<21;i++) {
            System.out.print("-");
            try {
                Thread.sleep(150); // pause de 1 seconde (1000 ms)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
            System.out.println("\n1. Se connecter en tant qu'administrateur");
            System.out.println("2. Se connecter en tant que client");
            System.out.println("3. Créer un compte client");
            System.out.println("4. Continuer en tant que simple visiteur");
            System.out.println("5. Quitter");
            int choix;
            do {
                System.out.println("Entrez votre choix: ");
                choix = clavier.nextInt();
                if (choix < 1 || choix >5){
                    System.out.println("Votre valeur entré est incorrecte !");
                }
            }
            while(choix < 1 || choix > 5);
            switch(choix){
                case 1:
                    teste.connexion();
                    int choixAdmin;
                    do {
                        System.out.println("\n  MENU ADMINISTRATEUR ");
                        System.out.println("1. Ajouter une musique");
                        System.out.println("2. Supprimer une musique");
                        System.out.println("3. Quitter le menu admin");
                        System.out.print("Votre choix : ");
                        choixAdmin = clavier.nextInt();
                        clavier.nextLine(); // vider le buffer

                        switch(choixAdmin){
                            case 1:
                                administrateur.ajouter_musique();
                                break;
                            case 2:
                                administrateur.supprimer_musique();
                                break;
                            case 3:
                                System.out.println("Retour au menu principal");
                                break;
                            default:
                                System.out.println("Choix invalide !");
                        }
                    } while(choixAdmin != 3);
                    break;

                case 2:
                    teste.connexion();
                    int choixClient;
                    do {
                        System.out.println("\n  MENU Client ");
                        System.out.println("1. Créer et gérer une playlist");
                        System.out.println("2. Ecouter une musique");
                        System.out.println("3. Consulter l'historique d'écoute");
                        System.out.println("4. Revenir au menu principal");

                        System.out.print("Votre choix : ");
                        choixClient = clavier.nextInt();
                        clavier.nextLine();

                        switch(choixClient){
                            case 1:

                                break;
                            case 2:
                                visiteur.ecouter();
                                break;
                            case 3:

                                break;
                            case 4:
                                System.out.println("Retour au menu principal");
                                break;
                            default:
                                System.out.println("Choix invalide !");
                        }
                    } while(choixClient != 4);
                    break;
                case 3:
                    teste.inscription();
                case 4:
                    visiteur.ecouter();
                case 5:
                    System.exit(0);
            }
    }
}
