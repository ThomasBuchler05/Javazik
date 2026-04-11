package model;

import java.io.*;
import java.util.*;

public class Playlist {

    private int id;
    private String nom;
    private int idClient;
    private List<Musique> musiques;

    private static final String FICHIER_PLAYLISTS = "playlists.txt";
    private static final String FICHIER_PLAYLIST_MUSIQUES = "playlist_musiques.txt";

    public Playlist(int id, String nom, int idClient) {
        this.id = id;
        this.nom = nom;
        this.idClient = idClient;
        this.musiques = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public int getIdClient() { return idClient; }
    public List<Musique> getMusiques() { return musiques; }
    public void setMusiques(List<Musique> musiques) { this.musiques = musiques; }

    // ==================== CRUD PLAYLISTS ====================

    /**
     * Crée une nouvelle playlist pour un client et la sauvegarde dans playlists.txt
     * Format : id;nom;idClient
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
     * Retourne toutes les playlists d'un client donné
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
                    p.setMusiques(getMusiquesDansPlaylist(id));
                    liste.add(p);
                }
            }
        } catch (FileNotFoundException e) {
            // fichier pas encore créé : aucune playlist
        } catch (IOException e) {
            e.printStackTrace();
        }

        return liste;
    }

    /**
     * Supprime une playlist (et ses associations de musiques)
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

        // supprimer aussi les musiques associées à cette playlist
        supprimerAssociationsPlaylist(idPlaylist);
        return true;
    }

    // ==================== GESTION DES MUSIQUES DANS PLAYLIST ====================

    /**
     * Ajoute une musique dans une playlist
     * Format dans playlist_musiques.txt : idPlaylist;idMusique
     */
    public static boolean ajouterMusique(int idPlaylist, int idMusique) {
        // vérifier que la musique n'est pas déjà dans la playlist
        List<Musique> existantes = getMusiquesDansPlaylist(idPlaylist);
        for (Musique m : existantes) {
            if (m.getId() == idMusique) {
                return false; // déjà présente
            }
        }

        try (FileWriter fw = new FileWriter(FICHIER_PLAYLIST_MUSIQUES, true)) {
            fw.write(idPlaylist + ";" + idMusique + "\n");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retire une musique d'une playlist
     */
    public static boolean retirerMusique(int idPlaylist, int idMusique) {
        List<String> lignesAGarder = new ArrayList<>();
        boolean trouve = false;

        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_PLAYLIST_MUSIQUES))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                int idP = Integer.parseInt(parts[0]);
                int idM = Integer.parseInt(parts[1]);
                if (idP == idPlaylist && idM == idMusique) {
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

        try (FileWriter fw = new FileWriter(FICHIER_PLAYLIST_MUSIQUES, false)) {
            for (String l : lignesAGarder) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Retourne la liste des musiques dans une playlist donnée
     */
    public static List<Musique> getMusiquesDansPlaylist(int idPlaylist) {
        List<Musique> musiques = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_PLAYLIST_MUSIQUES))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (parts.length < 2) continue;
                int idP = Integer.parseInt(parts[0]);
                int idM = Integer.parseInt(parts[1]);
                if (idP == idPlaylist) {
                    Musique m = Musique.rechercherParId(idM);
                    if (m != null) musiques.add(m);
                }
            }
        } catch (FileNotFoundException e) {
            // fichier pas encore créé
        } catch (IOException e) {
            e.printStackTrace();
        }

        return musiques;
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
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_PLAYLIST_MUSIQUES))) {
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
        try (FileWriter fw = new FileWriter(FICHIER_PLAYLIST_MUSIQUES, false)) {
            for (String l : lignesAGarder) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nom + " (" + musiques.size() + " musique(s))";
    }
}
