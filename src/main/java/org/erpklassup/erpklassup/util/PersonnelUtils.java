package org.erpklassup.erpklassup.util;

import org.erpklassup.erpklassup.dao.EcoleDAO;
import org.erpklassup.erpklassup.models.Ecole;

import java.time.Year;
import java.util.Optional;
import java.util.Random;

public class PersonnelUtils {

    private static final Random RANDOM = new Random();
    private static final EcoleDAO ecoleDAO = new EcoleDAO();

    /**
     * Calcule ou récupère les initiales de l'école active pour une école donnée.
     *
     * @param idEcole L'ID de l'école courante
     * @return Les initiales (ex: "FIS")
     */
    public static String obtenirInitialesEcole(String idEcole) {
        if (idEcole != null && !idEcole.isBlank()) {
            Optional<Ecole> ecoleOpt = ecoleDAO.findById(idEcole);
            if (ecoleOpt.isPresent()) {
                Ecole ecole = ecoleOpt.get();
                // Si le code école existe et est court (ex: "FIS"), on l'utilise en priorité
                if (ecole.getCodeEcole() != null && !ecole.getCodeEcole().isBlank()) {
                    return ecole.getCodeEcole().trim().toUpperCase();
                }
                // Sinon, on calcule les initiales à partir du nom complet de l'école
                if (ecole.getNomEcole() != null && !ecole.getNomEcole().isBlank()) {
                    return calculerInitialesDepuisNom(ecole.getNomEcole());
                }
            }
        }

        // Fallback sur la première école active si l'ID est introuvable
        Optional<Ecole> premiereEcole = ecoleDAO.findPremiereEcoleActive();
        if (premiereEcole.isPresent() && premiereEcole.get().getCodeEcole() != null) {
            return premiereEcole.get().getCodeEcole().trim().toUpperCase();
        }

        return "ECO"; // Valeur par défaut de secours
    }

    /**
     * Extrait les initiales d'un nom d'école (ex: "Foyer International Scolaire" -> "FIS").
     * Ignore automatiquement les petits mots de liaison (de, la, et, des, etc.).
     */
    public static String calculerInitialesDepuisNom(String nomEcole) {
        if (nomEcole == null || nomEcole.isBlank()) {
            return "ECO";
        }

        String[] mots = nomEcole.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (String mot : mots) {
            // Nettoyage des caractères spéciaux potentiels
            String motNettoye = mot.replaceAll("[^a-zA-ZÀ-ÿ]", "");
            if (!motNettoye.isEmpty()) {
                String motMin = motNettoye.toLowerCase();
                // Ignorer les mots de liaison courants
                if (!motMin.matches("de|la|et|du|les|des|le|l|d|au|aux|en|sur|par")) {
                    sb.append(Character.toUpperCase(motNettoye.charAt(0)));
                }
            }
        }

        // Si aucun mot significatif n'a été trouvé, on prend les 3 premières lettres du nom
        if (sb.length() == 0) {
            String clean = nomEcole.replaceAll("[^a-zA-Z]", "");
            return clean.length() >= 3 ? clean.substring(0, 3).toUpperCase() : "ECO";
        }

        return sb.toString();
    }

    /**
     * Génère un matricule complet au format : [InitialesÉcole][Année][4ChiffresAléatoires]
     * Exemple : FIS20261163
     *
     * @param idEcole L'ID de l'école courante pour récupérer ses initiales
     * @return Le matricule généré
     */
    public static String genererMatricule(String idEcole) {
        String initiales = obtenirInitialesEcole(idEcole);
        String emp = "EMP";
        int anneeCourante = Year.now().getValue();
        int nombreAleatoire = 1000 + RANDOM.nextInt(9000); // Nombre aléatoire entre 1000 et 9999

        return initiales + emp +  anneeCourante + nombreAleatoire;
    }

    public static String genererMatriculeEleve(String idEcole) {
        String initiales = obtenirInitialesEcole(idEcole);
        String emp = "ELV";
        int anneeCourante = Year.now().getValue();
        int nombreAleatoire = 1000 + RANDOM.nextInt(9000); // Nombre aléatoire entre 1000 et 9999

        return initiales + emp +  anneeCourante + nombreAleatoire;
    }
}