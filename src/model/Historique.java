package model;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Gère l'historique d'écoute d'un abonné.
 * Chaque entrée est sauvegardée dans "historique.txt" au format :
 *   idClient;idMusique;titre;artiste;annee;dateHeure
 */
public class Historique {

    private static final String FICHIER_HISTORIQUE = "historique.txt";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int idMusique;
    private String titre;
    private String artiste;
    private int annee;
    private String dateHeure;

    public Historique(int idMusique, String titre, String artiste, int annee, String dateHeure) {
        this.idMusique = idMusique;
        this.titre     = titre;
        this.artiste   = artiste;
        this.annee     = annee;
        this.dateHeure = dateHeure;
    }

    // ==================== GETTERS ====================

    public int    getIdMusique() { return idMusique; }
    public String getTitre()     { return titre; }
    public String getArtiste()   { return artiste; }
    public int    getAnnee()     { return annee; }
    public String getDateHeure() { return dateHeure; }

    // ==================== PERSISTENCE ====================

    /**
     * Enregistre une écoute dans l'historique persistant.
     *
     * @param idClient l'ID du client connecté
     * @param musique  la musique qui vient d'être écoutée
     */
    public static void enregistrerEcoute(int idClient, Musique musique) {
        String dateHeure = LocalDateTime.now().format(FORMATTER);
        String ligne = idClient + ";"
                + musique.getId()    + ";"
                + musique.getTitre() + ";"
                + musique.getArtiste() + ";"
                + musique.getAnnee()   + ";"
                + dateHeure;

        try (FileWriter fw = new FileWriter(FICHIER_HISTORIQUE, true)) {
            fw.write(ligne + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Charge et retourne l'historique d'écoute d'un client donné,
     * du plus récent au plus ancien.
     *
     * @param idClient l'ID du client
     * @return liste des entrées d'historique
     */
    public static List<Historique> getHistoriqueClient(int idClient) {
        List<Historique> liste = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_HISTORIQUE))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (parts.length < 6) continue;
                int idCli = Integer.parseInt(parts[0]);
                if (idCli != idClient) continue;

                int    idMusique  = Integer.parseInt(parts[1]);
                String titre      = parts[2];
                String artiste    = parts[3];
                int    annee      = Integer.parseInt(parts[4]);
                String dateHeure  = parts[5];

                liste.add(new Historique(idMusique, titre, artiste, annee, dateHeure));
            }
        } catch (FileNotFoundException e) {
            // aucun historique encore : liste vide
        } catch (IOException e) {
            e.printStackTrace();
        }

        // plus récent en premier
        Collections.reverse(liste);
        return liste;
    }

    @Override
    public String toString() {
        return "[" + dateHeure + "] " + titre + " - " + artiste + " (" + annee + ")";
    }
}