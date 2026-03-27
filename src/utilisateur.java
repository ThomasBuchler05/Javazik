import java.util.*;
import java.io.*;

public class utilisateur {
    private int ID;
    private String NOM, PRENOM, MDP, EMAIL;
    Random randomID = new Random();
    void inscription() {
        Scanner sc = new Scanner(System.in);

        ID = randomID.nextInt(1000)+1;
        System.out.println("Entrez votre nom : ");
        NOM = sc.nextLine();

        System.out.print("Entrez votre prénom : ");
        PRENOM = sc.nextLine();

        System.out.print("Entrez votre email : ");
        EMAIL = sc.nextLine();

        System.out.print("Entrez votre mot de passe : ");
        MDP = sc.nextLine();

        System.out.println("Inscription réussie !");
        try {
            FileWriter fw = new FileWriter("monfichier.txt", true);
            fw.write(ID + ";" + NOM + ";" + PRENOM + ";"+ MDP + ";"+ EMAIL + "\n");
            fw.close();
            System.out.println("Écriture réussie !");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
