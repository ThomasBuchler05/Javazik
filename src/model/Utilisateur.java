package model;

import java.io.*;
import java.util.*;

public class Utilisateur {
    private int ID;
    private String NOM, PRENOM, MDP, EMAIL;
    Random randomID = new Random();

    public int getID() { return ID; }
    public void setID(int ID) { this.ID = ID; }
    public String getNOM() { return NOM; }
    public void setNOM(String NOM) { this.NOM = NOM; }
    public String getPRENOM() { return PRENOM; }
    public void setPRENOM(String PRENOM) { this.PRENOM = PRENOM; }
    public String getMDP() { return MDP; }
    public void setMDP(String MDP) { this.MDP = MDP; }
    public String getEMAIL() { return EMAIL; }
    public void setEMAIL(String EMAIL) { this.EMAIL = EMAIL; }

    /**
     * Inscrit un utilisateur avec les infos fournies et l'écrit dans le fichier
     * Retourne l'ID généré
     */
    public int inscrire(String nom, String prenom, String email, String mdp) {
        this.ID = randomID.nextInt(1000) + 1;
        this.NOM = nom;
        this.PRENOM = prenom;
        this.EMAIL = email;
        this.MDP = mdp;

        try {
            FileWriter fw = new FileWriter("monfichier.txt", true);
            fw.write(ID + ";" + NOM + ";" + PRENOM + ";" + MDP + ";" + EMAIL + "\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ID;
    }

    /**
     * Vérifie la connexion client (choix==2) : cherche le mail dans le fichier
     * Retourne :
     *  - "MAIL_NOT_FOUND" si mail introuvable
     *  - "MAIL_FOUND" si mail trouvé (le mdp attendu est stocké dans cet objet via setMDP)
     */
    public String verifierMailClient(String mailSaisi) {
        try (BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                String[] parts = ligne.split(";");
                String id = parts[0];
                String nom = parts[1];
                String prenom = parts[2];
                String mdp = parts[3];
                String email = parts[4];
                if (mailSaisi.equals(email)) {
                    this.ID = Integer.parseInt(id);
                    this.NOM = nom;
                    this.PRENOM = prenom;
                    this.MDP = mdp;
                    this.EMAIL = email;
                    return "MAIL_FOUND";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "MAIL_NOT_FOUND";
    }

    /**
     * Vérifie la connexion admin (choix==1) : cherche le mail + vérifie le flag admin (parts[5])
     * Retourne :
     *  - "MAIL_NOT_FOUND" si mail introuvable
     *  - "NOT_ADMIN" si mail trouvé mais pas admin
     *  - "MAIL_FOUND" si mail trouvé et admin (flag == "0")
     */
    public String verifierMailAdmin(String mailSaisi) {
        try (BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                String[] parts = ligne.split(";");
                String id = parts[0];
                String nom = parts[1];
                String prenom = parts[2];
                String mdp = parts[3];
                String email = parts[4];
                String admin = parts[5];
                if (mailSaisi.equals(email) && admin.equals("0")) {
                    this.ID = Integer.parseInt(id);
                    this.NOM = nom;
                    this.PRENOM = prenom;
                    this.MDP = mdp;
                    this.EMAIL = email;
                    return "MAIL_FOUND";
                } else if (mailSaisi.equals(email) && !admin.equals("0")) {
                    return "NOT_ADMIN";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "MAIL_NOT_FOUND";
    }

    /**
     * Vérifie si le mot de passe saisi correspond au MDP stocké
     */
    public boolean verifierMdp(String mdpSaisi) {
        return mdpSaisi.equals(this.MDP);
    }
}
