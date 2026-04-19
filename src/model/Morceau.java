package model;

import java.io.*;
import java.util.*;

/**
 * Représente un morceau musical dans le catalogue de la plateforme.
 * <p>
 * Un morceau est interprété soit par un artiste solo, soit par un groupe.
 * Les deux cas sont mutuellement exclusifs : {@code idArtiste} vaut {@code 0}
 * pour un groupe, et {@code idGroupe} vaut {@code 0} pour un artiste solo.
 * </p>
 * <p>
 * <strong>Format de persistance ({@code morceaux.txt}) :</strong><br>
 * {@code id;titre;duree;genre;annee;idArtiste;idGroupe}
 * </p>
 * <p>
 * Les notes des morceaux sont stockées dans {@code notes.txt} au format :<br>
 * {@code idMorceau;idClient;note} (note entre 1 et 5).
 * </p>
 *
 * @see Artiste
 * @see Groupe
 * @see Album
 */
public class Morceau {

    /** Chemin du fichier de persistance des morceaux. */
    private static final String FICHIER = "morceaux.txt";

    /** Chemin du fichier de persistance des notes. */
    private static final String FICHIER_NOTES = "notes.txt";

    /** Identifiant unique du morceau. */
    private int id;

    /** Titre du morceau. */
    private String titre;

    /** Durée du morceau en secondes. */
    private int duree;

    /** Genre musical du morceau (ex. Rock, Jazz, Pop…). */
    private String genre;

    /** Année de sortie du morceau. */
    private int annee;

    /** Identifiant de l'artiste solo interprète ; {@code 0} si c'est un groupe. */
    private int idArtiste;

    /** Identifiant du groupe interprète ; {@code 0} si c'est un artiste solo. */
    private int idGroupe;

    /**
     * Construit un morceau avec tous ses attributs.
     *
     * @param id        identifiant unique
     * @param titre     titre du morceau
     * @param duree     durée en secondes
     * @param genre     genre musical
     * @param annee     année de sortie
     * @param idArtiste ID de l'artiste solo ({@code 0} si groupe)
     * @param idGroupe  ID du groupe ({@code 0} si artiste solo)
     */
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

    /**
     * Retourne l'identifiant unique du morceau.
     * @return l'ID du morceau
     */
    public int getId() { return id; }

    /**
     * Retourne le titre du morceau.
     * @return le titre
     */
    public String getTitre() { return titre; }

    /**
     * Retourne la durée du morceau en secondes.
     * @return la durée en secondes
     */
    public int getDuree() { return duree; }

    /**
     * Retourne le genre musical du morceau.
     * @return le genre
     */
    public String getGenre() { return genre; }

    /**
     * Retourne l'année de sortie du morceau.
     * @return l'année
     */
    public int getAnnee() { return annee; }

    /**
     * Retourne l'identifiant de l'artiste solo interprète.
     * Vaut {@code 0} si le morceau est interprété par un groupe.
     * @return l'ID de l'artiste
     */
    public int getIdArtiste() { return idArtiste; }

    /**
     * Retourne l'identifiant du groupe interprète.
     * Vaut {@code 0} si le morceau est interprété par un artiste solo.
     * @return l'ID du groupe
     */
    public int getIdGroupe() { return idGroupe; }

    /**
     * Modifie le titre du morceau.
     * @param titre le nouveau titre
     */
    public void setTitre(String titre) { this.titre = titre; }

    /**
     * Modifie la durée du morceau.
     * @param duree la nouvelle durée en secondes
     */
    public void setDuree(int duree) { this.duree = duree; }

    /**
     * Modifie le genre musical du morceau.
     * @param genre le nouveau genre
     */
    public void setGenre(String genre) { this.genre = genre; }

    /**
     * Retourne le nom de l'interprète du morceau.
     * <p>
     * Recherche en priorité le groupe ; si aucun groupe n'est associé,
     * retourne le nom complet de l'artiste solo. Retourne {@code "Inconnu"}
     * si ni l'un ni l'autre n'est défini.
     * </p>
     *
     * @return le nom du groupe ou de l'artiste interprète
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
     * Retourne la durée du morceau formatée en {@code mm:ss}.
     *
     * @return chaîne au format {@code "m:ss"} (ex. {@code "3:45"})
     */
    public String getDureeFormatee() {
        int min = duree / 60;
        int sec = duree % 60;
        return String.format("%d:%02d", min, sec);
    }

    // ==================== PERSISTENCE ====================

    /**
     * Charge et retourne tous les morceaux depuis {@code morceaux.txt}.
     * <p>
     * Si le fichier n'existe pas encore, retourne une liste vide.
     * </p>
     *
     * @return liste de tous les morceaux du catalogue
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
     * Recherche un morceau par son identifiant.
     *
     * @param idCible l'ID du morceau recherché
     * @return le {@link Morceau} correspondant, ou {@code null} si introuvable
     */
    public static Morceau rechercherParId(int idCible) {
        for (Morceau m : chargerTous()) {
            if (m.getId() == idCible) return m;
        }
        return null;
    }

    /**
     * Recherche des morceaux dont le titre contient la chaîne fournie
     * (recherche partielle, insensible à la casse).
     *
     * @param recherche la chaîne à chercher dans les titres
     * @return liste des morceaux correspondants (peut être vide)
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
     * Recherche globale dans le catalogue par titre, nom d'interprète ou genre.
     * La recherche est partielle et insensible à la casse.
     *
     * @param recherche la chaîne à chercher
     * @return liste des morceaux correspondant à au moins un des critères
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
     * Retourne tous les morceaux d'un artiste solo donné.
     *
     * @param idArtiste l'ID de l'artiste
     * @return liste des morceaux de cet artiste (peut être vide)
     */
    public static List<Morceau> getMorceauxParArtiste(int idArtiste) {
        List<Morceau> resultats = new ArrayList<>();
        for (Morceau m : chargerTous()) {
            if (m.getIdArtiste() == idArtiste) resultats.add(m);
        }
        return resultats;
    }

    /**
     * Retourne tous les morceaux d'un groupe donné.
     *
     * @param idGroupe l'ID du groupe
     * @return liste des morceaux de ce groupe (peut être vide)
     */
    public static List<Morceau> getMorceauxParGroupe(int idGroupe) {
        List<Morceau> resultats = new ArrayList<>();
        for (Morceau m : chargerTous()) {
            if (m.getIdGroupe() == idGroupe) resultats.add(m);
        }
        return resultats;
    }

    /**
     * Retourne tous les morceaux appartenant à un genre musical donné
     * (comparaison insensible à la casse).
     *
     * @param genre le genre recherché
     * @return liste des morceaux de ce genre (peut être vide)
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
     * Retourne la liste triée (ordre alphabétique) de tous les genres musicaux
     * distincts présents dans le catalogue.
     *
     * @return liste des genres disponibles
     */
    public static List<String> getGenresDisponibles() {
        Set<String> genres = new TreeSet<>();
        for (Morceau m : chargerTous()) {
            genres.add(m.getGenre());
        }
        return new ArrayList<>(genres);
    }

    /**
     * Ajoute un nouveau morceau dans le catalogue et le persiste dans
     * {@code morceaux.txt}.
     *
     * @param titre     titre du morceau
     * @param duree     durée en secondes
     * @param genre     genre musical
     * @param annee     année de sortie
     * @param idArtiste ID de l'artiste solo ({@code 0} si groupe)
     * @param idGroupe  ID du groupe ({@code 0} si artiste solo)
     * @return l'objet {@link Morceau} créé avec son nouvel ID
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
     * Supprime un morceau du catalogue en retirant sa ligne de {@code morceaux.txt}.
     *
     * @param idCible l'ID du morceau à supprimer
     * @return {@code true} si la suppression a réussi, {@code false} si l'ID
     *         est introuvable ou en cas d'erreur d'I/O
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

    /**
     * Génère un nouvel ID unique en retournant le maximum des IDs existants + 1.
     *
     * @return le prochain ID disponible
     */
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

    /**
     * Enregistre ou met à jour la note attribuée par un client à ce morceau.
     * <p>
     * La note doit être comprise entre 1 et 5 inclus. Si le client a déjà noté
     * ce morceau, l'ancienne note est remplacée. Les notes sont persistées dans
     * {@code notes.txt}.
     * </p>
     *
     * @param idMorceau l'ID du morceau à noter
     * @param idClient  l'ID du client qui note
     * @param note      la note attribuée (entre 1 et 5 inclus)
     * @return {@code true} si la note a été enregistrée, {@code false} si la note
     *         est hors plage ou en cas d'erreur d'I/O
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
     * Calcule et retourne la note moyenne d'un morceau.
     *
     * @param idMorceau l'ID du morceau
     * @return la note moyenne (entre 1.0 et 5.0), ou {@code 0.0} si aucune note
     *         n'a encore été attribuée
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
     * Retourne le nombre total de votes (notations) reçus par un morceau.
     *
     * @param idMorceau l'ID du morceau
     * @return le nombre de votes, ou {@code 0} si aucun
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
     * Retourne la liste de tous les morceaux ayant reçu au moins une note,
     * triée par note moyenne décroissante.
     * <p>
     * Chaque entrée du tableau {@code int[]} contient :
     * <ul>
     *   <li>index 0 : ID du morceau</li>
     *   <li>index 1 : note moyenne × 10 (pour le tri entier)</li>
     *   <li>index 2 : nombre de votes</li>
     * </ul>
     * </p>
     *
     * @return liste triée par note décroissante des morceaux notés
     */
    public static List<int[]> getMorceauxNotes() {
        java.util.Map<Integer, int[]> map = new java.util.LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_NOTES))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] p = ligne.split(";");
                if (p.length < 3) continue;
                int idM = Integer.parseInt(p[0]);
                int note = Integer.parseInt(p[2]);
                map.computeIfAbsent(idM, k -> new int[]{0, 0});
                map.get(idM)[0] += note;
                map.get(idM)[1]++;
            }
        } catch (FileNotFoundException e) {
            // pas de notes
        } catch (IOException e) { e.printStackTrace(); }

        List<int[]> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Integer, int[]> entry : map.entrySet()) {
            int id = entry.getKey();
            int total = entry.getValue()[0];
            int count = entry.getValue()[1];
            result.add(new int[]{id, total * 10 / count, count});
        }
        result.sort((a, b) -> b[1] - a[1]);
        return result;
    }

    /**
     * Retourne la note donnée par un client spécifique pour un morceau donné.
     *
     * @param idMorceau l'ID du morceau
     * @param idClient  l'ID du client
     * @return la note attribuée (entre 1 et 5), ou {@code 0} si le client
     *         n'a pas encore noté ce morceau
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