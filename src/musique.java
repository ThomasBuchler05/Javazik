import java.io.*;

public class musique {

    public static void rechercher(String titreRecherche) {
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

                    if (titre.equalsIgnoreCase(titreRecherche)) {
                        System.out.println("ID : " + id);
                        System.out.println("Titre : " + titre);
                        System.out.println("Artiste : " + artiste);
                        System.out.println("Année : " + annee);
                        trouve = true;
                        break;
                }
            }

            if (!trouve) {
                System.out.println("Aucune musique trouvée avec ce titre.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}