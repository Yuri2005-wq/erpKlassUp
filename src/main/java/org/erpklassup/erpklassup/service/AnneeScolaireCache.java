package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.dao.module2.AnneeScolaireDAO;
import org.erpklassup.erpklassup.models.module2.AnneeScolaire;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache en mémoire des entités AnneeScolaire, tenu à jour pour éviter
 * une requête DB à chaque écriture (Note, Paiement, Inscription...).
 *
 * Invalidation :
 * - explicite après cloturerAnnee()/reouvrirAnnee()/archiverAnnee() dans ce process
 * - automatique par RafraichisseurAnneeScolaire quand la vérification périodique
 *   détecte un changement (clôture décidée sur un autre poste, reçue par sync)
 */
public final class AnneeScolaireCache {

    private static final Map<String, AnneeScolaire> cache = new ConcurrentHashMap<>();
    private static final AnneeScolaireDAO anneeScolaireDAO = new AnneeScolaireDAO();

    private AnneeScolaireCache() {}

    public static AnneeScolaire get(String idAnnee) {
        if (idAnnee == null) {
            throw new IllegalArgumentException("idAnnee ne peut pas être null");
        }
        return cache.computeIfAbsent(idAnnee, id -> {
            Optional<AnneeScolaire> annee = anneeScolaireDAO.findById(id);
            return annee.orElseThrow(() -> new IllegalStateException("AnneeScolaire introuvable : " + id));
        });
    }

    public static void invalidate(String idAnnee) {
        cache.remove(idAnnee);
    }

    public static void invalidateAll() {
        cache.clear();
    }
}
