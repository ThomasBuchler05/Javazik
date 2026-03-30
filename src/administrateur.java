import java.util.*;
import java.io.*;

public class administrateur extends utilisateur{
    private int ID;
    private String NOM, PRENOM, MDP, EMAIL;
    static String titre;
    static public int annee;
    static String artiste;
    static public int idCible;

    public administrateur(int ID, String NOM, String PRENOM, String MDP, String EMAIL, String prenom, String titre, int annee,  String artiste,  int idCible) {
        this.ID = ID;
        this.NOM = NOM;
        this.PRENOM = PRENOM;
        this.MDP = MDP;
        this.EMAIL = EMAIL;
        this.titre = titre;
        this.annee = annee;
        this.artiste = artiste;
        this.idCible = idCible;
    }

    static void ajouter_musique(){
        String fichier = "musiques.txt";
        Scanner sc = new Scanner(System.in);
        System.out.println("Veuillez saisir le Titre de la musique");
        titre = sc.nextLine();
        System.out.println("Veuillez saisir l'artiste de la musique");
        artiste = sc.nextLine();

        System.out.println("Veuillez saisir l'année de la musique");
        annee = sc.nextInt();

        //générer un ID; max existant + 1
        int nouvelID = 1;
        try (BufferedReader reader = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parties = ligne.split(";");
                int idExistant = Integer.parseInt(parties[0]);
                if (idExistant >= nouvelID) {
                    nouvelID = idExistant + 1;
                }
            }
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }

        //écrire dans le fichier
        try (FileWriter fw = new FileWriter(fichier, true)) {
            fw.write(nouvelID + ";" + titre + ";" + artiste + ";" + annee + "\n");
            System.out.println("Musique ajoutée ! (ID : " + nouvelID + ")");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static void supprimer_musique(){
        String fichier = "musiques.txt";
        Scanner sc = new Scanner(System.in);
        System.out.println("Veuillez saisir l'ID de la musique à supprimer");
        idCible = sc.nextInt();

        //lire et filtrer
        List<String> lignesAGarder = new ArrayList<>();
        boolean trouve = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parties = ligne.split(";");
                int idLigne = Integer.parseInt(parties[0]);
                if (idLigne == idCible) {
                    trouve = true;
                } else {
                    lignesAGarder.add(ligne);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        //réécrire le fichier
        if (!trouve) {
            System.out.println("Aucune musique trouvée avec l'ID " + idCible);
            return;
        }
        try (FileWriter fw = new FileWriter(fichier, false)) { // false = écrasement
            for (String ligne : lignesAGarder) {
                fw.write( "\n" + ligne );
            }
            System.out.println("Musique supprimée !");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    }

    





