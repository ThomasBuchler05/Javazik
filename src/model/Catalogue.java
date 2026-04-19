package model;

import java.util.*;

/**
 * Classe centrale du catalogue musical de la plateforme.
 * <p>
 * Fournit un point d'accès unique pour toutes les opérations de recherche,
 * de navigation entre entités et de statistiques globales. Cette classe
 * respecte le pattern MVC : elle ne contient aucune instruction d'affichage
 * et délègue la persistance aux classes modèles ({@link Morceau}, {@link Album},
 * {@link Artiste}, {@link Groupe}).
 * </p>
 * <p>
 * Toutes les méthodes sont statiques ; la classe ne doit pas être instanciée.
 * </p>
 *
 * @see Morceau
 * @see Album
 * @see Artiste
 * @see Groupe
 */
public class Catalogue {

    // ==================== RECHERCHE GLOBALE ====================

    /**
     * Effectue une recherche globale dans tout le catalogue sur les quatre
     * entités : morceaux, albums, artistes et groupes.
     * <p>
     * La recherche est partielle et insensible à la casse. Elle porte sur
     * le titre/nom principal de chaque entité et, pour les morceaux,
     * également sur l'interprète et le genre.
     * </p>
     *
     * @param recherche la chaîne à rechercher
     * @return un objet {@link ResultatRecherche} contenant les résultats
     *         répartis par catégorie
     */
    public static ResultatRecherche rechercherGlobal(String recherche) {
        ResultatRecherche r = new ResultatRecherche();
        r.morceaux = Morceau.rechercherGlobal(recherche);
        r.albums = Album.rechercherParTitre(recherche);
        r.artistes = Artiste.rechercherParNom(recherche);
        r.groupes = Groupe.rechercherParNom(recherche);
        return r;
    }

    // ==================== NAVIGATION : DEPUIS UN MORCEAU ====================

    /**
     * Retourne l'interprète (artiste ou groupe) associé à un morceau donné.
     * <p>
     * Le tableau retourné contient toujours deux éléments :
     * <ul>
     *   <li>index 0 : l'{@link Artiste} solo, ou {@code null}</li>
     *   <li>index 1 : le {@link Groupe}, ou {@code null}</li>
     * </ul>
     * Les deux ne sont pas {@code null} simultanément (sauf cas de données
     * incohérentes).
     * </p>
     *
     * @param m le morceau dont on veut connaître l'interprète
     * @return tableau {@code Object[2]} : {@code [Artiste|null, Groupe|null]}
     */
    public static Object[] getInterpreteDepuisMorceau(Morceau m) {
        Artiste artiste = null;
        Groupe groupe = null;
        if (m.getIdGroupe() > 0) {
            groupe = Groupe.rechercherParId(m.getIdGroupe());
        }
        if (m.getIdArtiste() > 0) {
            artiste = Artiste.rechercherParId(m.getIdArtiste());
        }
        return new Object[]{ artiste, groupe };
    }

    /**
     * Retourne la liste de tous les albums contenant un morceau donné.
     *
     * @param idMorceau l'ID du morceau
     * @return liste des albums contenant ce morceau (peut être vide)
     */
    public static List<Album> getAlbumsDepuisMorceau(int idMorceau) {
        return Album.getAlbumsParMorceau(idMorceau);
    }

    /**
     * Retourne les autres morceaux du même interprète que le morceau fourni,
     * en excluant ce dernier de la liste.
     *
     * @param m le morceau de référence
     * @return liste des autres morceaux du même interprète (peut être vide)
     */
    public static List<Morceau> getAutresMorceauxMemeInterprete(Morceau m) {
        List<Morceau> autres;
        if (m.getIdGroupe() > 0) {
            autres = Morceau.getMorceauxParGroupe(m.getIdGroupe());
        } else {
            autres = Morceau.getMorceauxParArtiste(m.getIdArtiste());
        }
        autres.removeIf(mo -> mo.getId() == m.getId());
        return autres;
    }

    // ==================== NAVIGATION : DEPUIS UN ALBUM ====================

    /**
     * Retourne la liste ordonnée des morceaux d'un album.
     *
     * @param idAlbum l'ID de l'album
     * @return liste des morceaux de l'album dans l'ordre des pistes,
     *         ou liste vide si l'album est introuvable
     */
    public static List<Morceau> getMorceauxDepuisAlbum(int idAlbum) {
        Album a = Album.rechercherParId(idAlbum);
        return (a != null) ? a.getMorceaux() : new ArrayList<>();
    }

    /**
     * Retourne les autres albums du même interprète que l'album fourni,
     * en excluant ce dernier de la liste.
     *
     * @param a l'album de référence
     * @return liste des autres albums du même interprète (peut être vide)
     */
    public static List<Album> getAutresAlbumsMemeInterprete(Album a) {
        List<Album> autres;
        if (a.getIdGroupe() > 0) {
            autres = Album.getAlbumsParGroupe(a.getIdGroupe());
        } else {
            autres = Album.getAlbumsParArtiste(a.getIdArtiste());
        }
        autres.removeIf(al -> al.getId() == a.getId());
        return autres;
    }

    // ==================== NAVIGATION : DEPUIS UN ARTISTE ====================

    /**
     * Retourne tous les morceaux d'un artiste solo.
     *
     * @param idArtiste l'ID de l'artiste
     * @return liste des morceaux de cet artiste (peut être vide)
     */
    public static List<Morceau> getMorceauxDepuisArtiste(int idArtiste) {
        return Morceau.getMorceauxParArtiste(idArtiste);
    }

    /**
     * Retourne tous les albums d'un artiste solo.
     *
     * @param idArtiste l'ID de l'artiste
     * @return liste des albums de cet artiste (peut être vide)
     */
    public static List<Album> getAlbumsDepuisArtiste(int idArtiste) {
        return Album.getAlbumsParArtiste(idArtiste);
    }

    /**
     * Retourne tous les groupes auxquels appartient un artiste donné.
     *
     * @param idArtiste l'ID de l'artiste
     * @return liste des groupes dont l'artiste est membre (peut être vide)
     */
    public static List<Groupe> getGroupesDepuisArtiste(int idArtiste) {
        return Groupe.getGroupesParArtiste(idArtiste);
    }

    // ==================== NAVIGATION : DEPUIS UN GROUPE ====================

    /**
     * Retourne tous les morceaux d'un groupe.
     *
     * @param idGroupe l'ID du groupe
     * @return liste des morceaux de ce groupe (peut être vide)
     */
    public static List<Morceau> getMorceauxDepuisGroupe(int idGroupe) {
        return Morceau.getMorceauxParGroupe(idGroupe);
    }

    /**
     * Retourne tous les albums d'un groupe.
     *
     * @param idGroupe l'ID du groupe
     * @return liste des albums de ce groupe (peut être vide)
     */
    public static List<Album> getAlbumsDepuisGroupe(int idGroupe) {
        return Album.getAlbumsParGroupe(idGroupe);
    }

    /**
     * Retourne tous les membres d'un groupe.
     *
     * @param idGroupe l'ID du groupe
     * @return liste des artistes membres du groupe, ou liste vide si introuvable
     */
    public static List<Artiste> getMembresDepuisGroupe(int idGroupe) {
        Groupe g = Groupe.rechercherParId(idGroupe);
        return (g != null) ? g.getMembres() : new ArrayList<>();
    }

    // ==================== LISTES COMPLETES ====================

    /**
     * Retourne tous les morceaux du catalogue.
     * @return liste complète des morceaux
     */
    public static List<Morceau> getTousLesMorceaux() {
        return Morceau.chargerTous();
    }

    /**
     * Retourne tous les albums du catalogue.
     * @return liste complète des albums
     */
    public static List<Album> getTousLesAlbums() {
        return Album.chargerTous();
    }

    /**
     * Retourne tous les artistes du catalogue.
     * @return liste complète des artistes
     */
    public static List<Artiste> getTousLesArtistes() {
        return Artiste.chargerTous();
    }

    /**
     * Retourne tous les groupes du catalogue.
     * @return liste complète des groupes
     */
    public static List<Groupe> getTousLesGroupes() {
        return Groupe.chargerTous();
    }

    /**
     * Retourne la liste triée alphabétiquement de tous les genres musicaux
     * disponibles dans le catalogue.
     *
     * @return liste des genres distincts
     */
    public static List<String> getGenresDisponibles() {
        return Morceau.getGenresDisponibles();
    }

    /**
     * Retourne tous les morceaux d'un genre musical donné.
     *
     * @param genre le genre recherché
     * @return liste des morceaux de ce genre (peut être vide)
     */
    public static List<Morceau> getMorceauxParGenre(String genre) {
        return Morceau.getMorceauxParGenre(genre);
    }

    // ==================== STATISTIQUES ====================

    /**
     * Retourne le nombre total de morceaux dans le catalogue.
     * @return nombre de morceaux
     */
    public static int getNombreMorceaux() {
        return Morceau.chargerTous().size();
    }

    /**
     * Retourne le nombre total d'albums dans le catalogue.
     * @return nombre d'albums
     */
    public static int getNombreAlbums() {
        return Album.chargerTous().size();
    }

    /**
     * Retourne le nombre total d'artistes dans le catalogue.
     * @return nombre d'artistes
     */
    public static int getNombreArtistes() {
        return Artiste.chargerTous().size();
    }

    /**
     * Retourne le nombre total de groupes dans le catalogue.
     * @return nombre de groupes
     */
    public static int getNombreGroupes() {
        return Groupe.chargerTous().size();
    }

    // ==================== CLASSE INTERNE RESULTAT ====================

    /**
     * Conteneur des résultats d'une recherche globale dans le catalogue.
     * <p>
     * Regroupe les résultats par catégorie (morceaux, albums, artistes, groupes)
     * et expose des méthodes utilitaires pour vérifier si la recherche a produit
     * des résultats et pour en connaître le total.
     * </p>
     *
     * @see Catalogue#rechercherGlobal(String)
     */
    public static class ResultatRecherche {

        /** Liste des morceaux correspondant à la recherche. */
        public List<Morceau> morceaux = new ArrayList<>();

        /** Liste des albums correspondant à la recherche. */
        public List<Album> albums = new ArrayList<>();

        /** Liste des artistes correspondant à la recherche. */
        public List<Artiste> artistes = new ArrayList<>();

        /** Liste des groupes correspondant à la recherche. */
        public List<Groupe> groupes = new ArrayList<>();

        /**
         * Retourne la liste des morceaux trouvés.
         * @return liste des morceaux
         */
        public List<Morceau> getMorceaux()  { return morceaux; }

        /**
         * Retourne la liste des albums trouvés.
         * @return liste des albums
         */
        public List<Album>   getAlbums()    { return albums; }

        /**
         * Retourne la liste des artistes trouvés.
         * @return liste des artistes
         */
        public List<Artiste> getArtistes()  { return artistes; }

        /**
         * Retourne la liste des groupes trouvés.
         * @return liste des groupes
         */
        public List<Groupe>  getGroupes()   { return groupes; }

        /**
         * Indique si la recherche n'a retourné aucun résultat dans aucune catégorie.
         *
         * @return {@code true} si toutes les listes sont vides, {@code false} sinon
         */
        public boolean estVide() {
            return morceaux.isEmpty() && albums.isEmpty()
                    && artistes.isEmpty() && groupes.isEmpty();
        }

        /**
         * Retourne le nombre total de résultats toutes catégories confondues.
         *
         * @return somme des tailles des quatre listes de résultats
         */
        public int getNombreTotal() {
            return morceaux.size() + albums.size()
                    + artistes.size() + groupes.size();
        }
    }
}