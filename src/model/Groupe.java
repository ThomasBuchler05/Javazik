package model;

import java.io.*;
import java.util.*;

/**
 * Représente un groupe musical composé d'artistes.
 * Persistance dans groupes.txt au format : id;nom;dateCreation;nationalite
 * Les membres sont dans groupe_artistes.txt au format : idGroupe;idArtiste
 */
public class Groupe {

    private static final String FICHIER = "groupes.txt";
    private static final String FICHIER_MEMBRES = "groupe_artistes.txt";

    private int id;
    private String nom;
    private int dateCreation;
    private String nationalite;
    private List<Artiste> membres;

    public Groupe(int id, String nom, int dateCreation, String nationalite) {
        this.id = id;
        this.nom = nom;
        this.dateCreation = dateCreation;
        this.nationalite = nationalite;
        this.membres = new ArrayList<>();
    }

    // ==================== GETTERS / SETTERS ====================

    public int getId() { return id; }
    public String getNom() { return nom; }
    public int getDateCreation() { return dateCreation; }
    public String getNationalite() { return nationalite; }
    public List<Artiste> getMembres() { return membres; }

    public void setNom(String nom) { this.nom = nom; }
    public void setMembres(List<Artiste> membres) { this.membres = membres; }

    // ==================== PERSISTENCE ====================

    /**
     * Charge tous les groupes depuis le fichier (avec leurs membres).
     */
    public static List<Groupe> chargerTous() {
        List<Groupe> liste = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 4) continue;
                Groupe g = new Groupe(
                    Integer.parseInt(p[0]), p[1],
                    Integer.parseInt(p[2]), p[3]
                );
                g.setMembres(chargerMembres(g.getId()));
                liste.add(g);
            }
        } catch (FileNotFoundException e) {
            // fichier pas encore créé
        } catch (IOException e) {
            e.printStackTrace();
        }
        return liste;
    }

    /**
     * Recherche un groupe par son ID.
     */
    public static Groupe rechercherParId(int idCible) {
        for (Groupe g : chargerTous()) {
            if (g.getId() == idCible) return g;
        }
        return null;
    }

    /**
     * Recherche des groupes par nom (partiel, insensible à la casse).
     */
    public static List<Groupe> rechercherParNom(String recherche) {
        List<Groupe> resultats = new ArrayList<>();
        String rech = recherche.toLowerCase();
        for (Groupe g : chargerTous()) {
            if (g.getNom().toLowerCase().contains(rech)) {
                resultats.add(g);
            }
        }
        return resultats;
    }

    /**
     * Ajoute un groupe et retourne l'objet créé.
     */
    public static Groupe ajouter(String nom, int dateCreation, String nationalite) {
        int nouvelId = genererNouvelId();
        Groupe g = new Groupe(nouvelId, nom, dateCreation, nationalite);
        try (FileWriter fw = new FileWriter(FICHIER, true)) {
            fw.write(nouvelId + ";" + nom + ";" + dateCreation + ";" + nationalite + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return g;
    }

    /**
     * Supprime un groupe par son ID (et ses associations de membres).
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
        ecrireFichier(FICHIER, lignes);
        supprimerAssociationsMembres(idCible);
        return true;
    }

    /**
     * Ajoute un artiste comme membre du groupe.
     */
    public static boolean ajouterMembre(int idGroupe, int idArtiste) {
        // vérifier qu'il n'est pas déjà membre
        List<Artiste> membres = chargerMembres(idGroupe);
        for (Artiste a : membres) {
            if (a.getId() == idArtiste) return false;
        }
        try (FileWriter fw = new FileWriter(FICHIER_MEMBRES, true)) {
            fw.write(idGroupe + ";" + idArtiste + "\n");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retourne les groupes auxquels appartient un artiste donné.
     */
    public static List<Groupe> getGroupesParArtiste(int idArtiste) {
        List<Integer> idsGroupes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_MEMBRES))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 2) continue;
                if (Integer.parseInt(p[1]) == idArtiste) {
                    idsGroupes.add(Integer.parseInt(p[0]));
                }
            }
        } catch (FileNotFoundException e) {
            // pas de fichier
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Groupe> resultats = new ArrayList<>();
        for (int idG : idsGroupes) {
            Groupe g = rechercherParId(idG);
            if (g != null) resultats.add(g);
        }
        return resultats;
    }

    // ==================== UTILITAIRES PRIVES ====================

    private static List<Artiste> chargerMembres(int idGroupe) {
        List<Artiste> membres = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_MEMBRES))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 2) continue;
                if (Integer.parseInt(p[0]) == idGroupe) {
                    Artiste a = Artiste.rechercherParId(Integer.parseInt(p[1]));
                    if (a != null) membres.add(a);
                }
            }
        } catch (FileNotFoundException e) {
            // pas de fichier
        } catch (IOException e) {
            e.printStackTrace();
        }
        return membres;
    }

    private static int genererNouvelId() {
        int max = 0;
        for (Groupe g : chargerTous()) {
            if (g.getId() > max) max = g.getId();
        }
        return max + 1;
    }

    private static void ecrireFichier(String fichier, List<String> lignes) {
        try (FileWriter fw = new FileWriter(fichier, false)) {
            for (String l : lignes) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void supprimerAssociationsMembres(int idGroupe) {
        List<String> lignes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_MEMBRES))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (Integer.parseInt(p[0]) != idGroupe) {
                    lignes.add(ligne);
                }
            }
        } catch (IOException e) {
            return;
        }
        ecrireFichier(FICHIER_MEMBRES, lignes);
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nom + " (depuis " + dateCreation + ", " + nationalite + ") - "
               + membres.size() + " membre(s)";
    }
}
