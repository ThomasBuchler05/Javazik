package model;

/**
 * Représente un administrateur de la plateforme musicale.
 * <p>
 * Un administrateur est un {@link Utilisateur} disposant de droits étendus,
 * notamment la gestion du catalogue (ajout/suppression de morceaux, albums,
 * artistes, groupes) et la gestion des comptes abonnés (suspension, réactivation).
 * </p>
 * <p>
 * Dans le fichier {@code monfichier.txt}, un administrateur est identifié
 * par le rôle {@code 0} (les clients ont le rôle {@code 1}).
 * </p>
 *
 * @see Utilisateur
 * @see Client
 */
public class Administrateur extends Utilisateur {

    /**
     * Construit un administrateur avec les valeurs par défaut héritées
     * de {@link Utilisateur}.
     */
    public Administrateur() {
        super();
    }
}