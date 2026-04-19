package model;

/**
 * Représente un client (abonné) de la plateforme musicale.
 * <p>
 * Un client est un {@link Utilisateur} disposant de droits d'accès standards :
 * écoute de morceaux, gestion de ses playlists, consultation de son historique
 * d'écoute et notation des morceaux.
 * </p>
 * <p>
 * Dans le fichier {@code monfichier.txt}, un client est identifié par le rôle
 * {@code 1} (les administrateurs ont le rôle {@code 0}).
 * Un client peut être suspendu (flag {@code SUSPENDU} dans le fichier) ;
 * dans ce cas, il ne peut plus se connecter jusqu'à réactivation par un administrateur.
 * </p>
 *
 * @see Utilisateur
 * @see Administrateur
 * @see Playlist
 * @see Historique
 */
public class Client extends Utilisateur {

}