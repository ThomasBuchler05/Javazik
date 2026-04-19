package controller;

import model.*;
import view.VueGraphique;
import view.VueConsole;

import java.io.*;
import java.util.*;

/**
 * Contrôleur principal de l'application JavaZik.
 *
 * <p>Implémente le patron MVC : fait le lien entre le modèle (catalogue,
 * utilisateurs, playlists, historique) et la vue (console ou graphique Swing).
 * Toute la logique de navigation, d'authentification et des opérations CRUD
 * admin est centralisée ici.</p>
 *
 * <p>Ce contrôleur est agnostique vis-à-vis de la vue : il ne manipule que
 * {@link VueConsole}, {@link VueGraphique} en étant un sous-type transparent.</p>
 */
public class ControleurPrincipal {

    private VueConsole vue;
    private Utilisateur utilisateur;

    /**
     * Crée le contrôleur et instancie la vue appropriée.
     *
     * @param graphique {@code true} pour démarrer en mode interface graphique Swing,
     *                  {@code false} pour le mode console
     */
    public ControleurPrincipal(boolean graphique) {
        if (graphique) {
            this.vue = new VueGraphique();
        } else {
            this.vue = new VueConsole();
        }
        this.utilisateur = new Client();
    }

    /**
     * Lance la boucle principale de l'application.
     *
     * <p>Affiche le message de bienvenue puis entre dans la boucle du menu
     * principal jusqu'à ce que l'utilisateur choisisse de quitter.</p>
     */
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

    /**
     * Gère le flux de connexion administrateur.
     *
     * <p>Demande l'adresse mail en boucle jusqu'à en trouver une valide,
     * puis vérifie le mot de passe (3 tentatives maximum).
     * En cas de succès, ouvre le menu administrateur.</p>
     */
    private void connexionAdmin() {
        vue.afficherConnexionAdmin();
        String resultat;
        do {
            String mail = vue.demanderMail();
            resultat = utilisateur.verifierMailAdmin(mail);
            if (resultat.equals("MAIL_NOT_FOUND")) {
                vue.afficherMailIncorrect();
            }
        } while (!resultat.equals("MAIL_FOUND"));

        // Mail admin valide : vérifier le mot de passe
        String mdpSaisi = vue.demanderMdp();
        int tentatives = 0;
        while (!utilisateur.verifierMdp(mdpSaisi) && tentatives < 3) {
            vue.afficherMdpIncorrect();
            mdpSaisi = vue.demanderMdp();
            tentatives++;
        }
        if (utilisateur.verifierMdp(mdpSaisi)) {
            vue.afficherConnexionReussie();
            vue.notifierSessionAdmin(utilisateur.getNOM());
            menuAdmin();
        } else {
            vue.afficherMessage("Trop de tentatives, retour au menu principal.");
        }
    }

    // ==================== CONNEXION CLIENT ====================

    /**
     * Gère le flux de connexion client (abonné).
     *
     * <p>Demande l'adresse mail en boucle jusqu'à en trouver une valide,
     * puis vérifie le mot de passe (3 tentatives maximum).
     * En cas de succès, notifie la vue de la session et ouvre le menu client.</p>
     */
    private void connexionClient() {
        vue.afficherConnexionClient();
        String resultat;
        // Reboucle tant que le mail est introuvable
        do {
            String mail = vue.demanderMail();
            resultat = utilisateur.verifierMailClient(mail);
            if (resultat.equals("MAIL_NOT_FOUND")) {
                vue.afficherMailIncorrect();
            }
        } while (!resultat.equals("MAIL_FOUND"));

        // Mail client valide : vérifier le mot de passe
        String mdpSaisi = vue.demanderMdp();
        int tentatives = 0;
        while (!utilisateur.verifierMdp(mdpSaisi) && tentatives < 3) {
            vue.afficherMdpIncorrect();
            mdpSaisi = vue.demanderMdp();
            tentatives++;
        }
        if (utilisateur.verifierMdp(mdpSaisi)) {
            vue.afficherConnexionReussie();
            if (vue instanceof view.VueGraphique) {
                ((view.VueGraphique) vue).notifierSessionClientAvecId(
                        utilisateur.getNOM(), utilisateur.getID());
            } else {
                vue.notifierSessionClient(utilisateur.getNOM());
            }
            menuClient();
        } else {
            vue.afficherMessage("Trop de tentatives, retour au menu principal.");
        }
    }

    // ==================== INSCRIPTION ====================

    /**
     * Gère le flux d'inscription d'un nouveau compte client.
     *
     * <p>Collecte nom, prénom, email et mot de passe, puis délègue
     * la création du compte au modèle {@link Utilisateur}.</p>
     */
    private void inscription() {
        String nom = vue.demanderNom();
        String prenom = vue.demanderPrenom();
        String email = vue.demanderEmail();
        String mdp = vue.demanderMotDePasse();
        utilisateur.inscrire(nom, prenom, email, mdp);
        vue.afficherInscriptionReussie();
    }

    // ==================== MENU ADMIN ====================

    /**
     * Boucle du menu administrateur.
     *
     * <p>Dispatche les actions CRUD sur les morceaux, albums, artistes,
     * groupes et comptes abonnés, ainsi que la consultation des statistiques.
     * La boucle se termine quand l'utilisateur choisit "Retour" (option 13).</p>
     */
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
                case 14: menuCatalogue(); break;
                default: vue.afficherChoixInvalide();
            }
        } while (choix != 13);
    }

    // ==================== HELPERS ANNULATION ====================

    /**
     * Indique si une saisie entière correspond à un signal d'annulation
     * (valeur spéciale {@link VueGraphique#CANCEL_INT} en mode graphique).
     *
     * @param value la valeur entière lue depuis la vue
     * @return {@code true} si la saisie doit être interprétée comme une annulation
     */
    private boolean isCancelled(int value) {
        return value == VueGraphique.CANCEL_INT;
    }

    /**
     * Indique si une saisie texte correspond à une annulation ou à un champ vide.
     *
     * @param value la chaîne lue depuis la vue, potentiellement {@code null}
     * @return {@code true} si la saisie est {@code null} ou vide après trim
     */
    private boolean isCancelled(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Affiche un message d'annulation standard et retourne {@code true}.
     * Permet un early-return lisible dans les méthodes admin :
     * {@code if (isCancelled(val)) return annuler();}.
     *
     * @return toujours {@code true}
     */
    private boolean annuler() {
        vue.afficherMessage("Action annulee.");
        return true;
    }

    // ==================== ADMIN : AJOUT / SUPPRESSION ====================

    /**
     * Flux d'ajout d'un morceau au catalogue.
     * Affiche les artistes et groupes existants, puis collecte les informations
     * nécessaires (titre, durée, genre, année, interprète). Annule si l'une
     * des saisies est invalide ou vide.
     */
    private void adminAjouterMorceau() {
        vue.afficherListeArtistes(Catalogue.getTousLesArtistes());
        vue.afficherListeGroupes(Catalogue.getTousLesGroupes());

        String titre = vue.demanderTitreMorceau();
        if (isCancelled(titre)) { annuler(); return; }

        int duree = vue.demanderDureeMorceau();
        if (isCancelled(duree)) { annuler(); return; }

        String genre = vue.demanderGenreMorceau();
        if (isCancelled(genre)) { annuler(); return; }

        int annee = vue.demanderAnneeMorceau();
        if (isCancelled(annee)) { annuler(); return; }

        int idArtiste = vue.demanderIdArtisteMorceau();
        if (isCancelled(idArtiste)) { annuler(); return; }

        int idGroupe = vue.demanderIdGroupeMorceau();
        if (isCancelled(idGroupe)) { annuler(); return; }

        Morceau m = Morceau.ajouter(titre, duree, genre, annee, idArtiste, idGroupe);
        vue.afficherMorceauAjoute(m.getId());
    }

    /**
     * Flux de suppression d'un morceau du catalogue.
     * Affiche la liste des morceaux puis demande l'ID à supprimer.
     */
    private void adminSupprimerMorceau() {
        vue.afficherListeMorceaux(Catalogue.getTousLesMorceaux());
        int id = vue.demanderIdSuppression();
        if (isCancelled(id)) { annuler(); return; }

        if (Morceau.supprimer(id)) {
            vue.afficherElementSupprime("Morceau");
            // Rafraîchir la liste après suppression
            vue.afficherListeMorceaux(Catalogue.getTousLesMorceaux());
        } else {
            vue.afficherElementNonTrouve("morceau", id);
        }
    }

    /**
     * Flux d'ajout d'un album au catalogue.
     * Collecte le titre, l'année et l'interprète (artiste ou groupe).
     */
    private void adminAjouterAlbum() {
        vue.afficherListeArtistes(Catalogue.getTousLesArtistes());
        vue.afficherListeGroupes(Catalogue.getTousLesGroupes());

        String titre = vue.demanderTitreAlbum();
        if (isCancelled(titre)) { annuler(); return; }

        int annee = vue.demanderAnneeAlbum();
        if (isCancelled(annee)) { annuler(); return; }

        int idArtiste = vue.demanderIdArtisteAlbum();
        if (isCancelled(idArtiste)) { annuler(); return; }

        int idGroupe = vue.demanderIdGroupeAlbum();
        if (isCancelled(idGroupe)) { annuler(); return; }

        Album a = Album.ajouter(titre, annee, idArtiste, idGroupe);
        vue.afficherAlbumAjoute(a.getId());
    }

    /**
     * Flux de suppression d'un album du catalogue.
     */
    private void adminSupprimerAlbum() {
        vue.afficherListeAlbums(Catalogue.getTousLesAlbums());
        int id = vue.demanderIdSuppression();
        if (isCancelled(id)) { annuler(); return; }

        if (Album.supprimer(id)) {
            vue.afficherElementSupprime("Album");
            vue.afficherListeAlbums(Catalogue.getTousLesAlbums());
        } else {
            vue.afficherElementNonTrouve("album", id);
        }
    }

    /**
     * Flux d'ajout d'un artiste au catalogue.
     * Collecte nom, prénom et nationalité.
     */
    private void adminAjouterArtiste() {
        String nom = vue.demanderNomArtiste();
        if (isCancelled(nom)) { annuler(); return; }

        String prenom = vue.demanderPrenomArtiste();
        if (isCancelled(prenom)) { annuler(); return; }

        String nationalite = vue.demanderNationaliteArtiste();
        if (isCancelled(nationalite)) { annuler(); return; }

        Artiste a = Artiste.ajouter(nom, prenom, nationalite);
        vue.afficherArtisteAjoute(a.getId());
    }

    /**
     * Flux de suppression d'un artiste du catalogue.
     */
    private void adminSupprimerArtiste() {
        vue.afficherListeArtistes(Catalogue.getTousLesArtistes());
        int id = vue.demanderIdSuppression();
        if (isCancelled(id)) { annuler(); return; }

        if (Artiste.supprimer(id)) {
            vue.afficherElementSupprime("Artiste");
            vue.afficherListeArtistes(Catalogue.getTousLesArtistes());
        } else {
            vue.afficherElementNonTrouve("artiste", id);
        }
    }

    /**
     * Flux d'ajout d'un groupe au catalogue.
     * Collecte nom, année de création et nationalité.
     */
    private void adminAjouterGroupe() {
        String nom = vue.demanderNomGroupe();
        if (isCancelled(nom)) { annuler(); return; }

        int date = vue.demanderDateCreationGroupe();
        if (isCancelled(date)) { annuler(); return; }

        String nationalite = vue.demanderNationaliteGroupe();
        if (isCancelled(nationalite)) { annuler(); return; }

        Groupe g = Groupe.ajouter(nom, date, nationalite);
        vue.afficherGroupeAjoute(g.getId());
    }

    /**
     * Flux de suppression d'un groupe du catalogue.
     */
    private void adminSupprimerGroupe() {
        vue.afficherListeGroupes(Catalogue.getTousLesGroupes());
        int id = vue.demanderIdSuppression();
        if (isCancelled(id)) { annuler(); return; }

        if (Groupe.supprimer(id)) {
            vue.afficherElementSupprime("Groupe");
            vue.afficherListeGroupes(Catalogue.getTousLesGroupes());
        } else {
            vue.afficherElementNonTrouve("groupe", id);
        }
    }

    // ==================== ADMIN : ASSOCIATION MORCEAU <-> ALBUM ====================

    /**
     * Flux d'association d'un morceau existant à un album existant.
     *
     * <p>Affiche les albums disponibles, demande l'ID de l'album cible,
     * affiche les morceaux disponibles, demande l'ID du morceau et le numéro
     * de piste, puis effectue l'association via {@link Album#ajouterMorceau}.</p>
     */
    private void adminAjouterMorceauDansAlbum() {
        List<Album> albums = Catalogue.getTousLesAlbums();
        vue.afficherListeAlbums(albums);
        if (albums.isEmpty()) {
            vue.afficherMessage("Aucun album dans le catalogue. Creez d'abord un album.");
            return;
        }

        int idAlbum = vue.demanderIdAlbumAssociation();
        if (isCancelled(idAlbum)) { annuler(); return; }

        Album album = Album.rechercherParId(idAlbum);
        if (album == null) {
            vue.afficherElementNonTrouve("album", idAlbum);
            return;
        }

        vue.afficherDetailsAlbum(album);
        vue.afficherListeMorceaux(Catalogue.getTousLesMorceaux());

        int idMorceau = vue.demanderIdMorceauAssociation();
        if (isCancelled(idMorceau)) { annuler(); return; }

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
        if (isCancelled(numPiste)) { annuler(); return; }

        if (Album.ajouterMorceau(idAlbum, idMorceau, numPiste)) {
            vue.afficherMorceauAjouteDansAlbum(morceau.getTitre(), album.getTitre());
        } else {
            vue.afficherMessage("Erreur lors de l'ajout du morceau dans l'album.");
        }
    }

    // ==================== ADMIN : ASSOCIATION ARTISTE <-> GROUPE ====================

    /**
     * Flux d'ajout d'un artiste existant comme membre d'un groupe existant.
     *
     * <p>Affiche les groupes, demande l'ID du groupe cible, affiche les artistes,
     * demande l'ID de l'artiste, puis effectue l'association via
     * {@link Groupe#ajouterMembre}.</p>
     */
    private void adminAjouterMembreGroupe() {
        List<Groupe> groupes = Catalogue.getTousLesGroupes();
        vue.afficherListeGroupes(groupes);
        if (groupes.isEmpty()) {
            vue.afficherMessage("Aucun groupe dans le catalogue. Creez d'abord un groupe.");
            return;
        }

        int idGroupe = vue.demanderIdGroupeAssociation();
        if (isCancelled(idGroupe)) { annuler(); return; }

        Groupe groupe = Groupe.rechercherParId(idGroupe);
        if (groupe == null) {
            vue.afficherElementNonTrouve("groupe", idGroupe);
            return;
        }

        vue.afficherDetailsGroupe(groupe);
        vue.afficherListeArtistes(Catalogue.getTousLesArtistes());

        int idArtiste = vue.demanderIdArtisteAssociation();
        if (isCancelled(idArtiste)) { annuler(); return; }

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

    /**
     * Affiche la liste des abonnés et propose les actions de gestion
     * (suppression, suspension, réactivation).
     */
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

    /**
     * Charge la liste des abonnés depuis le fichier de persistance.
     *
     * <p>Chaque ligne est découpée par {@code ;} et seules les entrées
     * d'au moins 5 champs sont retournées. Les entrées marquées comme
     * supprimées (champ 6 = "0") sont ignorées.</p>
     *
     * @return la liste des abonnés sous forme de tableaux de chaînes
     */
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

    /**
     * Supprime un abonné du fichier de persistance en retirant sa ligne.
     *
     * <p>Les entrées déjà marquées supprimées (champ 6 = "0") sont conservées
     * telles quelles ; seule la ligne correspondant à {@code idAbonne} avec un
     * statut actif est retirée.</p>
     *
     * @param idAbonne l'identifiant de l'abonné à supprimer
     * @return {@code true} si l'abonné a été trouvé et supprimé, {@code false} sinon
     */
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

    /**
     * Compte le nombre de lignes non vides dans le fichier de persistance,
     * utilisé comme approximation du nombre total d'utilisateurs inscrits.
     *
     * @return le nombre d'utilisateurs, ou 0 si le fichier est absent
     */
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

    /**
     * Récupère les statistiques globales du catalogue et les transmet à la vue.
     * Inclut le nombre de morceaux, albums, artistes, groupes, utilisateurs et
     * écoutes totales.
     */
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

    /**
     * Boucle du menu client (abonné).
     *
     * <p>Propose la consultation du catalogue, la gestion des playlists,
     * l'écoute illimitée et la consultation de l'historique.
     * La boucle se termine quand l'utilisateur choisit "Retour" (option 5).</p>
     */
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

    /**
     * Boucle du menu visiteur (sans compte).
     *
     * <p>Propose la consultation du catalogue et l'écoute limitée à 5 morceaux
     * par session. La boucle se termine quand l'utilisateur choisit "Retour" (option 3).</p>
     */
    private void menuVisiteur() {
        vue.notifierSessionVisiteur();
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

    /**
     * Boucle de navigation dans le catalogue musical.
     *
     * <p>Permet la recherche globale, la consultation par type d'entité
     * (morceaux, albums, artistes, groupes) et le filtrage par genre.
     * Chaque résultat est suivi d'un sous-menu de navigation détaillée
     * ({@link #naviguer()}).</p>
     */
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

    /**
     * Sous-menu de navigation détaillée après un affichage de résultats.
     *
     * <p>Permet de consulter les détails d'un morceau, album, artiste ou groupe
     * par son identifiant. La boucle se termine quand l'utilisateur choisit "Retour".</p>
     */
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

    /**
     * Boucle de gestion des playlists du client connecté.
     *
     * <p>Charge et affiche les playlists existantes au démarrage, puis propose
     * la création, la consultation, l'ajout/retrait de morceaux, la lecture,
     * le renommage et la suppression de playlists.</p>
     */
    private void menuPlaylist() {
        // Charger et afficher les playlists dès l'ouverture de l'écran
        List<Playlist> playlistsInit = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlistsInit);

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

    /**
     * Crée une nouvelle playlist pour le client connecté et notifie la vue.
     */
    private void creerPlaylist() {
        String nom = vue.demanderNomPlaylist();
        Playlist p = Playlist.creer(nom, utilisateur.getID());
        vue.afficherPlaylistCreee(p.getId(), p.getNom());
    }

    /**
     * Affiche toutes les playlists du client connecté ainsi que leur contenu détaillé.
     */
    private void voirPlaylists() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        for (Playlist p : playlists) {
            vue.afficherContenuPlaylist(p);
        }
    }

    /**
     * Ajoute un morceau à une playlist existante du client connecté.
     *
     * <p>En mode graphique, si la recherche retourne le jeton {@code "__bypass__"},
     * l'ID du morceau a déjà été sélectionné par le sélecteur graphique et est
     * récupéré directement via {@link VueConsole#demanderIdMorceau()}.</p>
     */
    private void ajouterMorceauPlaylist() {
        List<Playlist> playlists = Playlist.getPlaylistsClient(utilisateur.getID());
        vue.afficherListePlaylists(playlists);
        if (playlists.isEmpty()) return;

        int idPlaylist = vue.demanderIdPlaylist();
        Playlist cible = trouverPlaylist(playlists, idPlaylist);
        if (cible == null) { vue.afficherPlaylistIntrouvable(); return; }

        String recherche = vue.demanderRechercheMusique();

        int idMorceau;
        if ("__bypass__".equals(recherche)) {
            // Le picker graphique a déjà choisi le morceau — on le récupère directement
            idMorceau = vue.demanderIdMorceau();
        } else {
            List<Morceau> resultats = Morceau.rechercherGlobal(recherche);
            vue.afficherResultatsEcoute(resultats);
            if (resultats.isEmpty()) return;
            idMorceau = vue.demanderIdMorceau();
        }

        boolean ok = Playlist.ajouterMorceau(idPlaylist, idMorceau);
        if (ok) {
            Morceau m = Morceau.rechercherParId(idMorceau);
            vue.afficherMorceauAjoutePlaylist(m != null ? m.getTitre() : "?", cible.getNom());
        } else {
            vue.afficherMorceauDejaPresent();
        }
    }

    /**
     * Retire un morceau d'une playlist du client connecté.
     */
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

    /**
     * Renomme une playlist existante du client connecté.
     */
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

    /**
     * Lance la lecture séquentielle d'une playlist du client connecté.
     *
     * <p>Affiche la pochette et simule l'écoute de chaque morceau. L'utilisateur
     * peut naviguer entre les morceaux (précédent / suivant) ou arrêter la lecture.
     * Chaque écoute est enregistrée dans l'historique.</p>
     */
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
        while (index >= 0 && index < morceaux.size()) {
            Morceau m = morceaux.get(index);
            vue.afficherPochette(index + 1, morceaux.size(), m);
            vue.afficherEcoute(m);
            Historique.enregistrerEcoute(utilisateur.getID(), m);
            int action = vue.afficherControlesLecteur(index > 0, index < morceaux.size() - 1);
            switch (action) {
                case 1: index = Math.max(0, index - 1); break;  // prev — jamais < 0
                case 2: index++; break;                          // next
                case 3: vue.afficherFinPlaylist(cible.getNom()); return; // stop
                default: index++;
            }
        }
        vue.afficherFinPlaylist(cible.getNom());
    }

    /**
     * Supprime une playlist du client connecté après confirmation de son identifiant.
     */
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

    /**
     * Recherche une playlist dans une liste par son identifiant.
     *
     * @param playlists la liste des playlists dans laquelle chercher
     * @param id        l'identifiant de la playlist recherchée
     * @return la playlist correspondante, ou {@code null} si aucune n'est trouvée
     */
    private Playlist trouverPlaylist(List<Playlist> playlists, int id) {
        for (Playlist p : playlists) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    // ==================== ECOUTE ====================

    /**
     * Boucle d'écoute de morceaux, partagée entre clients et visiteurs.
     *
     * <p>Effectue une recherche, affiche les résultats, simule la lecture du
     * morceau choisi et enregistre l'écoute dans l'historique pour les abonnés.
     * La boucle s'arrête quand la limite est atteinte ou quand l'utilisateur
     * choisit de quitter.</p>
     *
     * @param maxEcoutes nombre maximum d'écoutes autorisées pour cette session ;
     *                   {@link Integer#MAX_VALUE} signifie illimité (abonné)
     */
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

    /**
     * Charge et affiche l'historique d'écoute du client connecté.
     */
    private void consulterHistorique() {
        List<Historique> historique = Historique.getHistoriqueClient(utilisateur.getID());
        vue.afficherHistorique(historique);
    }
}