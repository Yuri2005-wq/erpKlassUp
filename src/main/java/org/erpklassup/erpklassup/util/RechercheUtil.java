package org.erpklassup.erpklassup.util;

import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Utilitaire pour configurer une barre de recherche avec :
 * <ul>
 *   <li>Un <b>debounce</b> (délai anti-rebond) pour ne pas déclencher
 *       la recherche à chaque frappe</li>
 *   <li>Un <b>bouton clear</b> qui apparaît automatiquement quand le champ
 *       contient du texte et le vide au clic</li>
 *   <li>Un <b>callback</b> appelé après le debounce</li>
 * </ul>
 *
 * <p><b>Utilisation :</b></p>
 * <pre>{@code
 * RechercheUtil.configurer(searchAnneeField, clearSearchAnnee,
 *     () -> autoRefresh.restartNow());
 * }</pre>
 *
 * <p>La méthode retourne le {@link PauseTransition} pour permettre
 * un contrôle avancé si nécessaire.</p>
 */
public final class RechercheUtil {

    /** Délai par défaut du debounce (millisecondes). */
    public static final int DEBOUNCE_MS_DEFAUT = 350;

    private RechercheUtil() {
        throw new UnsupportedOperationException("Classe utilitaire — ne pas instancier");
    }

    /* ============================================================
       MÉTHODE PRINCIPALE
       ============================================================ */

    /**
     * Configure une barre de recherche complète :
     * debounce + bouton clear + callback.
     *
     * @param champRecherche Le TextField de saisie
     * @param boutonClear    Le bouton pour vider le champ (peut être null)
     * @param action         Le callback à exécuter après le debounce
     */
    public static void configurer(TextField champRecherche,
                                  Button boutonClear,
                                  Runnable action) {
        configurer(champRecherche, boutonClear, action, DEBOUNCE_MS_DEFAUT);
    }

    /**
     * Version avec délai de debounce personnalisé.
     */
    public static PauseTransition configurer(TextField champRecherche,
                                             Button boutonClear,
                                             Runnable action,
                                             int debounceMs) {

        if (champRecherche == null) {
            throw new IllegalArgumentException("Le champ de recherche ne peut pas être null");
        }

        // 1. Créer le debounce
        PauseTransition debounce = new PauseTransition(Duration.millis(debounceMs));
        debounce.setOnFinished(e -> {
            if (action != null) action.run();
        });

        // 2. Masquer le bouton clear au démarrage
        if (boutonClear != null) {
            boutonClear.setVisible(false);
            boutonClear.setManaged(false);
        }

        // 3. Écouter les changements du champ
        champRecherche.textProperty().addListener((obs, ancien, nouveau) -> {
            boolean aDuTexte = nouveau != null && !nouveau.trim().isEmpty();

            // Afficher/masquer le bouton clear
            if (boutonClear != null) {
                boutonClear.setVisible(aDuTexte);
                boutonClear.setManaged(aDuTexte);
            }

            // Relancer le debounce
            debounce.playFromStart();
        });

        // 4. Action du bouton clear
        if (boutonClear != null) {
            boutonClear.setOnAction(e -> {
                champRecherche.clear();
                champRecherche.requestFocus();
                // Le listener va automatiquement masquer le bouton
                // et relancer le debounce
            });
        }

        return debounce;
    }

    /* ============================================================
       VARIANTES UTILES
       ============================================================ */

    /**
     * Version sans bouton clear (juste debounce + callback).
     */
    public static PauseTransition configurer(TextField champRecherche,
                                             Runnable action) {
        return configurer(champRecherche, null, action, DEBOUNCE_MS_DEFAUT);
    }

    /**
     * Version avec Consumer<String> au lieu de Runnable.
     * Le callback reçoit le texte saisi en paramètre.
     */
    public static PauseTransition configurerAvecTexte(TextField champRecherche,
                                                      Button boutonClear,
                                                      Consumer<String> action) {
        return configurer(champRecherche,
                () -> {
                    String texte = champRecherche.getText();
                    if (action != null) action.accept(texte != null ? texte.trim() : "");
                });
    }

    /**
     * Vide un champ de recherche et masque le bouton clear.
     * Utile pour un reset programmatique.
     */
    public static void vider(TextField champRecherche, Button boutonClear) {
        if (champRecherche != null) champRecherche.clear();
        if (boutonClear != null) {
            boutonClear.setVisible(false);
            boutonClear.setManaged(false);
        }
    }

    /**
     * Retourne true si le champ contient du texte.
     */
    public static boolean aDuTexte(TextField champRecherche) {
        if (champRecherche == null) return false;
        String texte = champRecherche.getText();
        return texte != null && !texte.trim().isEmpty();
    }
}