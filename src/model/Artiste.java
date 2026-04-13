package model;

import java.io.*;
import java.util.*;

/**
 * Représente un artiste musical (solo ou membre d'un groupe).
 * Persistance dans artistes.txt au format : id;nom;prenom;nationalite
 */
public class Artiste {

    private static final String FICHIER = "artistes.txt";

    private int id;
    private String nom;
    private String prenom;
    private String nationalite;

    public Artiste(int id, String nom, String prenom, String nationalite) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.nationalite = nationalite;
    }

    // ==================== GETTERS / SETTERS ====================

    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getNationalite() { return nationalite; }

    public void setNom(String nom) { this.nom = nom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public void setNationalite(String nationalite) { this.nationalite = nationalite; }

    /**
     * Retourne le nom complet (prenom nom) ou juste le nom si pas de prenom.
     */
    public String getNomComplet() {
        if (prenom == null || prenom.isEmpty()) {
            return nom;
        }
        return prenom + " " + nom;
    }

    // ==================== PERSISTENCE ====================

    /**
     * Charge tous les artistes depuis le fichier.
     */
    public static List<Artiste> chargerTous() {
        List<Artiste> liste = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 4) continue;
                liste.add(new Artiste(
                    Integer.parseInt(p[0]), p[1], p[2], p[3]
                ));
            }
        } catch (FileNotFoundException e) {
            // fichier pas encore créé
        } catch (IOException e) {
            e.printStackTrace();
        }
        return liste;
    }

    /**
     * Recherche un artiste par son ID.
     */
    public static Artiste rechercherParId(int idCible) {
        for (Artiste a : chargerTous()) {
            if (a.getId() == idCible) return a;
        }
        return null;
    }

    /**
     * Recherche des artistes par nom (partiel, insensible à la casse).
     */
    public static List<Artiste> rechercherParNom(String recherche) {
        List<Artiste> resultats = new ArrayList<>();
        String rech = recherche.toLowerCase();
        for (Artiste a : chargerTous()) {
            if (a.getNom().toLowerCase().contains(rech)
                || a.getPrenom().toLowerCase().contains(rech)
                || a.getNomComplet().toLowerCase().contains(rech)) {
                resultats.add(a);
            }
        }
        return resultats;
    }

    /**
     * Ajoute un artiste et retourne l'objet créé.
     */
    public static Artiste ajouter(String nom, String prenom, String nationalite) {
        int nouvelId = genererNouvelId();
        Artiste a = new Artiste(nouvelId, nom, prenom, nationalite);
        try (FileWriter fw = new FileWriter(FICHIER, true)) {
            fw.write(nouvelId + ";" + nom + ";" + prenom + ";" + nationalite + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return a;
    }

    /**
     * Supprime un artiste par son ID.
     */
    public static boolean supprimer(int idCible) {
        List<String> lignes = new ArrayList<>();
        boolean trouve = false;
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (Integer.parseInt(p[0]) == idCible) {
                    trouve = true;
                } else {
                    lignes.add(ligne);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (!trouve) return false;
        ecrireFichier(lignes);
        return true;
    }

    // ==================== UTILITAIRES ====================

    private static int genererNouvelId() {
        int max = 0;
        for (Artiste a : chargerTous()) {
            if (a.getId() > max) max = a.getId();
        }
        return max + 1;
    }

    private static void ecrireFichier(List<String> lignes) {
        try (FileWriter fw = new FileWriter(FICHIER, false)) {
            for (String l : lignes) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "[" + id + "] " + getNomComplet() + " (" + nationalite + ")";
    }
}
