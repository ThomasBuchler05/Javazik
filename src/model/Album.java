package model;

import java.io.*;
import java.util.*;

/**
 * Représente un album musical.
 * Persistance dans albums.txt au format : id;titre;annee;idArtiste;idGroupe
 * (idArtiste=0 si c'est un groupe, idGroupe=0 si c'est un artiste solo)
 * Les morceaux de l'album sont dans album_morceaux.txt au format : idAlbum;idMorceau;numPiste
 */
public class Album {

    private static final String FICHIER = "albums.txt";
    private static final String FICHIER_MORCEAUX = "album_morceaux.txt";

    private int id;
    private String titre;
    private int annee;
    private int idArtiste;   // 0 si c'est un groupe
    private int idGroupe;    // 0 si c'est un artiste solo
    private List<Morceau> morceaux;

    public Album(int id, String titre, int annee, int idArtiste, int idGroupe) {
        this.id = id;
        this.titre = titre;
        this.annee = annee;
        this.idArtiste = idArtiste;
        this.idGroupe = idGroupe;
        this.morceaux = new ArrayList<>();
    }

    // ==================== GETTERS / SETTERS ====================

    public int getId() { return id; }
    public String getTitre() { return titre; }
    public int getAnnee() { return annee; }
    public int getIdArtiste() { return idArtiste; }
    public int getIdGroupe() { return idGroupe; }
    public List<Morceau> getMorceaux() { return morceaux; }

    public void setTitre(String titre) { this.titre = titre; }
    public void setMorceaux(List<Morceau> morceaux) { this.morceaux = morceaux; }

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
     * Calcule la durée totale de l'album en secondes.
     */
    public int getDureeTotale() {
        int total = 0;
        for (Morceau m : morceaux) {
            total += m.getDuree();
        }
        return total;
    }

    // ==================== PERSISTENCE ====================

    /**
     * Charge tous les albums depuis le fichier (avec leurs morceaux).
     */
    public static List<Album> chargerTous() {
        List<Album> liste = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 5) continue;
                Album a = new Album(
                    Integer.parseInt(p[0]), p[1],
                    Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]),
                    Integer.parseInt(p[4])
                );
                a.setMorceaux(chargerMorceauxAlbum(a.getId()));
                liste.add(a);
            }
        } catch (FileNotFoundException e) {
            // fichier pas encore créé
        } catch (IOException e) {
            e.printStackTrace();
        }
        return liste;
    }

    /**
     * Recherche un album par son ID.
     */
    public static Album rechercherParId(int idCible) {
        for (Album a : chargerTous()) {
            if (a.getId() == idCible) return a;
        }
        return null;
    }

    /**
     * Recherche des albums par titre (partiel, insensible à la casse).
     */
    public static List<Album> rechercherParTitre(String recherche) {
        List<Album> resultats = new ArrayList<>();
        String rech = recherche.toLowerCase();
        for (Album a : chargerTous()) {
            if (a.getTitre().toLowerCase().contains(rech)) {
                resultats.add(a);
            }
        }
        return resultats;
    }

    /**
     * Retourne tous les albums d'un artiste solo.
     */
    public static List<Album> getAlbumsParArtiste(int idArtiste) {
        List<Album> resultats = new ArrayList<>();
        for (Album a : chargerTous()) {
            if (a.getIdArtiste() == idArtiste) resultats.add(a);
        }
        return resultats;
    }

    /**
     * Retourne tous les albums d'un groupe.
     */
    public static List<Album> getAlbumsParGroupe(int idGroupe) {
        List<Album> resultats = new ArrayList<>();
        for (Album a : chargerTous()) {
            if (a.getIdGroupe() == idGroupe) resultats.add(a);
        }
        return resultats;
    }

    /**
     * Retourne tous les albums contenant un morceau donné.
     */
    public static List<Album> getAlbumsParMorceau(int idMorceau) {
        // d'abord trouver les IDs d'albums contenant ce morceau
        Set<Integer> idsAlbums = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_MORCEAUX))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 2) continue;
                if (Integer.parseInt(p[1]) == idMorceau) {
                    idsAlbums.add(Integer.parseInt(p[0]));
                }
            }
        } catch (FileNotFoundException e) {
            // pas de fichier
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Album> resultats = new ArrayList<>();
        for (int idA : idsAlbums) {
            Album a = rechercherParId(idA);
            if (a != null) resultats.add(a);
        }
        return resultats;
    }

    /**
     * Ajoute un album et retourne l'objet créé.
     */
    public static Album ajouter(String titre, int annee, int idArtiste, int idGroupe) {
        int nouvelId = genererNouvelId();
        Album a = new Album(nouvelId, titre, annee, idArtiste, idGroupe);
        try (FileWriter fw = new FileWriter(FICHIER, true)) {
            fw.write(nouvelId + ";" + titre + ";" + annee + ";" + idArtiste + ";" + idGroupe + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return a;
    }

    /**
     * Supprime un album par son ID (et ses associations de morceaux).
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
        supprimerAssociationsMorceaux(idCible);
        return true;
    }

    /**
     * Ajoute un morceau dans un album.
     */
    public static boolean ajouterMorceau(int idAlbum, int idMorceau, int numPiste) {
        try (FileWriter fw = new FileWriter(FICHIER_MORCEAUX, true)) {
            fw.write(idAlbum + ";" + idMorceau + ";" + numPiste + "\n");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== UTILITAIRES PRIVES ====================

    private static List<Morceau> chargerMorceauxAlbum(int idAlbum) {
        List<int[]> pistes = new ArrayList<>(); // [idMorceau, numPiste]
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_MORCEAUX))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 3) continue;
                if (Integer.parseInt(p[0]) == idAlbum) {
                    pistes.add(new int[]{ Integer.parseInt(p[1]), Integer.parseInt(p[2]) });
                }
            }
        } catch (FileNotFoundException e) {
            // pas de fichier
        } catch (IOException e) {
            e.printStackTrace();
        }

        // trier par numéro de piste
        pistes.sort(Comparator.comparingInt(a -> a[1]));

        List<Morceau> morceaux = new ArrayList<>();
        for (int[] piste : pistes) {
            Morceau m = Morceau.rechercherParId(piste[0]);
            if (m != null) morceaux.add(m);
        }
        return morceaux;
    }

    private static int genererNouvelId() {
        int max = 0;
        for (Album a : chargerTous()) {
            if (a.getId() > max) max = a.getId();
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

    private static void supprimerAssociationsMorceaux(int idAlbum) {
        List<String> lignes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_MORCEAUX))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (Integer.parseInt(p[0]) != idAlbum) {
                    lignes.add(ligne);
                }
            }
        } catch (IOException e) {
            return;
        }
        ecrireFichier(FICHIER_MORCEAUX, lignes);
    }

    /**
     * Formate la durée totale en mm:ss.
     */
    public String getDureeTotaleFormatee() {
        int total = getDureeTotale();
        return (total / 60) + "min " + (total % 60) + "s";
    }

    @Override
    public String toString() {
        return "[" + id + "] " + titre + " (" + annee + ") - " + getNomInterprete()
               + " | " + morceaux.size() + " morceau(x), " + getDureeTotaleFormatee();
    }
}
