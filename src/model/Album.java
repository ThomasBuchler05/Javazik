package model;

import java.io.*;
import java.util.*;

/**
 * Représente un album musical dans le catalogue de la plateforme.
 * <p>
 * Un album est interprété soit par un artiste solo, soit par un groupe.
 * Les deux cas sont mutuellement exclusifs : {@code idArtiste} vaut {@code 0}
 * pour un groupe, et {@code idGroupe} vaut {@code 0} pour un artiste solo.
 * </p>
 * <p>
 * <strong>Format de persistance ({@code albums.txt}) :</strong><br>
 * {@code id;titre;annee;idArtiste;idGroupe}
 * </p>
 * <p>
 * <strong>Format des pistes ({@code album_morceaux.txt}) :</strong><br>
 * {@code idAlbum;idMorceau;numPiste}
 * </p>
 *
 * @see Morceau
 * @see Artiste
 * @see Groupe
 */
public class Album {

    /** Chemin du fichier de persistance des albums. */
    private static final String FICHIER = "albums.txt";

    /** Chemin du fichier de persistance des associations album ↔ morceau. */
    private static final String FICHIER_MORCEAUX = "album_morceaux.txt";

    /** Identifiant unique de l'album. */
    private int id;

    /** Titre de l'album. */
    private String titre;

    /** Année de sortie de l'album. */
    private int annee;

    /** Identifiant de l'artiste solo interprète ; {@code 0} si c'est un groupe. */
    private int idArtiste;

    /** Identifiant du groupe interprète ; {@code 0} si c'est un artiste solo. */
    private int idGroupe;

    /** Liste ordonnée des morceaux de l'album (par numéro de piste). */
    private List<Morceau> morceaux;

    /**
     * Construit un album avec tous ses attributs.
     * La liste des morceaux est initialisée vide.
     *
     * @param id        identifiant unique
     * @param titre     titre de l'album
     * @param annee     année de sortie
     * @param idArtiste ID de l'artiste solo ({@code 0} si groupe)
     * @param idGroupe  ID du groupe ({@code 0} si artiste solo)
     */
    public Album(int id, String titre, int annee, int idArtiste, int idGroupe) {
        this.id = id;
        this.titre = titre;
        this.annee = annee;
        this.idArtiste = idArtiste;
        this.idGroupe = idGroupe;
        this.morceaux = new ArrayList<>();
    }

    // ==================== GETTERS / SETTERS ====================

    /**
     * Retourne l'identifiant unique de l'album.
     * @return l'ID
     */
    public int getId() { return id; }

    /**
     * Retourne le titre de l'album.
     * @return le titre
     */
    public String getTitre() { return titre; }

    /**
     * Retourne l'année de sortie de l'album.
     * @return l'année
     */
    public int getAnnee() { return annee; }

    /**
     * Retourne l'identifiant de l'artiste solo interprète.
     * Vaut {@code 0} si l'album est interprété par un groupe.
     * @return l'ID de l'artiste
     */
    public int getIdArtiste() { return idArtiste; }

    /**
     * Retourne l'identifiant du groupe interprète.
     * Vaut {@code 0} si l'album est interprété par un artiste solo.
     * @return l'ID du groupe
     */
    public int getIdGroupe() { return idGroupe; }

    /**
     * Retourne la liste ordonnée des morceaux de l'album.
     * @return liste des morceaux (par numéro de piste)
     */
    public List<Morceau> getMorceaux() { return morceaux; }

    /**
     * Modifie le titre de l'album.
     * @param titre le nouveau titre
     */
    public void setTitre(String titre) { this.titre = titre; }

    /**
     * Définit la liste des morceaux de l'album.
     * @param morceaux la nouvelle liste de morceaux
     */
    public void setMorceaux(List<Morceau> morceaux) { this.morceaux = morceaux; }

    /**
     * Retourne le nom de l'interprète de l'album (artiste solo ou groupe).
     * <p>
     * Recherche en priorité le groupe ; si aucun groupe n'est associé,
     * retourne le nom complet de l'artiste solo.
     * </p>
     *
     * @return le nom du groupe ou de l'artiste interprète, ou {@code "Inconnu"}
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
     * Calcule et retourne la durée totale de l'album en secondes.
     * <p>
     * Correspond à la somme des durées de tous les morceaux de l'album.
     * </p>
     *
     * @return durée totale en secondes
     */
    public int getDureeTotale() {
        int total = 0;
        for (Morceau m : morceaux) {
            total += m.getDuree();
        }
        return total;
    }

    /**
     * Retourne la durée totale de l'album formatée en {@code "Xmin Ys"}.
     *
     * @return chaîne formatée (ex. {@code "42min 15s"})
     */
    public String getDureeTotaleFormatee() {
        int total = getDureeTotale();
        return (total / 60) + "min " + (total % 60) + "s";
    }

    // ==================== PERSISTENCE ====================

    /**
     * Charge et retourne tous les albums depuis {@code albums.txt},
     * en peuplant la liste des morceaux de chacun depuis {@code album_morceaux.txt}.
     * <p>
     * Si le fichier n'existe pas encore, retourne une liste vide.
     * </p>
     *
     * @return liste de tous les albums enregistrés (avec leurs morceaux)
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
     * Recherche un album par son identifiant.
     *
     * @param idCible l'ID de l'album recherché
     * @return l'{@link Album} correspondant, ou {@code null} si introuvable
     */
    public static Album rechercherParId(int idCible) {
        for (Album a : chargerTous()) {
            if (a.getId() == idCible) return a;
        }
        return null;
    }

    /**
     * Recherche des albums dont le titre contient la chaîne fournie
     * (recherche partielle, insensible à la casse).
     *
     * @param recherche la chaîne à chercher dans les titres
     * @return liste des albums correspondants (peut être vide)
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
     * Retourne tous les albums d'un artiste solo donné.
     *
     * @param idArtiste l'ID de l'artiste
     * @return liste des albums de cet artiste (peut être vide)
     */
    public static List<Album> getAlbumsParArtiste(int idArtiste) {
        List<Album> resultats = new ArrayList<>();
        for (Album a : chargerTous()) {
            if (a.getIdArtiste() == idArtiste) resultats.add(a);
        }
        return resultats;
    }

    /**
     * Retourne tous les albums d'un groupe donné.
     *
     * @param idGroupe l'ID du groupe
     * @return liste des albums de ce groupe (peut être vide)
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
     * <p>
     * Parcourt {@code album_morceaux.txt} pour trouver les albums associés
     * au morceau spécifié.
     * </p>
     *
     * @param idMorceau l'ID du morceau recherché
     * @return liste des albums contenant ce morceau (peut être vide)
     */
    public static List<Album> getAlbumsParMorceau(int idMorceau) {
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
     * Ajoute un nouvel album dans le catalogue et le persiste dans
     * {@code albums.txt}.
     *
     * @param titre     titre de l'album
     * @param annee     année de sortie
     * @param idArtiste ID de l'artiste solo ({@code 0} si groupe)
     * @param idGroupe  ID du groupe ({@code 0} si artiste solo)
     * @return l'objet {@link Album} créé avec son nouvel ID
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
     * Supprime un album du catalogue ainsi que toutes ses associations de morceaux.
     *
     * @param idCible l'ID de l'album à supprimer
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
        supprimerAssociationsMorceaux(idCible);
        return true;
    }

    /**
     * Ajoute un morceau dans un album avec son numéro de piste.
     * <p>
     * L'association est persistée dans {@code album_morceaux.txt}.
     * </p>
     *
     * @param idAlbum   l'ID de l'album
     * @param idMorceau l'ID du morceau à ajouter
     * @param numPiste  le numéro de piste dans l'album (commence à 1)
     * @return {@code true} si l'ajout a réussi, {@code false} en cas d'erreur d'I/O
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

    /**
     * Charge les morceaux d'un album depuis {@code album_morceaux.txt},
     * triés par numéro de piste croissant.
     *
     * @param idAlbum l'ID de l'album
     * @return liste des morceaux dans l'ordre des pistes
     */
    private static List<Morceau> chargerMorceauxAlbum(int idAlbum) {
        List<int[]> pistes = new ArrayList<>();
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

        pistes.sort(Comparator.comparingInt(a -> a[1]));

        List<Morceau> morceaux = new ArrayList<>();
        for (int[] piste : pistes) {
            Morceau m = Morceau.rechercherParId(piste[0]);
            if (m != null) morceaux.add(m);
        }
        return morceaux;
    }

    /**
     * Génère un nouvel ID unique en retournant le maximum des IDs existants + 1.
     *
     * @return le prochain ID disponible
     */
    private static int genererNouvelId() {
        int max = 0;
        for (Album a : chargerTous()) {
            if (a.getId() > max) max = a.getId();
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
     * Supprime toutes les associations pistes de l'album donné dans
     * {@code album_morceaux.txt}.
     *
     * @param idAlbum l'ID de l'album dont les associations sont à supprimer
     */
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

    @Override
    public String toString() {
        return "[" + id + "] " + titre + " (" + annee + ") - " + getNomInterprete()
                + " | " + morceaux.size() + " morceau(x), " + getDureeTotaleFormatee();
    }
}