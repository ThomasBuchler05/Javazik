package model;

import java.io.*;
import java.util.*;

/**
 * Représente un morceau musical.
 * Persistance dans morceaux.txt au format : id;titre;duree;genre;annee;idArtiste;idGroupe
 * (idArtiste=0 si c'est un groupe, idGroupe=0 si c'est un artiste solo)
 */
public class Morceau {

    private static final String FICHIER = "morceaux.txt";

    private int id;
    private String titre;
    private int duree;       // durée en secondes
    private String genre;
    private int annee;
    private int idArtiste;   // 0 si c'est un groupe
    private int idGroupe;    // 0 si c'est un artiste solo

    public Morceau(int id, String titre, int duree, String genre, int annee, int idArtiste, int idGroupe) {
        this.id = id;
        this.titre = titre;
        this.duree = duree;
        this.genre = genre;
        this.annee = annee;
        this.idArtiste = idArtiste;
        this.idGroupe = idGroupe;
    }

    // ==================== GETTERS / SETTERS ====================

    public int getId() { return id; }
    public String getTitre() { return titre; }
    public int getDuree() { return duree; }
    public String getGenre() { return genre; }
    public int getAnnee() { return annee; }
    public int getIdArtiste() { return idArtiste; }
    public int getIdGroupe() { return idGroupe; }

    public void setTitre(String titre) { this.titre = titre; }
    public void setDuree(int duree) { this.duree = duree; }
    public void setGenre(String genre) { this.genre = genre; }

    /**
     * Retourne le nom de l'interprète (artiste solo ou groupe).
     */
    public String getNomInterprete() {
        if (idGroupe > 0) {
            Groupe g = Groupe.rechercherParId(idGroupe);
            return (g != null) ? g.getNom() : "Groupe inconnu";
        } else if (idArtiste > 0) {
            Artiste a = Artiste.rechercherParId(idArtiste);
            return (a != null) ? a.getNomComplet() : "Artiste inconnu";
        }
        return "Inconnu";
    }

    /**
     * Formate la durée en mm:ss.
     */
    public String getDureeFormatee() {
        int min = duree / 60;
        int sec = duree % 60;
        return String.format("%d:%02d", min, sec);
    }

    // ==================== PERSISTENCE ====================

    /**
     * Charge tous les morceaux depuis le fichier.
     */
    public static List<Morceau> chargerTous() {
        List<Morceau> liste = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 7) continue;
                liste.add(new Morceau(
                        Integer.parseInt(p[0]),
                        p[1],
                        Integer.parseInt(p[2]),
                        p[3],
                        Integer.parseInt(p[4]),
                        Integer.parseInt(p[5]),
                        Integer.parseInt(p[6])
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
     * Recherche un morceau par son ID.
     */
    public static Morceau rechercherParId(int idCible) {
        for (Morceau m : chargerTous()) {
            if (m.getId() == idCible) return m;
        }
        return null;
    }

    /**
     * Recherche des morceaux par titre (partiel, insensible à la casse).
     */
    public static List<Morceau> rechercherParTitre(String recherche) {
        List<Morceau> resultats = new ArrayList<>();
        String rech = recherche.toLowerCase();
        for (Morceau m : chargerTous()) {
            if (m.getTitre().toLowerCase().contains(rech)) {
                resultats.add(m);
            }
        }
        return resultats;
    }

    /**
     * Recherche globale par titre ou nom d'interprète.
     * Retourne la liste de tous les morceaux correspondants.
     */
    public static List<Morceau> rechercherGlobal(String recherche) {
        List<Morceau> resultats = new ArrayList<>();
        String rech = recherche.toLowerCase();
        for (Morceau m : chargerTous()) {
            if (m.getTitre().toLowerCase().contains(rech)
                    || m.getNomInterprete().toLowerCase().contains(rech)
                    || m.getGenre().toLowerCase().contains(rech)) {
                resultats.add(m);
            }
        }
        return resultats;
    }

    /**
     * Retourne tous les morceaux d'un artiste solo.
     */
    public static List<Morceau> getMorceauxParArtiste(int idArtiste) {
        List<Morceau> resultats = new ArrayList<>();
        for (Morceau m : chargerTous()) {
            if (m.getIdArtiste() == idArtiste) resultats.add(m);
        }
        return resultats;
    }

    /**
     * Retourne tous les morceaux d'un groupe.
     */
    public static List<Morceau> getMorceauxParGroupe(int idGroupe) {
        List<Morceau> resultats = new ArrayList<>();
        for (Morceau m : chargerTous()) {
            if (m.getIdGroupe() == idGroupe) resultats.add(m);
        }
        return resultats;
    }

    /**
     * Retourne tous les morceaux d'un genre donné.
     */
    public static List<Morceau> getMorceauxParGenre(String genre) {
        List<Morceau> resultats = new ArrayList<>();
        String g = genre.toLowerCase();
        for (Morceau m : chargerTous()) {
            if (m.getGenre().toLowerCase().equals(g)) resultats.add(m);
        }
        return resultats;
    }

    /**
     * Retourne la liste des genres distincts présents dans le catalogue.
     */
    public static List<String> getGenresDisponibles() {
        Set<String> genres = new TreeSet<>();
        for (Morceau m : chargerTous()) {
            genres.add(m.getGenre());
        }
        return new ArrayList<>(genres);
    }

    /**
     * Ajoute un morceau et retourne l'objet créé.
     */
    public static Morceau ajouter(String titre, int duree, String genre, int annee,
                                  int idArtiste, int idGroupe) {
        int nouvelId = genererNouvelId();
        Morceau m = new Morceau(nouvelId, titre, duree, genre, annee, idArtiste, idGroupe);
        try (FileWriter fw = new FileWriter(FICHIER, true)) {
            fw.write(nouvelId + ";" + titre + ";" + duree + ";" + genre + ";"
                    + annee + ";" + idArtiste + ";" + idGroupe + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return m;
    }

    /**
     * Supprime un morceau par son ID.
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
        try (FileWriter fw = new FileWriter(FICHIER, false)) {
            for (String l : lignes) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    // ==================== UTILITAIRES ====================

    private static int genererNouvelId() {
        int max = 0;
        for (Morceau m : chargerTous()) {
            if (m.getId() > max) max = m.getId();
        }
        return max + 1;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + titre + " - " + getNomInterprete()
                + " (" + annee + ") [" + genre + "] " + getDureeFormatee();
    }

    // ==================== NOTES ====================

    private static final String FICHIER_NOTES = "notes.txt";

    /**
     * Enregistre ou met à jour la note d'un abonné pour ce morceau.
     * La note doit être entre 1 et 5.
     * Format du fichier : idMorceau;idClient;note
     */
    public static boolean noterMorceau(int idMorceau, int idClient, int note) {
        if (note < 1 || note > 5) return false;

        List<String> lignes = new ArrayList<>();
        boolean dejaNote = false;

        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_NOTES))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 3) continue;
                int idM = Integer.parseInt(p[0]);
                int idC = Integer.parseInt(p[1]);
                if (idM == idMorceau && idC == idClient) {
                    // Mise à jour de la note existante
                    lignes.add(idMorceau + ";" + idClient + ";" + note);
                    dejaNote = true;
                } else {
                    lignes.add(ligne);
                }
            }
        } catch (FileNotFoundException e) {
            // fichier pas encore créé, on continue
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!dejaNote) {
            lignes.add(idMorceau + ";" + idClient + ";" + note);
        }

        try (FileWriter fw = new FileWriter(FICHIER_NOTES, false)) {
            for (String l : lignes) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Retourne la note moyenne d'un morceau (0.0 si aucune note).
     */
    public static double getNoteMoyenne(int idMorceau) {
        int total = 0;
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_NOTES))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 3) continue;
                if (Integer.parseInt(p[0]) == idMorceau) {
                    total += Integer.parseInt(p[2]);
                    count++;
                }
            }
        } catch (FileNotFoundException e) {
            // pas encore de notes
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (count == 0) return 0.0;
        return (double) total / count;
    }

    /**
     * Retourne le nombre de votes pour un morceau.
     */
    public static int getNombreVotes(int idMorceau) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_NOTES))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 3) continue;
                if (Integer.parseInt(p[0]) == idMorceau) count++;
            }
        } catch (FileNotFoundException e) {
            // pas encore de notes
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Retourne la note donnée par un client pour ce morceau, ou 0 si pas encore noté.
     */
    public static int getNoteClient(int idMorceau, int idClient) {
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_NOTES))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 3) continue;
                if (Integer.parseInt(p[0]) == idMorceau && Integer.parseInt(p[1]) == idClient) {
                    return Integer.parseInt(p[2]);
                }
            }
        } catch (FileNotFoundException e) {
            // pas encore de notes
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}