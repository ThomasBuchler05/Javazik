package model;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Gère l'historique d'écoute d'un abonné.
 * Chaque entrée est sauvegardée dans "historique.txt" au format :
 *   idClient;idMorceau;titre;interprete;annee;dateHeure
 */
public class Historique {

    private static final String FICHIER_HISTORIQUE = "historique.txt";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int idMorceau;
    private String titre;
    private String interprete;
    private int annee;
    private String dateHeure;

    public Historique(int idMorceau, String titre, String interprete, int annee, String dateHeure) {
        this.idMorceau = idMorceau;
        this.titre     = titre;
        this.interprete = interprete;
        this.annee     = annee;
        this.dateHeure = dateHeure;
    }

    // ==================== GETTERS ====================

    public int    getIdMorceau()  { return idMorceau; }
    public String getTitre()      { return titre; }
    public String getInterprete() { return interprete; }
    public int    getAnnee()      { return annee; }
    public String getDateHeure()  { return dateHeure; }

    // ==================== PERSISTENCE ====================

    /**
     * Enregistre une écoute dans l'historique persistant.
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
     * Retourne le nombre total d'écoutes tous clients confondus.
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

    @Override
    public String toString() {
        return "[" + dateHeure + "] " + titre + " - " + interprete + " (" + annee + ")";
    }
}
