package model;

import java.io.*;
import java.util.*;

/**
 * Représente un artiste musical (interprète solo ou membre d'un groupe).
 * <p>
 * Un artiste peut être lié à un ou plusieurs {@link Groupe}s via le fichier
 * {@code groupe_artistes.txt}, et peut avoir composé ou interprété des
 * {@link Morceau}x et des {@link Album}s de façon indépendante.
 * </p>
 * <p>
 * <strong>Format de persistance ({@code artistes.txt}) :</strong><br>
 * {@code id;nom;prenom;nationalite}
 * </p>
 *
 * @see Groupe
 * @see Morceau
 * @see Album
 */
public class Artiste {

    /** Chemin du fichier de persistance des artistes. */
    private static final String FICHIER = "artistes.txt";

    /** Identifiant unique de l'artiste. */
    private int id;

    /** Nom de famille de l'artiste. */
    private String nom;

    /** Prénom de l'artiste (peut être vide pour les artistes à nom unique). */
    private String prenom;

    /** Nationalité de l'artiste. */
    private String nationalite;

    /**
     * Construit un artiste avec tous ses attributs.
     *
     * @param id          identifiant unique
     * @param nom         nom de famille
     * @param prenom      prénom (peut être vide)
     * @param nationalite nationalité
     */
    public Artiste(int id, String nom, String prenom, String nationalite) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.nationalite = nationalite;
    }

    // ==================== GETTERS / SETTERS ====================

    /**
     * Retourne l'identifiant unique de l'artiste.
     * @return l'ID
     */
    public int getId() { return id; }

    /**
     * Retourne le nom de famille de l'artiste.
     * @return le nom
     */
    public String getNom() { return nom; }

    /**
     * Retourne le prénom de l'artiste.
     * @return le prénom (peut être vide)
     */
    public String getPrenom() { return prenom; }

    /**
     * Retourne la nationalité de l'artiste.
     * @return la nationalité
     */
    public String getNationalite() { return nationalite; }

    /**
     * Modifie le nom de famille de l'artiste.
     * @param nom le nouveau nom
     */
    public void setNom(String nom) { this.nom = nom; }

    /**
     * Modifie le prénom de l'artiste.
     * @param prenom le nouveau prénom
     */
    public void setPrenom(String prenom) { this.prenom = prenom; }

    /**
     * Modifie la nationalité de l'artiste.
     * @param nationalite la nouvelle nationalité
     */
    public void setNationalite(String nationalite) { this.nationalite = nationalite; }

    /**
     * Retourne le nom complet de l'artiste au format {@code "prénom nom"}.
     * <p>
     * Si le prénom est absent ou vide, retourne uniquement le nom.
     * </p>
     *
     * @return le nom complet de l'artiste
     */
    public String getNomComplet() {
        if (prenom == null || prenom.isEmpty()) {
            return nom;
        }
        return prenom + " " + nom;
    }

    // ==================== PERSISTENCE ====================

    /**
     * Charge et retourne tous les artistes depuis {@code artistes.txt}.
     * <p>
     * Si le fichier n'existe pas encore, retourne une liste vide.
     * </p>
     *
     * @return liste de tous les artistes enregistrés
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
     * Recherche un artiste par son identifiant.
     *
     * @param idCible l'ID de l'artiste recherché
     * @return l'{@link Artiste} correspondant, ou {@code null} si introuvable
     */
    public static Artiste rechercherParId(int idCible) {
        for (Artiste a : chargerTous()) {
            if (a.getId() == idCible) return a;
        }
        return null;
    }

    /**
     * Recherche des artistes dont le nom, le prénom ou le nom complet contient
     * la chaîne fournie (recherche partielle, insensible à la casse).
     *
     * @param recherche la chaîne à chercher
     * @return liste des artistes correspondants (peut être vide)
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
     * Ajoute un nouvel artiste dans le catalogue et le persiste dans
     * {@code artistes.txt}.
     *
     * @param nom         nom de famille
     * @param prenom      prénom (peut être vide)
     * @param nationalite nationalité
     * @return l'objet {@link Artiste} créé avec son nouvel ID
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
     * Supprime un artiste du catalogue en retirant sa ligne de {@code artistes.txt}.
     *
     * @param idCible l'ID de l'artiste à supprimer
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
        ecrireFichier(lignes);
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
        for (Artiste a : chargerTous()) {
            if (a.getId() > max) max = a.getId();
        }
        return max + 1;
    }

    /**
     * Réécrit entièrement {@code artistes.txt} avec la liste de lignes fournie.
     *
     * @param lignes les lignes à écrire dans le fichier
     */
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