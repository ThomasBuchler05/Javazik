import java.util.*;
import java.io.*;
import java.io.IOException;
import java.util.Scanner;

public class utilisateur {
    private int ID;
    private String NOM, PRENOM, MDP, EMAIL;
    Random randomID = new Random();
    Scanner sc = new Scanner(System.in);
    public int getID() {return ID;}
    public void setID(int ID) {this.ID = ID;}
    public String getNOM() {return NOM;}
    public void setNOM(String NOM) {
        this.NOM = NOM;
    }

    public String getPRENOM() {
        return PRENOM;
    }

    public void setPRENOM(String PRENOM) {
        this.PRENOM = PRENOM;
    }

    public String getMDP() {
        return MDP;
    }

    public void setMDP(String MDP) {
        this.MDP = MDP;
    }

    public String getEMAIL() {
        return EMAIL;
    }

    public void setEMAIL(String EMAIL) {
        this.EMAIL = EMAIL;
    }

    void inscription() {

        ID = randomID.nextInt(1000)+1;
        System.out.println("Entrez votre nom : ");
        NOM = sc.nextLine();

        System.out.print("Entrez votre prénom : ");
        PRENOM = sc.nextLine();

        System.out.print("Entrez votre email : ");
        EMAIL = sc.nextLine();

        System.out.print("Entrez votre mot de passe : ");
        MDP = sc.nextLine();

        System.out.println("Inscription réussie !");
        try {
            FileWriter fw = new FileWriter("monfichier.txt", true);
            fw.write(ID + ";" + NOM + ";" + PRENOM + ";"+ MDP + ";"+ EMAIL + "\n");
            fw.close();
            System.out.println("Écriture réussie !");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void connexion(int choix){
        boolean connexion = false;
        if (choix==2){
            while(connexion == false){
                System.out.print("Veuillez entrer votre mail : ");
                String mailsaisi = sc.nextLine();
                String mdp = null;
                String id = null;
                String nom = null;
                String prenom = null;
                String email = null;
                String admin = null;
                try(BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))){
                    String ligne;
                    boolean trouver = false;
                    while((ligne = br.readLine())!= null) {
                        String lignetrouvee = null;
                        String[] parts = ligne.split(";");
                        id = parts[0];
                        nom = parts[1];
                        prenom = parts[2];
                        mdp = parts[3];
                        email = parts[4];
                        if (mailsaisi.equals(email)) {
                            trouver = true;
                            break;
                        }

                    }
                    if (trouver){
                        System.out.println("Veuillez entrer votre mot de passe :");
                        String mdpsaisi =  sc.nextLine();

                        while(!mdpsaisi.equals(mdp)){
                            System.out.println("Mot de passe incorrecte!");
                            System.out.println("Veuillez entrer votre mot de passe :");
                            mdpsaisi =  sc.nextLine();
                        }
                        System.out.println("Connexion réussie !");
                        connexion = true;
                    }
                    else
                    {
                        System.out.println("Mail incorecte !");
                    }


                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        if(choix==1){
            while(connexion == false){
                System.out.println("Veuillez êtes en connexion admin");
                System.out.print("Veuillez entrer votre mail : ");
                String mailsaisi = sc.nextLine();
                String mdp = null;
                String id = null;
                String nom = null;
                String prenom = null;
                String email = null;
                String admin = null;
                try(BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))){
                    String ligne;
                    boolean trouver = false;
                    boolean pasadmin = false;
                    while((ligne = br.readLine())!= null) {
                        String lignetrouvee = null;
                        String[] parts = ligne.split(";");
                        id = parts[0];
                        nom = parts[1];
                        prenom = parts[2];
                        mdp = parts[3];
                        email = parts[4];
                        admin = parts[5];
                        if (mailsaisi.equals(email) && admin.equals("0")) {
                            trouver = true;
                            break;
                        }else if(mailsaisi.equals(email) && !admin.equals("0")){
                            pasadmin = true;
                            break;
                        }

                    }

                    if (trouver) {
                        System.out.println("Veuillez entrer votre mot de passe :");
                        String mdpsaisi = sc.nextLine();

                        while (!mdpsaisi.equals(mdp)) {
                            System.out.println("Mot de passe incorrecte!");
                            System.out.println("Veuillez entrer votre mot de passe :");
                            mdpsaisi = sc.nextLine();
                        }
                        System.out.println("Connexion réussie !");
                        connexion = true;
                    }else if (pasadmin) {
                        System.out.println("Vous n'êtes pas admin");
                    }else{
                        System.out.println("Mail incorecte !");
                    }


                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
