import java.util.*;
public class visiteur {
    public static void ecouter() {
        Scanner clavier = new Scanner(System.in);
        boolean stop = false;
        int max = 1;
        do {
            System.out.println("Quelle musique recherchez-vous ? Titre: ");
            String titre = clavier.nextLine();
            if (titre == "stop"){
                stop = true;
            }
            else{
            musique.rechercher(titre);
            max++;};

        }while(stop == false && max < 5);
    }
}
