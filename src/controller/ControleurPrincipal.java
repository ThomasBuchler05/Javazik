package controller;

import model.*;
import view.VueGraphique;
import view.VueConsole;

import java.io.*;
import java.util.*;

/**
 * Contrôleur principal de l'application Javazik.
 * Fait le lien entre le modèle et la vue.
 */
public class ControleurPrincipal {

    private VueConsole vue;
    private Utilisateur utilisateur;

    public ControleurPrincipal(boolean graphique) {
        if (graphique) {
            this.vue = new VueGraphique();
        } else {
            this.vue = new VueConsole();
        }
        this.utilisateur = new Client();
    }

    public void lancer() {
        vue.afficherBienvenue();
        boolean quitter = false;
        while (!quitter) {
            int choix = vue.afficherMenuPrincipal();
            switch (choix) {
                case 1: connexionAdmin(); break;
                case 2: connexionClient(); break;
                case 3: inscription(); break;
                case 4: menuVisiteur(); break;
                case 5: quitter = true; break;
                default: vue.afficherChoixInvalide();
            }
        }
        vue.afficherMessage("Merci d'avoir utilise Javazik. A bientot !");
    }

    // ==================== CONNEXION ADMIN ====================

    private void connexionAdmin() {
        vue.afficherConnexionAdmin();
        String mail = vue.demanderMail();
        String resultat = utilisateur.verifierMailAdmin(mail);

        if (resultat.equals("MAIL_FOUND")) {
            String mdpSaisi = vue.demanderMdp();
            int tentatives = 0;
            while (!utilisateur.verifierMdp(mdpSaisi) && tentatives < 3) {
                vue.afficherMdpIncorrect();
                mdpSaisi = vue.demanderMdp();
                tentatives++;
            }
            if (utilisateur.verifierMdp(mdpSaisi)) {
                vue.afficherConnexionReussie();
                menuAdmin();
            } else {
                vue.afficherMessage("Trop de tentatives, retour au menu principal.");
            }
        } else if (resultat.equals("NOT_ADMIN")) {
            vue.afficherPasAdmin();
        } else {
            vue.afficherMailIncorrect();
        }
    }

    // ==================== CONNEXION CLIENT ====================

    private void connexionClient() {
        String mail = vue.demanderMail();
        String resultat = utilisateur.verifierMailClient(mail);

        if (resultat.equals("MAIL_FOUND")) {
            String mdpSaisi = vue.demanderMdp();
            int tentatives = 0;
            while (!utilisateur.verifierMdp(mdpSaisi) && tentatives < 3) {
                vue.afficherMdpIncorrect();
                mdpSaisi = vue.demanderMdp();
                tentatives++;
            }
            if (utilisateur.verifierMdp(mdpSaisi)) {
                vue.afficherConnexionReussie();
                menuClient();
            } else {
                vue.afficherMessage("Trop de tentatives, retour au menu principal.");
            }
        } else {
            vue.afficherMailIncorrect();
        }
    }

    // ==================== INSCRIPTION ====================

    private void inscription() {
        String nom = vue.demanderNom();
        String prenom = vue.demanderPrenom();
        String email = vue.demanderEmail();
        String mdp = vue.demanderMotDePasse();
        utilisateur.inscrire(nom, prenom, email, mdp);
        vue.afficherInscriptionReussie();
    }

    // ==================== MENU ADMIN ====================

    private void menuAdmin() {
        int choix;
        do {
            choix = vue.afficherMenuAdmin();
            switch (choix) {
                case 1: adminAjouterMorceau(); break;
                case 2: adminSupprimerMorceau(); break;
                case 3: adminAjouterAlbum(); break;
                case 4: adminSupprimerAlbum(); break;
                case 5: adminAjouterMorceauDansAlbum(); break;
                case 6: adminAjouterArtiste(); break;
                case 7: adminSupprimerArtiste(); break;
                case 8: adminAjouterGroupe(); break;
                case 9: adminSupprimerGroupe(); break;
                case 10: adminAjouterMembreGroupe(); break;
                case 11: adminGererComptes(); break;
                case 12: adminStatistiques(); break;
                case 13: vue.afficherRetourMenuPrincipal(); break;
                default: vue.afficherChoixInvalide();
            }
        } while (choix != 13);
    }

    private void adminAjouterMorceau() {
        vue.afficherListeArtistes(Catalogue.getTousLesArtistes());
        vue.afficherListeGroupes(Catalogue.getTousLesGroupes());

        String titre = vue.demanderTitreMorceau();
        int duree = vue.demanderDureeMorceau();
        String genre = vue.demanderGenreMorceau();
        int annee = vue.demanderAnneeMorceau();
        int idArtiste = vue.demanderIdArtisteMorceau();
        int idGroupe = vue.demanderIdGroupeMorceau();
        Morceau m = Morceau.ajouter(titre, duree, genre, annee, idArtiste, idGroupe);
        vue.afficherMorceauAjoute(m.getId());
    }

    private void adminSupprimerMorceau() {
        vue.afficherListeMorceaux(Catalogue.getTousLesMorceaux());
        int id = vue.demanderIdSuppression();
        if (Morceau.supprimer(id)) {
            vue.afficherElementSupprime("Morceau");
        } else {
            vue.afficherElementNonTrouve("morceau", id);
        }
    }

    private void adminAjouterAlbum() {
        vue.afficherListeArtistes(Catalogue.getTousLesArtistes());
        vue.afficherListeGroupes(Catalogue.getTousLesGroupes());

        String titre = vue.demanderTitreAlbum();
        int annee = vue.demanderAnneeAlbum();
        int idArtiste = vue.demanderIdArtisteAlbum();
        int idGroupe = vue.demanderIdGroupeAlbum();
        Album a = Album.ajouter(titre, annee, idArtiste, idGroupe);
        vue.afficherAlbumAjoute(a.getId());
    }

    private void adminSupprimerAlbum() {
        vue.afficherListeAlbums(Catalogue.getTousLesAlbums());
        int id = vue.demanderIdSuppression();
        if (Album.supprimer(id)) {
            vue.afficherElementSupprime("Album");
        } else {
            vue.afficherElementNonTrouve("album", id);
        }
    }

    private void adminAjouterArtiste() {
        String nom = vue.demanderNomArtiste();
        String prenom = vue.demanderPrenomArtiste();
        String nationalite = vue.demanderNationaliteArtiste();
        Artiste a = Artiste.ajouter(nom, prenom, nationalite);
        vue.afficherArtisteAjoute(a.getId());
    }

    private void adminSupprimerArtiste() {
        vue.afficherListeArtistes(Catalogue.getTousLesArtistes());
        int id = vue.demanderIdSuppression();
        if (Artiste.supprimer(id)) {
            vue.afficherElementSupprime("Artiste");
        } else {
            vue.afficherElementNonTrouve("artiste", id);
        }
    }

    private void adminAjouterGroupe() {
        String nom = vue.demanderNomGroupe();
        int date = vue.demanderDateCreationGroupe();
        String nationalite = vue.demanderNationaliteGroupe();
        Groupe g = Groupe.ajouter(nom, date, nationalite);
        vue.afficherGroupeAjoute(g.getId());
    }

    private void adminSupprimerGroupe() {
        vue.afficherListeGroupes(Catalogue.getTousLesGroupes());
        int id = vue.demanderIdSuppression();
        if (Groupe.supprimer(id)) {
            vue.afficherElementSupprime("Groupe");
        } else {
            vue.afficherElementNonTrouve("groupe", id);
        }
    }

    // ==================== ADMIN : ASSOCIATION MORCEAU <-> ALBUM ====================

    private void adminAjouterMorceauDansAlbum() {
        List<Album> albums = Catalogue.getTousLesAlbums();
        vue.afficherListeAlbums(albums);
        if (albums.isEmpty()) {
            vue.afficherMessage("Aucun album dans le catalogue. Creez d'abord un album.");
            return;
        }

        int idAlbum = vue.demanderIdAlbumAssociation();
        Album album = Album.rechercherParId(idAlbum);
        if (album == null) {
            vue.afficherElementNonTrouve("album", idAlbum);
            return;
        }

        vue.afficherDetailsAlbum(album);
        vue.afficherListeMorceaux(Catalogue.getTousLesMorceaux());

        int idMorceau = vue.demanderIdMorceauAssociation();
        Morceau morceau = Morceau.rechercherParId(idMorceau);
        if (morceau == null) {
            vue.afficherElementNonTrouve("morceau", idMorceau);
            return;
        }

        for (Morceau m : album.getMorceaux()) {
            if (m.getId() == idMorceau) {
                vue.afficherMessage("Ce morceau est deja dans cet album.");
                return;
            }
        }

        int numPiste = vue.demanderNumeroPiste();
        if (Album.ajouterMorceau(idAlbum, idMorceau, numPiste)) {
            vue.afficherMorceauAjouteDansAlbum(morceau.getTitre(), album.getTitre());
        } else {
            vue.afficherMessage("Erreur lors de l'ajout du morceau dans l'album.");
        }
    }

    // ==================== ADMIN : ASSOCIATION ARTISTE <-> GROUPE ====================

    private void adminAjouterMembreGroupe() {
        List<Groupe> groupes = Catalogue.getTousLesGroupes();
        vue.afficherListeGroupes(groupes);
        if (groupes.isEmpty()) {
            vue.afficherMessage("Aucun groupe dans le catalogue. Creez d'abord un groupe.");
            return;
        }

        int idGroupe = vue.demanderIdGroupeAssociation();
        Groupe groupe = Groupe.rechercherParId(idGroupe);
        if (groupe == null) {
            vue.afficherElementNonTrouve("groupe", idGroupe);
            return;
        }

        vue.afficherDetailsGroupe(groupe);
        vue.afficherListeArtistes(Catalogue.getTousLesArtistes());

        int idArtiste = vue.demanderIdArtisteAssociation();
        Artiste artiste = Artiste.rechercherParId(idArtiste);
        if (artiste == null) {
            vue.afficherElementNonTrouve("artiste", idArtiste);
            return;
        }

        if (Groupe.ajouterMembre(idGroupe, idArtiste)) {
            vue.afficherMembreAjouteDansGroupe(artiste.getNomComplet(), groupe.getNom());
        } else {
            vue.afficherMessage("Cet artiste est deja membre de ce groupe.");
        }
    }

    private void adminGererComptes() {
        List<String[]> abonnes = chargerAbonnes();
        vue.afficherListeAbonnes(abonnes);
        int choix = vue.afficherMenuGestionComptes();
        if (choix == 1) {
            int idAbonne = vue.demanderIdAbonne();
            if (supprimerAbonne(idAbonne)) {
                vue.afficherAbonneSupprime();
            } else {
                vue.afficherAbonneNonTrouve();
            }
        }
    }

    private List<String[]> chargerAbonnes() {
        List<String[]> abonnes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (parts.length >= 5) {
                    if (parts.length >= 6 && parts[5].equals("0")) continue;
                    abonnes.add(parts);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return abonnes;
    }

    private boolean supprimerAbonne(int idAbonne) {
        List<String> lignes = new ArrayList<>();
        boolean trouve = false;
        try (BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.trim().isEmpty()) continue;
                String[] parts = ligne.split(";");
                if (Integer.parseInt(parts[0]) == idAbonne) {
                    if (parts.length >= 6 && parts[5].equals("0")) {
                        lignes.add(ligne);
                    } else {
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

    private int compterUtilisateurs() {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader("monfichier.txt"))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (!ligne.trim().isEmpty()) count++;
            }
        } catch (IOException e) {
            // pas de fichier
        }
        return count;
    }

    private void adminStatistiques() {
        vue.afficherStatistiques(
                Catalogue.getNombreMorceaux(),
                Catalogue.getNombreAlbums(),
                Catalogue.getNombreArtistes(),
                Catalogue.getNombreGroupes(),
                compterUtilisateurs(),
                Historique.getNombreTotalEcoutes()
        );
    }

    // ==================== MENU CLIENT ====================

    private void menuClient() {
        int choix;
        do {
            choix = vue.afficherMenuClient();
            switch (choix) {
                case 1: menuCatalogue(); break;
                case 2: menuPlaylist(); break;
                case 3: ecouter(Integer.MAX_VALUE); break;
                case 4: consulterHistorique(); break;
                case 5: vue.afficherRetourMenuPrincipal(); break;
                default: vue.afficherChoixInvalide();
            }
        } while (choix != 5);
    }

    // ==================== MENU VISITEUR ====================

    private void menuVisiteur() {
        int choix;
        do {
            choix = vue.afficherMenuVisiteur();
            switch (choix) {
                case 1: menuCatalogue(); break;
                case 2: ecouter(5); break;
                case 3: break;
                default: vue.afficherChoixInvalide();
            }
        } while (choix != 3);
    }

    // ==================== CATALOGUE : NAVIGATION ====================

    private void menuCatalogue() {
        int choix;
        do {
            choix = vue.afficherMenuCatalogue();
            switch (choix) {
                case 1:
                    String recherche = vue.demanderRecherche();
                    Catalogue.ResultatRecherche r = Catalogue.rechercherGlobal(recherche);
                    vue.afficherResultatsRecherche(r);
                    if (!r.estVide()) { naviguer(); }
                    break;
                case 2:
                    vue.afficherListeMorceaux(Catalogue.getTousLesMorceaux());
                    naviguer();
                    break;
                case 3:
                    vue.afficherListeAlbums(Catalogue.getTousLesAlbums());
                    naviguer();
                    break;
                case 4:
                    vue.afficherListeArtistes(Catalogue.getTousLesArtistes());
                    naviguer();
                    break;
                case 5:
                    vue.afficherListeGroupes(Catalogue.getTousLesGroupes());
                    naviguer();
                    break;
                case 6:
                    List<String> genres = Catalogue.getGenresDisponibles();
                    vue.afficherGenresDisponibles(genres);
                    int numGenre = vue.demanderIdElement();
                    if (numGenre >= 1 && numGenre <= genres.size()) {
                        vue.afficherListeMorceaux(Catalogue.getMorceauxParGenre(genres.get(numGenre - 1)));
                        naviguer();
                    } else {
                        vue.afficherChoixInvalide();
                    }
                    break;
                case 7: break;
                default: vue.afficherChoixInvalide();
            }
        } while (choix != 7);
    }

    private void naviguer() {
        boolean continuer = true;
        while (continuer) {
            int choixNav = vue.afficherMenuNavigation();
            switch (choixNav) {
                case 1:
                    int idM = vue.demanderIdElement();
                    Morceau morceau = Morceau.rechercherParId(idM);
                    if (morceau != null) {
                        vue.afficherDetailsMorceau(morceau);
                        vue.afficherAlbumsDuMorceau(Catalogue.getAlbumsDepuisMorceau(idM));
                        vue.afficherAutresMorceauxInterprete(Catalogue.getAutresMorceauxMemeInterprete(morceau));
                    } else { vue.afficherErreurId(); }
                    break;
                case 2:
                    int idA = vue.demanderIdElement();
                    Album album = Album.rechercherParId(idA);
                    if (album != null) {
                        vue.afficherDetailsAlbum(album);
                        vue.afficherAutresAlbumsInterprete(Catalogue.getAutresAlbumsMemeInterprete(album));
                    } else { vue.afficherErreurId(); }
                    break;
                case 3:
                    int idAr = vue.demanderIdElement();
                    Artiste artiste = Artiste.rechercherParId(idAr);
                    if (artiste != null) {
                        vue.afficherDetailsArtiste(artiste);
                        vue.afficherGroupesDeLArtiste(Catalogue.getGroupesDepuisArtiste(idAr));
                        vue.afficherMorceauxArtiste(Catalogue.getMorceauxDepuisArtiste(idAr));
                        vue.afficherAlbumsArtiste(Catalogue.getAlbumsDepuisArtiste(idAr));
                    } else { vue.afficherErreurId(); }
                    break;
                case 4:
                    int idG = vue.demanderIdElement();
                    Groupe groupe = Groupe.rechercherParId(idG);
                    if (groupe != null) {
                        vue.afficherDetailsGroupe(groupe);
                        vue.afficherMorceauxGroupe(Catalogue.getMorceauxDepuisGroupe(idG));
                        vue.afficherAlbumsGroupe(Catalogue.getAlbumsDepuisGroupe(idG));
                    } else { vue.afficherErreurId(); }
                    break;
                case 5:
                    continuer = false;
                    break;
                default:
                    vue.afficherChoixInvalide();
            }
        }
    }

    // ==================== MENU PLAYLIST ====================

    private void menuPlaylist() {
        int choix;
        do {
            choix = vue.afficherMenuPlaylist();
            switch (choix) {
                case 1: creerPlaylist(); break;
                case 2: voirPlaylists(); break;
                case 3: ajouterMorceauPlaylist(); break;
                case 4: retirerMorceauPlaylist(); break;
                case 5: ecouterPlaylist(); break;
                case 6: renommerPlaylist(); break;
                case 7: supprimerPlaylist(); break;
                case 8: break;
                default: vue.afficherChoixInvalide();
            }
        } while (choix != 8);
    }

    private void creerPlaylist() {
        String nom = vue.demanderNomPlaylist();
        Playlist p = Playlist.creer(nom, utilisateur.getID());
        vue.afficherPlaylistCreee(p.getId(), p.getNom());
    }

    private void voirPlaylists() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        for (Playlist p : playlists) {
            vue.afficherContenuPlaylist(p);
        }
    }

    private void ajouterMorceauPlaylist() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        if (playlists.isEmpty()) return;

        int idPlaylist = vue.demanderIdPlaylist();
        Playlist cible = trouverPlaylist(playlists, idPlaylist);
        if (cible == null) { vue.afficherPlaylistIntrouvable(); return; }

        String recherche = vue.demanderRechercheMusique();
        List<Morceau> resultats = Morceau.rechercherGlobal(recherche);
        vue.afficherResultatsEcoute(resultats);
        if (resultats.isEmpty()) return;

        int idMorceau = vue.demanderIdMorceau();
        boolean ok = Playlist.ajouterMorceau(idPlaylist, idMorceau);
        if (ok) {
            Morceau m = Morceau.rechercherParId(idMorceau);
            vue.afficherMorceauAjoutePlaylist(m != null ? m.getTitre() : "?", cible.getNom());
        } else {
            vue.afficherMorceauDejaPresent();
        }
    }

    private void retirerMorceauPlaylist() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        if (playlists.isEmpty()) return;

        int idPlaylist = vue.demanderIdPlaylist();
        Playlist cible = trouverPlaylist(playlists, idPlaylist);
        if (cible == null) { vue.afficherPlaylistIntrouvable(); return; }
        vue.afficherContenuPlaylist(cible);

        int idMorceau = vue.demanderIdMorceau();
        if (Playlist.retirerMorceau(idPlaylist, idMorceau)) {
            vue.afficherMorceauRetire();
        } else {
            vue.afficherErreurId();
        }
    }

    private void renommerPlaylist() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        if (playlists.isEmpty()) return;

        int idPlaylist = vue.demanderIdPlaylist();
        Playlist cible = trouverPlaylist(playlists, idPlaylist);
        if (cible == null) { vue.afficherPlaylistIntrouvable(); return; }

        String nouveauNom = vue.demanderNouveauNomPlaylist();
        if (Playlist.renommer(idPlaylist, utilisateur.getID(), nouveauNom)) {
            vue.afficherPlaylistRenommee(cible.getNom(), nouveauNom);
        } else {
            vue.afficherPlaylistIntrouvable();
        }
    }

    private void ecouterPlaylist() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        if (playlists.isEmpty()) return;

        int idPlaylist = vue.demanderIdPlaylist();
        Playlist cible = trouverPlaylist(playlists, idPlaylist);
        if (cible == null) { vue.afficherPlaylistIntrouvable(); return; }

        List<Morceau> morceaux = cible.getMorceaux();
        if (morceaux.isEmpty()) { vue.afficherPlaylistVide(); return; }

        vue.afficherLecturePlaylist(cible.getNom());
        int index = 0;
        while (index < morceaux.size()) {
            Morceau m = morceaux.get(index);
            vue.afficherPochette(index + 1, morceaux.size(), m);
            vue.afficherEcoute(m);
            Historique.enregistrerEcoute(utilisateur.getID(), m);
            int action = vue.afficherControlesLecteur(index > 0, index < morceaux.size() - 1);
            switch (action) {
                case 1: index--; break;
                case 2: index++; break;
                case 3: vue.afficherFinPlaylist(cible.getNom()); return;
                default: index++;
            }
        }
        vue.afficherFinPlaylist(cible.getNom());
    }

    private void supprimerPlaylist() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        if (playlists.isEmpty()) return;

        int idPlaylist = vue.demanderIdPlaylist();
        Playlist cible = trouverPlaylist(playlists, idPlaylist);
        if (cible == null) { vue.afficherPlaylistIntrouvable(); return; }

        if (Playlist.supprimer(idPlaylist, utilisateur.getID())) {
            vue.afficherPlaylistSupprimee(cible.getNom());
        } else {
            vue.afficherPlaylistIntrouvable();
        }
    }

    private Playlist trouverPlaylist(List<Playlist> playlists, int id) {
        for (Playlist p : playlists) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    // ==================== ECOUTE ====================

    private void ecouter(int maxEcoutes) {
        boolean estAbonne = (maxEcoutes == Integer.MAX_VALUE);
        int compteur = 0;
        boolean quitter = false;

        while (!quitter && compteur < maxEcoutes) {
            String recherche = vue.demanderRechercheMusique();
            if (recherche.equalsIgnoreCase("stop") || recherche.equalsIgnoreCase("quitter")) break;

            List<Morceau> resultats = Morceau.rechercherGlobal(recherche);
            if (resultats.isEmpty()) {
                vue.afficherMessage("Aucun morceau trouve.");
                int suite = vue.afficherMenuApresEchecRecherche();
                if (suite == 2) quitter = true;
                continue;
            }

            vue.afficherResultatsEcoute(resultats);
            int idChoisi = vue.demanderIdMorceauEcoute();
            Morceau m = Morceau.rechercherParId(idChoisi);
            if (m == null) { vue.afficherErreurId(); continue; }

            vue.afficherEcoute(m);
            compteur++;
            if (estAbonne) Historique.enregistrerEcoute(utilisateur.getID(), m);

            int choixSuite = vue.afficherMenuApresEcoute(estAbonne ? -1 : maxEcoutes - compteur);
            if (choixSuite == 2) quitter = true;
        }

        if (compteur >= maxEcoutes && !estAbonne) {
            vue.afficherLimiteEcoutesAtteinte();
        }
    }

    // ==================== HISTORIQUE ====================

    private void consulterHistorique() {
        List<Historique> historique = Historique.getHistoriqueClient(utilisateur.getID());
        vue.afficherHistorique(historique);
    }
}