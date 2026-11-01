package org.erpklassup.erpklassup.util;

import javafx.collections.ObservableList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fusionne le résultat d'un rafraîchissement (venant de la base) dans une
 * {@link ObservableList} affichée par un TableView, SANS remplacer la liste
 * entière.
 * <p>
 * Contrairement à {@code liste.setAll(nouveaux)} ou {@code table.setItems(...)},
 * cette méthode ne modifie QUE les lignes qui ont réellement changé :
 * <ul>
 *   <li>ligne identique (equals) → aucune opération, aucun redraw</li>
 *   <li>ligne modifiée → remplacement ciblé (une seule ligne redessinée)</li>
 *   <li>ligne disparue côté serveur → suppression ciblée</li>
 *   <li>nouvelle ligne (ex : insérée par un autre utilisateur) → insertion
 *       ciblée à sa position naturelle</li>
 * </ul>
 * Ceci préserve le scroll, la sélection (si elle est suivie par index/objet),
 * le tri de colonne et toute édition de cellule en cours, puisque JavaFX ne
 * reçoit que des évènements de changement granulaires au lieu d'un
 * "tout a changé".
 * <p>
 * Prérequis : le type {@code T} doit avoir un {@code equals()} basé sur le
 * contenu (c'est le cas par défaut pour un {@code record}, ce qui est
 * idéal ici).
 */
public final class FusionListeUtil {

    private FusionListeUtil() {
    }

    /**
     * @param cible            la liste actuellement affichée par le TableView
     *                         (ex : {@code tableUtilisateurs.getItems()}, ou la
     *                         master-list si elle est enveloppée dans un
     *                         SortedList/FilteredList)
     * @param nouveauxElements le résultat frais chargé depuis la base
     * @param extracteurId     fonction extrayant l'identifiant unique d'un élément
     */
    public static <T> void fusionner(ObservableList<T> cible,
                                     List<T> nouveauxElements,
                                     Function<T, String> extracteurId) {

        // Index rapide id -> nouvel élément, en conservant l'ordre serveur
        Map<String, T> nouveauxParId = nouveauxElements.stream()
                .collect(Collectors.toMap(
                        extracteurId,
                        Function.identity(),
                        (existant, remplacant) -> existant,
                        LinkedHashMap::new
                ));

        // 1) Supprimer les lignes qui n'existent plus côté serveur
        //    (on parcourt à l'envers pour que les suppressions ne décalent
        //    pas les index restant à traiter)
        for (int i = cible.size() - 1; i >= 0; i--) {
            String id = extracteurId.apply(cible.get(i));
            if (!nouveauxParId.containsKey(id)) {
                cible.remove(i);
            }
        }

        // 2) Mettre à jour les lignes modifiées + insérer les nouvelles
        for (int i = 0; i < nouveauxElements.size(); i++) {
            T nouveau = nouveauxElements.get(i);
            String id = extracteurId.apply(nouveau);

            int indexActuel = indexDe(cible, id, extracteurId);

            if (indexActuel == -1) {
                // Ligne absente localement → insérée par quelqu'un d'autre.
                // On l'insère à la position correspondant à l'ordre serveur.
                int position = Math.min(i, cible.size());
                cible.add(position, nouveau);
            } else if (!nouveau.equals(cible.get(indexActuel))) {
                // Contenu différent → remplacement ciblé d'une seule ligne
                cible.set(indexActuel, nouveau);
            }
            // sinon : ligne strictement identique → on ne touche à rien
        }
    }

    private static <T> int indexDe(ObservableList<T> liste, String id, Function<T, String> extracteurId) {
        for (int i = 0; i < liste.size(); i++) {
            if (extracteurId.apply(liste.get(i)).equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
