import java.io.*;

public class musique {

    public static int rechercher(String titreRecherche) {
        String fichier = "musiques.txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            boolean trouve = false;

            while ((ligne = reader.readLine()) != null) {

                if (ligne.trim().isEmpty()) continue;

                String[] parties = ligne.split(";");

                String id = parties[0];
                String titre = parties[1];
                String artiste = parties[2];
                String annee = parties[3];

                if (titre.equalsIgnoreCase(titreRecherche) || artiste.equalsIgnoreCase(titreRecherche)) {
                    System.out.println("ID : " + id);
                    System.out.println("Titre : " + titre);
                    System.out.println("Artiste : " + artiste);
                    System.out.println("Année : " + annee);
                    trouve = true;
                    for(int i=0;i<15;i++) {
                        System.out.print("-");
                        try {
                            Thread.sleep(150); // pause de 1 seconde (1000 ms)
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return 1;

                }
            }

            if (!trouve) {
                System.out.println("Aucune musique trouvée avec ce titre.");
                return 0;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}