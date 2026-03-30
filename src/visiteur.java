import java.util.*;
public class visiteur {
    public static void ecouter() {
        Scanner clavier = new Scanner(System.in);
        boolean stop = false;
        int max = 0;
        do {
            System.out.println("Quelle musique recherchez-vous ? Titre ou Artiste que vous cherchez : ");
            String titre = clavier.nextLine();
            if (titre.equalsIgnoreCase("stop")){
                stop = true;
                break;
            }
            else{
            max = max + musique.rechercher(titre);
            }
            System.out.println("\nIl vous reste " + (5-max) + " musiques que vous pouvez écouter, pour plus d'essais prenez un abonnements à 1900 euros par semaines." );
        }while(!stop || max < 5);
    }
}
