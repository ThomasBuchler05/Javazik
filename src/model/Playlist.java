package model;

import java.io.*;
import java.util.*;

/**
 * Représente une playlist d'un abonné.
 * Persistance dans playlists.txt au format : id;nom;idClient
 * Morceaux dans playlist_morceaux.txt au format : idPlaylist;idMorceau
 */
public class Playlist {

    private int id;
    private String nom;
    private int idClient;
    private List<Morceau> morceaux;

    private static final String FICHIER_PLAYLISTS = "playlists.txt";
    private static final String FICHIER_PLAYLIST_MORCEAUX = "playlist_morceaux.txt";

    public Playlist(int id, String nom, int idClient) {
        this.id = id;
        this.nom = nom;
        this.idClient = idClient;
        this.morceaux = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getIdClient() { return idClient; }
    public List<Morceau> getMorceaux() { return morceaux; }
    public void setMorceaux(List<Morceau> morceaux) { this.morceaux = morceaux; }

    /**
     * Calcule la durée totale de la playlist en secondes.
     */
    public int getDureeTotale() {
        int total = 0;
        for (Morceau m : morceaux) {
            total += m.getDuree();
        }
        return total;
    }

    /**
     * Formate la durée totale en mm:ss.
     */
    public String getDureeTotaleFormatee() {
        int total = getDureeTotale();
        return (total / 60) + "min " + (total % 60) + "s";
    }

    // ==================== CRUD PLAYLISTS ====================

    /**
     * Crée une nouvelle playlist pour un client.
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
     * Retourne toutes les playlists d'un client donné.
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
     * Renomme une playlist.
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
     * Supprime une playlist (et ses associations de morceaux).
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
     */
    public static boolean ajouterMorceau(int idPlaylist, int idMorceau) {
        List<Morceau> existants = getMorceauxDansPlaylist(idPlaylist);
        for (Morceau m : existants) {
            if (m.getId() == idMorceau) {
                return false; // déjà présent
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
     * Retourne la liste des morceaux dans une playlist donnée.
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
