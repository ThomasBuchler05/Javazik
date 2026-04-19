package model;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Représente une entrée dans l'historique d'écoute d'un client.
 * <p>
 * Chaque écoute enregistrée contient les informations essentielles du morceau
 * ainsi que la date et l'heure d'écoute. Les entrées sont persistées dans
 * {@code historique.txt}.
 * </p>
 * <p>
 * <strong>Format de persistance ({@code historique.txt}) :</strong><br>
 * {@code idClient;idMorceau;titre;interprete;annee;dateHeure}
 * </p>
 *
 * @see Morceau
 * @see Client
 */
public class Historique {

    /** Chemin du fichier de persistance de l'historique d'écoute. */
    private static final String FICHIER_HISTORIQUE = "historique.txt";

    /** Formateur de date/heure utilisé pour l'affichage et la persistance. */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Identifiant du morceau écouté. */
    private int idMorceau;

    /** Titre du morceau écouté. */
    private String titre;

    /** Nom de l'interprète du morceau. */
    private String interprete;

    /** Année de sortie du morceau. */
    private int annee;

    /** Date et heure de l'écoute au format {@code dd/MM/yyyy HH:mm}. */
    private String dateHeure;

    /**
     * Construit une entrée d'historique avec toutes ses informations.
     *
     * @param idMorceau  identifiant du morceau
     * @param titre      titre du morceau
     * @param interprete nom de l'interprète
     * @param annee      année de sortie du morceau
     * @param dateHeure  date et heure de l'écoute (format {@code dd/MM/yyyy HH:mm})
     */
    public Historique(int idMorceau, String titre, String interprete, int annee, String dateHeure) {
        this.idMorceau = idMorceau;
        this.titre     = titre;
        this.interprete = interprete;
        this.annee     = annee;
        this.dateHeure = dateHeure;
    }

    // ==================== GETTERS ====================

    /**
     * Retourne l'identifiant du morceau écouté.
     * @return l'ID du morceau
     */
    public int    getIdMorceau()  { return idMorceau; }

    /**
     * Retourne le titre du morceau écouté.
     * @return le titre
     */
    public String getTitre()      { return titre; }

    /**
     * Retourne le nom de l'interprète du morceau.
     * @return le nom de l'interprète
     */
    public String getInterprete() { return interprete; }

    /**
     * Retourne l'année de sortie du morceau.
     * @return l'année
     */
    public int    getAnnee()      { return annee; }

    /**
     * Retourne la date et l'heure de l'écoute.
     * @return la date/heure au format {@code dd/MM/yyyy HH:mm}
     */
    public String getDateHeure()  { return dateHeure; }

    // ==================== PERSISTENCE ====================

    /**
     * Enregistre une écoute dans l'historique persistant ({@code historique.txt}).
     * <p>
     * La date et l'heure d'écoute sont automatiquement définies à l'instant présent.
     * </p>
     *
     * @param idClient l'ID du client qui écoute
     * @param morceau  le morceau écouté
     */
    public static void enregistrerEcoute(int idClient, Morceau morceau) {
        String dateHeure = LocalDateTime.now().format(FORMATTER);
        String ligne = idClient + ";"
                + morceau.getId()    + ";"
                + morceau.getTitre() + ";"
                + morceau.getNomInterprete() + ";"
                + morceau.getAnnee()   + ";"
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
     * @return liste des entrées d'historique (peut être vide si aucune écoute)
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

                int    idMorceau  = Integer.parseInt(parts[1]);
                String titre      = parts[2];
                String interprete = parts[3];
                int    annee      = Integer.parseInt(parts[4]);
                String dateHeure  = parts[5];

                liste.add(new Historique(idMorceau, titre, interprete, annee, dateHeure));
            }
        } catch (FileNotFoundException e) {
            // aucun historique encore
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.reverse(liste);
        return liste;
    }

    /**
     * Retourne le nombre total d'écoutes enregistrées, tous clients confondus.
     *
     * @return le nombre total d'écoutes
     */
    public static int getNombreTotalEcoutes() {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_HISTORIQUE))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (!ligne.trim().isEmpty()) count++;
            }
        } catch (FileNotFoundException e) {
            // pas d'historique
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Supprime toutes les entrées d'historique d'un client donné.
     * <p>
     * Réécrit {@code historique.txt} en conservant uniquement les lignes
     * appartenant aux autres clients.
     * </p>
     *
     * @param idClient l'ID du client dont l'historique doit être supprimé
     */
    public static void supprimerHistoriqueClient(int idClient) {
        File fichier = new File(FICHIER_HISTORIQUE);
        if (!fichier.exists()) return;

        List<String> lignesAutres = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (parts.length < 1) continue;
                try {
                    if (Integer.parseInt(parts[0]) != idClient)
                        lignesAutres.add(ligne);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) { e.printStackTrace(); return; }

        try (FileWriter fw = new FileWriter(fichier, false)) {
            for (String l : lignesAutres) fw.write(l + "\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public String toString() {
        return "[" + dateHeure + "] " + titre + " - " + interprete + " (" + annee + ")";
    }
}