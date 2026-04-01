package model;

import java.io.*;
import java.util.*;

public class Musique {

    private int id;
    private String titre;
    private String artiste;
    private int annee;

    public Musique(int id, String titre, String artiste, int annee) {
        this.id = id;
        this.titre = titre;
        this.artiste = artiste;
        this.annee = annee;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getArtiste() { return artiste; }
    public void setArtiste(String artiste) { this.artiste = artiste; }
    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    /**
     * Recherche une musique par titre ou artiste dans le fichier musiques.txt
     * Retourne la Musique trouvée ou null si rien trouvé
     */
    public static Musique rechercher(String titreRecherche) {
        String fichier = "musiques.txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(fichier))) {
            String ligne;

            while ((ligne = reader.readLine()) != null) {

                if (ligne.trim().isEmpty()) continue;

                String[] parties = ligne.split(";");

                int id = Integer.parseInt(parties[0]);
                String titre = parties[1];
                String artiste = parties[2];
                int annee = Integer.parseInt(parties[3]);

                if (titre.equalsIgnoreCase(titreRecherche) || artiste.equalsIgnoreCase(titreRecherche)) {
                    return new Musique(id, titre, artiste, annee);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Ajoute une musique dans le fichier musiques.txt
     * Génère automatiquement un nouvel ID (max existant + 1)
     */
    public static int ajouterMusique(String titre, String artiste, int annee) {
        String fichier = "musiques.txt";

        // générer un ID: max existant + 1
        int nouvelID = 1;
        try (BufferedReader reader = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parties = ligne.split(";");
                int idExistant = Integer.parseInt(parties[0]);
                if (idExistant >= nouvelID) {
                    nouvelID = idExistant + 1;
                }
            }
        } catch (FileNotFoundException e) {
            // fichier n'existe pas encore, on commence à 1
        } catch (IOException e) {
            e.printStackTrace();
        }

        // écrire dans le fichier
        try (FileWriter fw = new FileWriter(fichier, true)) {
            fw.write("\n" + nouvelID + ";" + titre + ";" + artiste + ";" + annee);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return nouvelID;
    }

    /**
     * Supprime une musique par son ID dans le fichier musiques.txt
     * Retourne true si trouvée et supprimée, false sinon
     */
    public static boolean supprimerMusique(int idCible) {
        String fichier = "musiques.txt";

        List<String> lignesAGarder = new ArrayList<>();
        boolean trouve = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parties = ligne.split(";");
                int idLigne = Integer.parseInt(parties[0]);
                if (idLigne == idCible) {
                    trouve = true;
                } else {
                    lignesAGarder.add(ligne);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!trouve) {
            return false;
        }

        try (FileWriter fw = new FileWriter(fichier, false)) {
            for (String ligne : lignesAGarder) {
                fw.write("\n" + ligne);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }
}
