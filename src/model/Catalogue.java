package model;

import java.util.*;

/**
 * Classe centrale du catalogue musical.
 * Fournit les méthodes de recherche, de navigation et les statistiques.
 * Ne contient aucune instruction d'affichage (respect MVC).
 */
public class Catalogue {

    // ==================== RECHERCHE GLOBALE ====================

    /**
     * Recherche globale dans tout le catalogue (morceaux, albums, artistes, groupes).
     * Retourne un objet ResultatRecherche contenant les résultats par catégorie.
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
     * A partir d'un morceau, retourne l'artiste ou le groupe interprète.
     * Retourne un Object[] : [0]=Artiste ou null, [1]=Groupe ou null
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
     * A partir d'un morceau, retourne les albums qui le contiennent.
     */
    public static List<Album> getAlbumsDepuisMorceau(int idMorceau) {
        return Album.getAlbumsParMorceau(idMorceau);
    }

    /**
     * A partir d'un morceau, retourne les autres morceaux du même interprète.
     */
    public static List<Morceau> getAutresMorceauxMemeInterprete(Morceau m) {
        List<Morceau> autres;
        if (m.getIdGroupe() > 0) {
            autres = Morceau.getMorceauxParGroupe(m.getIdGroupe());
        } else {
            autres = Morceau.getMorceauxParArtiste(m.getIdArtiste());
        }
        // retirer le morceau courant de la liste
        autres.removeIf(mo -> mo.getId() == m.getId());
        return autres;
    }

    // ==================== NAVIGATION : DEPUIS UN ALBUM ====================

    /**
     * A partir d'un album, retourne la liste ordonnée des morceaux.
     */
    public static List<Morceau> getMorceauxDepuisAlbum(int idAlbum) {
        Album a = Album.rechercherParId(idAlbum);
        return (a != null) ? a.getMorceaux() : new ArrayList<>();
    }

    /**
     * A partir d'un album, retourne les autres albums du même interprète.
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
     * A partir d'un artiste, retourne ses morceaux.
     */
    public static List<Morceau> getMorceauxDepuisArtiste(int idArtiste) {
        return Morceau.getMorceauxParArtiste(idArtiste);
    }

    /**
     * A partir d'un artiste, retourne ses albums.
     */
    public static List<Album> getAlbumsDepuisArtiste(int idArtiste) {
        return Album.getAlbumsParArtiste(idArtiste);
    }

    /**
     * A partir d'un artiste, retourne les groupes auxquels il appartient.
     */
    public static List<Groupe> getGroupesDepuisArtiste(int idArtiste) {
        return Groupe.getGroupesParArtiste(idArtiste);
    }

    // ==================== NAVIGATION : DEPUIS UN GROUPE ====================


    /**
     * A partir d'un groupe, retourne ses morceaux.
     */
    public static List<Morceau> getMorceauxDepuisGroupe(int idGroupe) {
        return Morceau.getMorceauxParGroupe(idGroupe);
    }

    /**
     * A partir d'un groupe, retourne ses albums.
     */
    public static List<Album> getAlbumsDepuisGroupe(int idGroupe) {
        return Album.getAlbumsParGroupe(idGroupe);
    }

    /**
     * A partir d'un groupe, retourne ses membres.
     */
    public static List<Artiste> getMembresDepuisGroupe(int idGroupe) {
        Groupe g = Groupe.rechercherParId(idGroupe);
        return (g != null) ? g.getMembres() : new ArrayList<>();
    }

    // ==================== LISTES COMPLETES ====================

    public static List<Morceau> getTousLesMorceaux() {
        return Morceau.chargerTous();
    }

    public static List<Album> getTousLesAlbums() {
        return Album.chargerTous();
    }

    public static List<Artiste> getTousLesArtistes() {
        return Artiste.chargerTous();
    }

    public static List<Groupe> getTousLesGroupes() {
        return Groupe.chargerTous();
    }

    public static List<String> getGenresDisponibles() {
        return Morceau.getGenresDisponibles();
    }

    public static List<Morceau> getMorceauxParGenre(String genre) {
        return Morceau.getMorceauxParGenre(genre);
    }

    // ==================== STATISTIQUES ====================

    public static int getNombreMorceaux() {
        return Morceau.chargerTous().size();
    }

    public static int getNombreAlbums() {
        return Album.chargerTous().size();
    }

    public static int getNombreArtistes() {
        return Artiste.chargerTous().size();
    }

    public static int getNombreGroupes() {
        return Groupe.chargerTous().size();
    }

    // ==================== CLASSE INTERNE RESULTAT ====================

    /**
     * Contient les résultats d'une recherche globale, séparés par catégorie.
     */
    public static class ResultatRecherche {
        public List<Morceau> morceaux = new ArrayList<>();
        public List<Album> albums = new ArrayList<>();
        public List<Artiste> artistes = new ArrayList<>();
        public List<Groupe> groupes = new ArrayList<>();

        // ← ajouter ici
        public List<Morceau> getMorceaux()  { return morceaux; }
        public List<Album>   getAlbums()    { return albums; }
        public List<Artiste> getArtistes()  { return artistes; }
        public List<Groupe>  getGroupes()   { return groupes; }

        public boolean estVide() {
            return morceaux.isEmpty() && albums.isEmpty()
                   && artistes.isEmpty() && groupes.isEmpty();
        }

        public int getNombreTotal() {
            return morceaux.size() + albums.size()
                   + artistes.size() + groupes.size();
        }
    }
}
