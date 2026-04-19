package model;

import java.io.*;
import java.util.*;

/**
 * Classe de base représentant un utilisateur de la plateforme musicale.
 * <p>
 * Un utilisateur possède un identifiant unique, un nom, un prénom, un mot de passe
 * et une adresse e-mail. Cette classe est étendue par {@link Client} et
 * {@link Administrateur} selon le rôle stocké dans le fichier de persistance.
 * </p>
 * <p>
 * <strong>Format de persistance ({@code monfichier.txt}) :</strong><br>
 * {@code id;nom;prenom;mdp;email;role[;SUSPENDU]}
 * <ul>
 *   <li>{@code role=0} → administrateur</li>
 *   <li>{@code role=1} → client</li>
 *   <li>Le suffixe {@code ;SUSPENDU} est optionnel et indique un compte suspendu.</li>
 * </ul>
 * </p>
 *
 * @see Client
 * @see Administrateur
 */
public class Utilisateur {

    /** Identifiant unique de l'utilisateur. */
    private int ID;

    /** Nom de famille de l'utilisateur. */
    private String NOM;

    /** Prénom de l'utilisateur. */
    private String PRENOM;

    /** Mot de passe de l'utilisateur (stocké en clair). */
    private String MDP;

    /** Adresse e-mail de l'utilisateur (sert d'identifiant de connexion). */
    private String EMAIL;

    /** Générateur de nombres aléatoires utilisé pour l'attribution d'un nouvel ID. */
    Random randomID = new Random();

    // ==================== GETTERS / SETTERS ====================

    /**
     * Retourne l'identifiant unique de l'utilisateur.
     *
     * @return l'ID de l'utilisateur
     */
    public int getID() { return ID; }

    /**
     * Définit l'identifiant unique de l'utilisateur.
     *
     * @param ID le nouvel ID
     */
    public void setID(int ID) { this.ID = ID; }

    /**
     * Retourne le nom de famille de l'utilisateur.
     *
     * @return le nom
     */
    public String getNOM() { return NOM; }

    /**
     * Définit le nom de famille de l'utilisateur.
     *
     * @param NOM le nouveau nom
     */
    public void setNOM(String NOM) { this.NOM = NOM; }

    /**
     * Retourne le prénom de l'utilisateur.
     *
     * @return le prénom
     */
    public String getPRENOM() { return PRENOM; }

    /**
     * Définit le prénom de l'utilisateur.
     *
     * @param PRENOM le nouveau prénom
     */
    public void setPRENOM(String PRENOM) { this.PRENOM = PRENOM; }

    /**
     * Retourne le mot de passe de l'utilisateur.
     *
     * @return le mot de passe
     */
    public String getMDP() { return MDP; }

    /**
     * Définit le mot de passe de l'utilisateur.
     *
     * @param MDP le nouveau mot de passe
     */
    public void setMDP(String MDP) { this.MDP = MDP; }

    /**
     * Retourne l'adresse e-mail de l'utilisateur.
     *
     * @return l'e-mail
     */
    public String getEMAIL() { return EMAIL; }

    /**
     * Définit l'adresse e-mail de l'utilisateur.
     *
     * @param EMAIL la nouvelle adresse e-mail
     */
    public void setEMAIL(String EMAIL) { this.EMAIL = EMAIL; }

    // ==================== INSCRIPTION / CONNEXION ====================

    /**
     * Inscrit un nouvel utilisateur avec les informations fournies et persiste
     * l'entrée dans {@code monfichier.txt} avec le rôle client ({@code 1}).
     * <p>
     * Un ID aléatoire compris entre 1 et 1000 est généré automatiquement.
     * Les champs de l'instance sont renseignés avant l'écriture.
     * </p>
     *
     * @param nom    le nom de famille
     * @param prenom le prénom
     * @param email  l'adresse e-mail
     * @param mdp    le mot de passe
     * @return l'ID généré pour le nouvel utilisateur
     */
    public int inscrire(String nom, String prenom, String email, String mdp) {
        this.ID = randomID.nextInt(1000) + 1;
        this.NOM = nom;
        this.PRENOM = prenom;
        this.EMAIL = email;
        this.MDP = mdp;

        try {
            FileWriter fw = new FileWriter("monfichier.txt", true);
            fw.write(ID + ";" + NOM + ";" + PRENOM + ";" + MDP + ";" + EMAIL + ";1\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ID;
    }

    /**
     * Vérifie si l'adresse e-mail saisie correspond à un compte client (rôle {@code 1})
     * dans le fichier de persistance.
     * <p>
     * Si le mail est trouvé, les champs de l'instance sont renseignés avec les données
     * du compte correspondant.
     * </p>
     *
     * @param mailSaisi l'adresse e-mail à rechercher
     * @return {@code "MAIL_FOUND"} si un compte client correspondant est trouvé,
     *         {@code "MAIL_NOT_FOUND"} sinon
     */
    public String verifierMailClient(String mailSaisi) {
        try (BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (parts.length < 6) continue;
                String id = parts[0];
                String nom = parts[1];
                String prenom = parts[2];
                String mdp = parts[3].trim();
                String email = parts[4].trim();
                String role = parts[5].trim();
                if (mailSaisi.equals(email) && role.equals("1")) {
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
     * Vérifie si l'adresse e-mail saisie correspond à un compte administrateur
     * (rôle {@code 0}) dans le fichier de persistance.
     * <p>
     * Si le mail est trouvé, les champs de l'instance sont renseignés avec les données
     * du compte correspondant.
     * </p>
     *
     * @param mailSaisi l'adresse e-mail à rechercher
     * @return {@code "MAIL_FOUND"} si un compte administrateur correspondant est trouvé,
     *         {@code "MAIL_NOT_FOUND"} sinon
     */
    public String verifierMailAdmin(String mailSaisi) {
        try (BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (parts.length < 6) continue;
                String id = parts[0];
                String nom = parts[1];
                String prenom = parts[2];
                String mdp = parts[3].trim();
                String email = parts[4].trim();
                String role = parts[5].trim();
                if (mailSaisi.equals(email) && role.equals("0")) {
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
     * Vérifie si le mot de passe saisi correspond au mot de passe stocké
     * dans l'instance courante.
     *
     * @param mdpSaisi le mot de passe à vérifier
     * @return {@code true} si le mot de passe est correct, {@code false} sinon
     */
    public boolean verifierMdp(String mdpSaisi) {
        return mdpSaisi.equals(this.MDP);
    }

    // ==================== SUSPENSION DE COMPTE ====================

    /**
     * Vérifie si un compte utilisateur est suspendu.
     * <p>
     * Un compte est considéré suspendu si la chaîne {@code "SUSPENDU"} apparaît
     * parmi les champs de sa ligne dans {@code monfichier.txt}.
     * </p>
     *
     * @param idCible l'identifiant du compte à vérifier
     * @return {@code true} si le compte est suspendu, {@code false} sinon
     *         (ou si l'ID est introuvable)
     */
    public static boolean estSuspendu(int idCible) {
        try (BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (Integer.parseInt(parts[0]) == idCible) {
                    for (String part : parts) {
                        if (part.equals("SUSPENDU")) return true;
                    }
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Suspend un compte abonné en ajoutant le flag {@code ;SUSPENDU} à la fin
     * de sa ligne dans {@code monfichier.txt}.
     * <p>
     * Les comptes administrateurs (rôle {@code 0}) ne peuvent pas être suspendus.
     * Un compte déjà suspendu n'est pas modifié.
     * </p>
     *
     * @param idCible l'identifiant du compte à suspendre
     * @return {@code true} si la suspension a été effectuée avec succès,
     *         {@code false} si le compte est introuvable, déjà suspendu,
     *         ou s'il s'agit d'un administrateur
     */
    public static boolean suspendreCompte(int idCible) {
        List<String> lignes = new ArrayList<>();
        boolean trouve = false;

        try (BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                int id = Integer.parseInt(parts[0]);

                if (id == idCible) {
                    if (parts.length >= 6 && parts[5].equals("0")) {
                        lignes.add(ligne);
                        continue;
                    }
                    boolean dejaSuspendu = false;
                    for (String part : parts) {
                        if (part.equals("SUSPENDU")) {
                            dejaSuspendu = true;
                            break;
                        }
                    }
                    if (dejaSuspendu) {
                        lignes.add(ligne);
                    } else {
                        lignes.add(ligne + ";SUSPENDU");
                        trouve = true;
                    }
                } else {
                    lignes.add(ligne);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!trouve) return false;

        try (FileWriter fw = new FileWriter("monfichier.txt", false)) {
            for (String l : lignes) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Réactive un compte suspendu en retirant le flag {@code SUSPENDU} de sa ligne
     * dans {@code monfichier.txt}.
     *
     * @param idCible l'identifiant du compte à réactiver
     * @return {@code true} si la réactivation a été effectuée avec succès,
     *         {@code false} si le compte est introuvable ou n'était pas suspendu
     */
    public static boolean reactiverCompte(int idCible) {
        List<String> lignes = new ArrayList<>();
        boolean trouve = false;

        try (BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                int id = Integer.parseInt(parts[0]);

                if (id == idCible) {
                    StringBuilder sb = new StringBuilder();
                    boolean avaitSuspendu = false;
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].equals("SUSPENDU")) {
                            avaitSuspendu = true;
                            continue;
                        }
                        if (sb.length() > 0) sb.append(";");
                        sb.append(parts[i]);
                    }
                    lignes.add(sb.toString());
                    if (avaitSuspendu) trouve = true;
                } else {
                    lignes.add(ligne);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!trouve) return false;

        try (FileWriter fw = new FileWriter("monfichier.txt", false)) {
            for (String l : lignes) fw.write(l + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}