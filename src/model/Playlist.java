package model;

import java.io.*;
import java.util.*;

/**
 * Représente une playlist personnelle d'un client.
 * <p>
 * Une playlist contient une liste de {@link Morceau}x sélectionnés par le client.
 * Un même morceau ne peut pas apparaître plusieurs fois dans la même playlist.
 * </p>
 * <p>
 * <strong>Format de persistance ({@code playlists.txt}) :</strong><br>
 * {@code id;nom;idClient}
 * </p>
 * <p>
 * <strong>Format des morceaux ({@code playlist_morceaux.txt}) :</strong><br>
 * {@code idPlaylist;idMorceau}
 * </p>
 *
 * @see Morceau
 * @see Client
 */
public class Playlist {

    /** Identifiant unique de la playlist. */
    private int id;

    /** Nom de la playlist. */
    private String nom;

    /** Identifiant du client propriétaire de la playlist. */
    private int idClient;

    /** Liste des morceaux contenus dans la playlist. */
    private List<Morceau> morceaux;

    /** Chemin du fichier de persistance des playlists. */
    private static final String FICHIER_PLAYLISTS = "playlists.txt";

    /** Chemin du fichier de persistance des associations playlist ↔ morceau. */
    private static final String FICHIER_PLAYLIST_MORCEAUX = "playlist_morceaux.txt";

    /**
     * Construit une playlist avec les attributs de base.
     * La liste des morceaux est initialisée vide.
     *
     * @param id       identifiant unique
     * @param nom      nom de la playlist
     * @param idClient ID du client propriétaire
     */
    public Playlist(int id, String nom, int idClient) {
        this.id = id;
        this.nom = nom;
        this.idClient = idClient;
        this.morceaux = new ArrayList<>();
    }

    // ==================== GETTERS / SETTERS ====================

    /**
     * Retourne l'identifiant unique de la playlist.
     * @return l'ID
     */
    public int getId() { return id; }

    /**
     * Retourne le nom de la playlist.
     * @return le nom
     */
    public String getNom() { return nom; }

    /**
     * Modifie le nom de la playlist.
     * @param nom le nouveau nom
     */
    public void setNom(String nom) { this.nom = nom; }

    /**
     * Retourne l'identifiant du client propriétaire de la playlist.
     * @return l'ID du client
     */
    public int getIdClient() { return idClient; }

    /**
     * Retourne la liste des morceaux de la playlist.
     * @return liste des morceaux
     */
    public List<Morceau> getMorceaux() { return morceaux; }

    /**
     * Définit la liste des morceaux de la playlist.
     * @param morceaux la nouvelle liste de morceaux
     */
    public void setMorceaux(List<Morceau> morceaux) { this.morceaux = morceaux; }

    /**
     * Calcule et retourne la durée totale de la playlist en secondes.
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
     * Retourne la durée totale de la playlist formatée en {@code "Xmin Ys"}.
     *
     * @return chaîne formatée (ex. {@code "25min 30s"})
     */
    public String getDureeTotaleFormatee() {
        int total = getDureeTotale();
        return (total / 60) + "min " + (total % 60) + "s";
    }

    // ==================== CRUD PLAYLISTS ====================

    /**
     * Crée une nouvelle playlist pour un client et la persiste dans
     * {@code playlists.txt}.
     *
     * @param nom      le nom de la nouvelle playlist
     * @param idClient l'ID du client propriétaire
     * @return l'objet {@link Playlist} créé avec son nouvel ID
     */
    public static Playlist creer(String nom, int idClient) {
        int nouvelId = genererNouvelId();

        try (FileWriter fw = new FileWriter(FICHIER_PLAYLISTS, true)) {
            fw.write(nouvelId + ";" + nom + ";" + idClient + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Playlist(nouvelId, nom, idClient);
    }

    /**
     * Retourne toutes les playlists d'un client donné, avec leurs morceaux chargés.
     *
     * @param idClient l'ID du client
     * @return liste des playlists du client (peut être vide)
     */
    public static List<Playlist> getPlaylistsClient(int idClient) {
        List<Playlist> liste = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_PLAYLISTS))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (parts.length < 3) continue;
                int id = Integer.parseInt(parts[0]);
                String nom = parts[1];
                int idCli = Integer.parseInt(parts[2]);
                if (idCli == idClient) {
                    Playlist p = new Playlist(id, nom, idCli);
                    p.setMorceaux(getMorceauxDansPlaylist(id));
                    liste.add(p);
                }
            }
        } catch (FileNotFoundException e) {
            // fichier pas encore créé
        } catch (IOException e) {
            e.printStackTrace();
        }

        return liste;
    }

    /**
     * Renomme une playlist appartenant à un client.
     *
     * @param idPlaylist  l'ID de la playlist à renommer
     * @param idClient    l'ID du client propriétaire (vérification d'appartenance)
     * @param nouveauNom  le nouveau nom
     * @return {@code true} si le renommage a réussi, {@code false} si la playlist
     *         est introuvable, n'appartient pas au client, ou en cas d'erreur d'I/O
     */
    public static boolean renommer(int idPlaylist, int idClient, String nouveauNom) {
        List<String> lignes = new ArrayList<>();
        boolean trouve = false;

        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_PLAYLISTS))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                int id = Integer.parseInt(parts[0]);
                int idCli = Integer.parseInt(parts[2]);
                if (id == idPlaylist && idCli == idClient) {
                    lignes.add(id + ";" + nouveauNom + ";" + idCli);
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

        try (FileWriter fw = new FileWriter(FICHIER_PLAYLISTS, false)) {
            for (String l : lignes) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Supprime une playlist appartenant à un client ainsi que toutes ses
     * associations de morceaux.
     *
     * @param idPlaylist l'ID de la playlist à supprimer
     * @param idClient   l'ID du client propriétaire (vérification d'appartenance)
     * @return {@code true} si la suppression a réussi, {@code false} si la playlist
     *         est introuvable, n'appartient pas au client, ou en cas d'erreur d'I/O
     */
    public static boolean supprimer(int idPlaylist, int idClient) {
        List<String> lignesAGarder = new ArrayList<>();
        boolean trouve = false;

        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_PLAYLISTS))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                int id = Integer.parseInt(parts[0]);
                int idCli = Integer.parseInt(parts[2]);
                if (id == idPlaylist && idCli == idClient) {
                    trouve = true;
                } else {
                    lignesAGarder.add(ligne);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!trouve) return false;

        try (FileWriter fw = new FileWriter(FICHIER_PLAYLISTS, false)) {
            for (String l : lignesAGarder) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        supprimerAssociationsPlaylist(idPlaylist);
        return true;
    }

    // ==================== GESTION DES MORCEAUX DANS PLAYLIST ====================

    /**
     * Ajoute un morceau dans une playlist.
     * <p>
     * Si le morceau est déjà présent dans la playlist, l'opération est ignorée
     * et {@code false} est retourné.
     * </p>
     *
     * @param idPlaylist l'ID de la playlist
     * @param idMorceau  l'ID du morceau à ajouter
     * @return {@code true} si l'ajout a réussi, {@code false} si le morceau
     *         est déjà dans la playlist ou en cas d'erreur d'I/O
     */
    public static boolean ajouterMorceau(int idPlaylist, int idMorceau) {
        List<Morceau> existants = getMorceauxDansPlaylist(idPlaylist);
        for (Morceau m : existants) {
            if (m.getId() == idMorceau) {
                return false;
            }
        }

        try (FileWriter fw = new FileWriter(FICHIER_PLAYLIST_MORCEAUX, true)) {
            fw.write(idPlaylist + ";" + idMorceau + "\n");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retire un morceau d'une playlist.
     *
     * @param idPlaylist l'ID de la playlist
     * @param idMorceau  l'ID du morceau à retirer
     * @return {@code true} si le retrait a réussi, {@code false} si le morceau
     *         n'est pas dans la playlist ou en cas d'erreur d'I/O
     */
    public static boolean retirerMorceau(int idPlaylist, int idMorceau) {
        List<String> lignesAGarder = new ArrayList<>();
        boolean trouve = false;

        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_PLAYLIST_MORCEAUX))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                int idP = Integer.parseInt(parts[0]);
                int idM = Integer.parseInt(parts[1]);
                if (idP == idPlaylist && idM == idMorceau) {
                    trouve = true;
                } else {
                    lignesAGarder.add(ligne);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!trouve) return false;

        try (FileWriter fw = new FileWriter(FICHIER_PLAYLIST_MORCEAUX, false)) {
            for (String l : lignesAGarder) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Retourne la liste des morceaux contenus dans une playlist donnée.
     *
     * @param idPlaylist l'ID de la playlist
     * @return liste des morceaux de la playlist (peut être vide)
     */
    public static List<Morceau> getMorceauxDansPlaylist(int idPlaylist) {
        List<Morceau> morceaux = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_PLAYLIST_MORCEAUX))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (parts.length < 2) continue;
                int idP = Integer.parseInt(parts[0]);
                int idM = Integer.parseInt(parts[1]);
                if (idP == idPlaylist) {
                    Morceau m = Morceau.rechercherParId(idM);
                    if (m != null) morceaux.add(m);
                }
            }
        } catch (FileNotFoundException e) {
            // fichier pas encore créé
        } catch (IOException e) {
            e.printStackTrace();
        }

        return morceaux;
    }

    // ==================== UTILITAIRES ====================

    /**
     * Génère un nouvel ID unique en retournant le maximum des IDs existants + 1.
     *
     * @return le prochain ID disponible
     */
    private static int genererNouvelId() {
        int max = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_PLAYLISTS))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                int id = Integer.parseInt(parts[0]);
                if (id > max) max = id;
            }
        } catch (FileNotFoundException e) {
            // premier ID sera 1
        } catch (IOException e) {
            e.printStackTrace();
        }
        return max + 1;
    }

    /**
     * Supprime toutes les associations morceaux de la playlist donnée dans
     * {@code playlist_morceaux.txt}.
     *
     * @param idPlaylist l'ID de la playlist dont les associations sont à supprimer
     */
    private static void supprimerAssociationsPlaylist(int idPlaylist) {
        List<String> lignesAGarder = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_PLAYLIST_MORCEAUX))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (Integer.parseInt(parts[0]) != idPlaylist) {
                    lignesAGarder.add(ligne);
                }
            }
        } catch (IOException e) {
            return;
        }
        try (FileWriter fw = new FileWriter(FICHIER_PLAYLIST_MORCEAUX, false)) {
            for (String l : lignesAGarder) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nom + " (" + morceaux.size() + " morceau(x), "
                + getDureeTotaleFormatee() + ")";
    }
}