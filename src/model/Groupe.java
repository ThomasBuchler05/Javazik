package model;

import java.io.*;
import java.util.*;

/**
 * Représente un groupe musical composé d'un ou plusieurs {@link Artiste}s.
 * <p>
 * La relation groupe ↔ membres est persistée dans {@code groupe_artistes.txt}.
 * Un groupe peut avoir des {@link Morceau}x et des {@link Album}s qui lui sont
 * rattachés directement.
 * </p>
 * <p>
 * <strong>Format de persistance ({@code groupes.txt}) :</strong><br>
 * {@code id;nom;dateCreation;nationalite}
 * </p>
 * <p>
 * <strong>Format des membres ({@code groupe_artistes.txt}) :</strong><br>
 * {@code idGroupe;idArtiste}
 * </p>
 *
 * @see Artiste
 * @see Morceau
 * @see Album
 */
public class Groupe {

    /** Chemin du fichier de persistance des groupes. */
    private static final String FICHIER = "groupes.txt";

    /** Chemin du fichier de persistance des associations groupe ↔ artiste. */
    private static final String FICHIER_MEMBRES = "groupe_artistes.txt";

    /** Identifiant unique du groupe. */
    private int id;

    /** Nom du groupe. */
    private String nom;

    /** Année de création du groupe. */
    private int dateCreation;

    /** Nationalité du groupe. */
    private String nationalite;

    /** Liste des artistes membres du groupe. */
    private List<Artiste> membres;

    /**
     * Construit un groupe avec tous ses attributs.
     * La liste des membres est initialisée vide et doit être peuplée séparément.
     *
     * @param id           identifiant unique
     * @param nom          nom du groupe
     * @param dateCreation année de création
     * @param nationalite  nationalité du groupe
     */
    public Groupe(int id, String nom, int dateCreation, String nationalite) {
        this.id = id;
        this.nom = nom;
        this.dateCreation = dateCreation;
        this.nationalite = nationalite;
        this.membres = new ArrayList<>();
    }

    // ==================== GETTERS / SETTERS ====================

    /**
     * Retourne l'identifiant unique du groupe.
     * @return l'ID
     */
    public int getId() { return id; }

    /**
     * Retourne le nom du groupe.
     * @return le nom
     */
    public String getNom() { return nom; }

    /**
     * Retourne l'année de création du groupe.
     * @return l'année de création
     */
    public int getDateCreation() { return dateCreation; }

    /**
     * Retourne la nationalité du groupe.
     * @return la nationalité
     */
    public String getNationalite() { return nationalite; }

    /**
     * Retourne la liste des artistes membres du groupe.
     * @return liste des membres
     */
    public List<Artiste> getMembres() { return membres; }

    /**
     * Modifie le nom du groupe.
     * @param nom le nouveau nom
     */
    public void setNom(String nom) { this.nom = nom; }

    /**
     * Définit la liste des membres du groupe.
     * @param membres la nouvelle liste d'artistes membres
     */
    public void setMembres(List<Artiste> membres) { this.membres = membres; }

    // ==================== PERSISTENCE ====================

    /**
     * Charge et retourne tous les groupes depuis {@code groupes.txt},
     * en peuplant la liste des membres de chacun depuis {@code groupe_artistes.txt}.
     * <p>
     * Si le fichier n'existe pas encore, retourne une liste vide.
     * </p>
     *
     * @return liste de tous les groupes enregistrés (avec leurs membres)
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
     * Recherche un groupe par son identifiant.
     *
     * @param idCible l'ID du groupe recherché
     * @return le {@link Groupe} correspondant, ou {@code null} si introuvable
     */
    public static Groupe rechercherParId(int idCible) {
        for (Groupe g : chargerTous()) {
            if (g.getId() == idCible) return g;
        }
        return null;
    }

    /**
     * Recherche des groupes dont le nom contient la chaîne fournie
     * (recherche partielle, insensible à la casse).
     *
     * @param recherche la chaîne à chercher dans les noms de groupes
     * @return liste des groupes correspondants (peut être vide)
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
     * Ajoute un nouveau groupe dans le catalogue et le persiste dans
     * {@code groupes.txt}.
     *
     * @param nom          nom du groupe
     * @param dateCreation année de création
     * @param nationalite  nationalité
     * @return l'objet {@link Groupe} créé avec son nouvel ID
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
     * Supprime un groupe du catalogue ainsi que toutes ses associations de membres.
     *
     * @param idCible l'ID du groupe à supprimer
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
        ecrireFichier(FICHIER, lignes);
        supprimerAssociationsMembres(idCible);
        return true;
    }

    /**
     * Ajoute un artiste comme membre du groupe dans {@code groupe_artistes.txt}.
     * <p>
     * Si l'artiste est déjà membre du groupe, l'opération est ignorée et
     * {@code false} est retourné.
     * </p>
     *
     * @param idGroupe  l'ID du groupe
     * @param idArtiste l'ID de l'artiste à ajouter
     * @return {@code true} si l'ajout a réussi, {@code false} si l'artiste
     *         était déjà membre ou en cas d'erreur d'I/O
     */
    public static boolean ajouterMembre(int idGroupe, int idArtiste) {
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
     * Retourne la liste de tous les groupes auxquels appartient un artiste donné.
     *
     * @param idArtiste l'ID de l'artiste
     * @return liste des groupes dont l'artiste est membre (peut être vide)
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

    /**
     * Charge et retourne les membres d'un groupe depuis {@code groupe_artistes.txt}.
     *
     * @param idGroupe l'ID du groupe
     * @return liste des artistes membres du groupe
     */
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

    /**
     * Génère un nouvel ID unique en retournant le maximum des IDs existants + 1.
     *
     * @return le prochain ID disponible
     */
    private static int genererNouvelId() {
        int max = 0;
        for (Groupe g : chargerTous()) {
            if (g.getId() > max) max = g.getId();
        }
        return max + 1;
    }

    /**
     * Réécrit entièrement un fichier de persistance avec la liste de lignes fournie.
     *
     * @param fichier le chemin du fichier à réécrire
     * @param lignes  les lignes à écrire
     */
    private static void ecrireFichier(String fichier, List<String> lignes) {
        try (FileWriter fw = new FileWriter(fichier, false)) {
            for (String l : lignes) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Supprime toutes les associations membres du groupe donné dans
     * {@code groupe_artistes.txt}.
     *
     * @param idGroupe l'ID du groupe dont les associations sont à supprimer
     */
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