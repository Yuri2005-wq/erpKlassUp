package org.erpklassup.erpklassup.util;

/**
 * Interface implémentée par les controllers de pages qui ont des
 * sous-sections (onglets internes) et veulent contrôler leur breadcrumb.
 *
 * <p>Le {@code ViewRegistry} appelle {@link #getSousSectionCourante()} à chaque
 * affichage de la vue, ce qui permet de restaurer automatiquement la
 * sous-section mémorisée.</p>
 */
public interface VueAvecSousSection {

    /**
     * Retourne la sous-section actuellement active dans cette page
     * (ex: "Configuration infos École").
     * Retourner {@code null} ou "" si aucune sous-section n'est active.
     */
    String getSousSectionCourante();

    /**
     * Appelée par le {@code ViewRegistry} quand la vue est (ré)affichée.
     * Le controller peut alors remettre à jour sa sous-section.
     */
    default void onAffichee() {}
}