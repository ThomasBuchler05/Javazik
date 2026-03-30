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
                    //utilisateur.connexion();
                    int i;
                    System.out.println("Entrez votre choix: ");

                    break;
                case 2:
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
