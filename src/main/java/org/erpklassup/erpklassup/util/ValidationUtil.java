package org.erpklassup.erpklassup.util;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Utilitaire centralisé pour la validation des champs de saisie JavaFX.
 *
 * Fournit :
 *  - Contraintes de TYPE (int, long, double, alphabétique, alphanumérique)
 *  - Contraintes de FORMAT (email, téléphone, code postal...)
 *  - Contraintes de LONGUEUR (min, max)
 *  - Contraintes de DATE (pas dans le passé, pas dans le futur, plage)
 *  - Forçage en MAJUSCULES
 *  - Nettoyage (trim, suppression caractères)
 *  - Alertes automatiques en cas d'erreur via AlertUtil
 *
 * Toutes les méthodes retournent un boolean : true = valide, false = invalide.
 */
public final class ValidationUtil {

    private ValidationUtil() {
        throw new UnsupportedOperationException("Classe utilitaire — ne pas instancier");
    }

    // ==========================================
    // 1. CONTRAINTES DE TYPE (TextFields)
    // ==========================================

    /** Force le champ à n'accepter que des chiffres (entier positif). */
    public static void forcerEntier(TextField field) {
        forcerEntier(field, false);
    }

    /** Force le champ à n'accepter que des chiffres, avec option négatif. */
    public static void forcerEntier(TextField field, boolean autoriserNegatif) {
        if (field == null) return;
        field.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            if (autoriserNegatif) {
                return newText.matches("-?\\d*") ? change : null;
            }
            return newText.matches("\\d*") ? change : null;
        }));
    }

    /** Force le champ à n'accepter qu'un nombre décimal. */
    public static void forcerDecimal(TextField field) {
        forcerDecimal(field, false);
    }

    /** Force le champ à n'accepter qu'un nombre décimal (avec option négatif). */
    public static void forcerDecimal(TextField field, boolean autoriserNegatif) {
        if (field == null) return;
        field.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            String regex = autoriserNegatif ? "-?\\d*(\\.\\d*)?" : "\\d*(\\.\\d*)?";
            return newText.matches(regex) ? change : null;
        }));
    }

    /** Force le champ à n'accepter que des lettres (alphabétique). */
    public static void forcerAlphabetique(TextField field) {
        if (field == null) return;
        field.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            return newText.matches("[a-zA-ZÀ-ÿ\\s'-]*") ? change : null;
        }));
    }

    /** Force le champ à n'accepter qu'alphanumérique (lettres + chiffres). */
    public static void forcerAlphanumerique(TextField field) {
        if (field == null) return;
        field.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            return newText.matches("[a-zA-Z0-9À-ÿ._-]*") ? change : null;
        }));
    }

    // ==========================================
    // 2. FORÇAGE EN MAJUSCULES
    // ==========================================

    /** Convertit automatiquement tout le texte en MAJUSCULES à la saisie. */
    public static void forcerMajuscules(TextField field) {
        if (field == null) return;
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(newVal.toUpperCase())) {
                field.setText(newVal.toUpperCase());
                field.positionCaret(field.getText().length());
            }
        });
    }

    /** Convertit automatiquement tout le texte en minuscules. */
    public static void forcerMinuscules(TextField field) {
        if (field == null) return;
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(newVal.toLowerCase())) {
                field.setText(newVal.toLowerCase());
                field.positionCaret(field.getText().length());
            }
        });
    }

    /** Force une casse "Titre" (première lettre de chaque mot en majuscule). */
    public static void forcerCasseTitre(TextField field) {
        if (field == null) return;
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && field.getText() != null && !field.getText().isBlank()) {
                String[] mots = field.getText().trim().split("\\s+");
                StringBuilder sb = new StringBuilder();
                for (String mot : mots) {
                    if (!mot.isEmpty()) {
                        sb.append(Character.toUpperCase(mot.charAt(0)))
                                .append(mot.substring(1).toLowerCase())
                                .append(" ");
                    }
                }
                field.setText(sb.toString().trim());
            }
        });
    }

    // ==========================================
    // 3. CONTRAINTES DE LONGUEUR
    // ==========================================

    /**
     * Force une longueur maximale au champ.
     * @return true si valide (longueur ≤ max)
     */
    public static boolean forcerLongueurMax(TextField field, int max, Window owner) {
        if (field == null) return true;

        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > max) {
                field.setText(newVal.substring(0, max));
                field.positionCaret(max);
                if (owner != null) {
                    AlertUtil.afficherInformation(
                            "Longueur maximale",
                            "Maximum " + max + " caractères autorisés.");
                }
            }
        });
        return true;
    }

    /**
     * Vérifie qu'un champ a une longueur minimale.
     * @return true si valide (longueur ≥ min)
     */
    public static boolean verifierLongueurMin(TextField field, int min,
                                              String nomChamp, Window owner) {
        if (field == null) return false;
        String value = field.getText();

        if (value == null || value.trim().length() < min) {
            if (owner != null) {
                AlertUtil.afficherAvertissement(
                        "Champ invalide",
                        "Le champ \"" + nomChamp + "\" doit contenir au moins "
                                + min + " caractère(s).");
            }
            field.requestFocus();
            return false;
        }
        return true;
    }

    // ==========================================
    // 4. VALIDATION ENTIER / DÉCIMAL
    // ==========================================

    /**
     * Vérifie que le champ contient un entier valide.
     * @return true si valide
     */
    public static boolean verifierEntier(TextField field, String nomChamp, Window owner) {
        return verifierEntier(field, nomChamp, owner, false);
    }

    /** Vérifie un entier avec option de plage. */
    public static boolean verifierEntier(TextField field, String nomChamp, Window owner,
                                         boolean obligatoire) {
        if (field == null) return false;
        String value = field.getText();

        if (value == null || value.isBlank()) {
            if (obligatoire && owner != null) {
                AlertUtil.afficherAvertissement("Champ requis",
                        "Le champ \"" + nomChamp + "\" est obligatoire.");
                field.requestFocus();
            }
            return !obligatoire;
        }

        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            if (owner != null) {
                AlertUtil.afficherErreur("Format invalide",
                        "Le champ \"" + nomChamp + "\" doit contenir un entier valide.");
            }
            field.requestFocus();
            return false;
        }
    }

    /**
     * Vérifie que le champ contient un entier dans une plage [min, max].
     */
    public static boolean verifierEntierPlage(TextField field, String nomChamp,
                                              int min, int max, Window owner) {
        if (!verifierEntier(field, nomChamp, owner, true)) return false;

        try {
            int valeur = Integer.parseInt(field.getText().trim());
            if (valeur < min || valeur > max) {
                if (owner != null) {
                    AlertUtil.afficherAvertissement("Valeur hors plage",
                            "Le champ \"" + nomChamp + "\" doit être entre "
                                    + min + " et " + max + ".");
                }
                field.requestFocus();
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Vérifie que le champ contient un décimal valide. */
    public static boolean verifierDecimal(TextField field, String nomChamp,
                                          Window owner, boolean obligatoire) {
        if (field == null) return false;
        String value = field.getText();

        if (value == null || value.isBlank()) {
            if (obligatoire && owner != null) {
                AlertUtil.afficherAvertissement("Champ requis",
                        "Le champ \"" + nomChamp + "\" est obligatoire.");
                field.requestFocus();
            }
            return !obligatoire;
        }

        try {
            Double.parseDouble(value.trim().replace(",", "."));
            return true;
        } catch (NumberFormatException e) {
            if (owner != null) {
                AlertUtil.afficherErreur("Format invalide",
                        "Le champ \"" + nomChamp + "\" doit contenir un nombre décimal valide.");
            }
            field.requestFocus();
            return false;
        }
    }

    // ==========================================
    // 5. VALIDATION EMAIL / TÉLÉPHONE
    // ==========================================

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern TELEPHONE_PATTERN =
            Pattern.compile("^[+]?[0-9\\s.-]{6,20}$");

    /** Vérifie qu'un champ contient un email valide. */
    public static boolean verifierEmail(TextField field, Window owner, boolean obligatoire) {
        if (field == null) return false;
        String value = field.getText();

        if (value == null || value.isBlank()) {
            if (obligatoire && owner != null) {
                AlertUtil.afficherAvertissement("Champ requis",
                        "L'email est obligatoire.");
                field.requestFocus();
            }
            return !obligatoire;
        }

        if (!EMAIL_PATTERN.matcher(value.trim()).matches()) {
            if (owner != null) {
                AlertUtil.afficherErreur("Email invalide",
                        "Veuillez saisir une adresse email valide (ex : nom@domaine.com).");
            }
            field.requestFocus();
            return false;
        }
        return true;
    }

    /** Vérifie qu'un champ contient un numéro de téléphone valide. */
    public static boolean verifierTelephone(TextField field, Window owner, boolean obligatoire) {
        if (field == null) return false;
        String value = field.getText();

        if (value == null || value.isBlank()) {
            if (obligatoire && owner != null) {
                AlertUtil.afficherAvertissement("Champ requis",
                        "Le téléphone est obligatoire.");
                field.requestFocus();
            }
            return !obligatoire;
        }

        if (!TELEPHONE_PATTERN.matcher(value.trim()).matches()) {
            if (owner != null) {
                AlertUtil.afficherErreur("Téléphone invalide",
                        "Veuillez saisir un numéro de téléphone valide.");
            }
            field.requestFocus();
            return false;
        }
        return true;
    }

    // ==========================================
    // 6. VALIDATION DATE
    // ==========================================

    /**
     * Vérifie qu'une date n'est PAS dans le passé.
     */
    public static boolean verifierDateFuture(DatePicker picker, String nomChamp, Window owner) {
        if (picker == null || picker.getValue() == null) {
            if (owner != null) {
                AlertUtil.afficherAvertissement("Champ requis",
                        "Le champ \"" + nomChamp + "\" est obligatoire.");
                picker.requestFocus();
            }
            return false;
        }

        if (picker.getValue().isBefore(LocalDate.now())) {
            if (owner != null) {
                AlertUtil.afficherErreur("Date invalide",
                        "Le champ \"" + nomChamp + "\" ne peut pas être dans le passé.");
            }
            picker.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Vérifie qu'une date n'est PAS dans le futur.
     */
    public static boolean verifierDatePassee(DatePicker picker, String nomChamp, Window owner) {
        if (picker == null || picker.getValue() == null) {
            if (owner != null) {
                AlertUtil.afficherAvertissement("Champ requis",
                        "Le champ \"" + nomChamp + "\" est obligatoire.");
                picker.requestFocus();
            }
            return false;
        }

        if (picker.getValue().isAfter(LocalDate.now())) {
            if (owner != null) {
                AlertUtil.afficherErreur("Date invalide",
                        "Le champ \"" + nomChamp + "\" ne peut pas être dans le futur.");
            }
            picker.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Vérifie qu'une date est comprise dans une plage [min, max].
     */
    public static boolean verifierDatePlage(DatePicker picker, String nomChamp,
                                            LocalDate min, LocalDate max, Window owner) {
        if (picker == null || picker.getValue() == null) {
            if (owner != null) {
                AlertUtil.afficherAvertissement("Champ requis",
                        "Le champ \"" + nomChamp + "\" est obligatoire.");
                picker.requestFocus();
            }
            return false;
        }

        LocalDate date = picker.getValue();
        if (date.isBefore(min) || date.isAfter(max)) {
            if (owner != null) {
                AlertUtil.afficherAvertissement("Date hors plage",
                        "Le champ \"" + nomChamp + "\" doit être entre "
                                + min + " et " + max + ".");
            }
            picker.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Force une DatePicker à désactiver les dates passées.
     * (Empêche la sélection, sans alerte)
     */
    public static void forcerDateFuture(DatePicker picker) {
        if (picker == null) return;
        picker.setDayCellFactory(dp -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) return;
                setDisable(item.isBefore(LocalDate.now()));
            }
        });
    }

    /**
     * Force une DatePicker à désactiver les dates futures.
     */
    public static void forcerDatePassee(DatePicker picker) {
        if (picker == null) return;
        picker.setDayCellFactory(dp -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) return;
                setDisable(item.isAfter(LocalDate.now()));
            }
        });
    }

    /**
     * Force une DatePicker à désactiver les dates hors plage.
     */
    public static void forcerDatePlage(DatePicker picker, LocalDate min, LocalDate max) {
        if (picker == null) return;
        picker.setDayCellFactory(dp -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) return;
                setDisable(item.isBefore(min) || item.isAfter(max));
            }
        });
    }

    // ==========================================
    // 7. VALIDATION GÉNÉRIQUE
    // ==========================================

    /** Vérifie qu'un champ n'est pas vide. */
    public static boolean verifierNonVide(TextField field, String nomChamp, Window owner) {
        if (field == null || field.getText() == null || field.getText().isBlank()) {
            if (owner != null) {
                AlertUtil.afficherAvertissement("Champ requis",
                        "Le champ \"" + nomChamp + "\" est obligatoire.");
            }
            if (field != null) field.requestFocus();
            return false;
        }
        return true;
    }

    /** Vérifie que le champ correspond à une regex. */
    public static boolean verifierRegex(TextField field, String regex,
                                        String nomChamp, String messageErreur, Window owner) {
        if (field == null || field.getText() == null) return false;

        if (!field.getText().trim().matches(regex)) {
            if (owner != null) {
                AlertUtil.afficherErreur("Format invalide", messageErreur);
            }
            field.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Vérifie plusieurs champs obligatoires d'un coup.
     * Retourne le premier champ invalide ou null si tout est OK.
     */
    public static TextField verifierChampsObligatoires(Window owner,
                                                       TextInputControl... champs) {
        for (TextInputControl champ : champs) {
            if (champ == null || champ.getText() == null || champ.getText().isBlank()) {
                if (owner != null) {
                    AlertUtil.afficherAvertissement("Champ requis",
                            "Tous les champs obligatoires doivent être remplis.");
                }
                if (champ != null) champ.requestFocus();
                return (TextField) (champ instanceof TextField ? champ : null);
            }
        }
        return null;
    }

    // ==========================================
    // 8. NETTOYAGE
    // ==========================================

    /** Trim (supprime les espaces autour) un champ au blur. */
    public static void trimAuBlur(TextField field) {
        if (field == null) return;
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && field.getText() != null) {
                field.setText(field.getText().trim());
            }
        });
    }

    /** Supprime les espaces internes en trop. */
    public static void normaliserEspaces(TextField field) {
        if (field == null) return;
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.contains("  ")) {
                field.setText(newVal.replaceAll("\\s+", " "));
                field.positionCaret(field.getText().length());
            }
        });
    }

    /** Vide un champ. */
    public static void vider(TextField field) {
        if (field != null) field.clear();
    }

    // ==========================================
    // 9. VALIDATION MOT DE PASSE
    // ==========================================

    /**
     * Vérifie la force d'un mot de passe et retourne un score [0, 1].
     */
    public static double forceMotDePasse(String password) {
        if (password == null || password.isEmpty()) return 0;

        double score = 0;
        if (password.length() >= 8) score += 0.3;
        if (password.length() >= 12) score += 0.1;
        if (password.matches(".*[A-Z].*")) score += 0.15;
        if (password.matches(".*[a-z].*")) score += 0.15;
        if (password.matches(".*[0-9].*")) score += 0.15;
        if (password.matches(".*[@#$%^&+=!_\\-].*")) score += 0.15;

        return Math.min(score, 1.0);
    }

    /**
     * Vérifie qu'un mot de passe et sa confirmation correspondent.
     */
    public static boolean verifierCorrespondanceMdp(TextField mdp1, TextField mdp2,
                                                    Window owner) {
        if (mdp1 == null || mdp2 == null) return false;

        String p1 = mdp1.getText();
        String p2 = mdp2.getText();

        if (p1 == null || p2 == null || !p1.equals(p2)) {
            if (owner != null) {
                AlertUtil.afficherErreur("Mots de passe différents",
                        "Les deux mots de passe ne correspondent pas.");
            }
            mdp2.requestFocus();
            return false;
        }
        return true;
    }

    /** Retourne un libellé pour la force d'un mot de passe. */
    public static String libelleForceMotDePasse(double score) {
        if (score < 0.4) return "Faible";
        if (score < 0.75) return "Moyen";
        return "Fort";
    }

    /** Retourne la couleur CSS correspondant à la force d'un mot de passe. */
    public static String couleurForceMotDePasse(double score) {
        if (score < 0.4) return "#E5484D";   // rouge
        if (score < 0.75) return "#F5A524";  // orange
        return "#16A34A";                     // vert
    }

    public static void configurerToggleSwitch(ToggleButton toggle) {
        Circle cercle = new Circle(11);
        cercle.getStyleClass().add("switch-bille");
        toggle.setGraphic(cercle);
        toggle.getStyleClass().add("switch-button");

        // Animation
        toggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(180), cercle);
            tt.setToX(newVal ? 1 : 0);
            tt.setInterpolator(Interpolator.SPLINE(0.33, 0, 0.1, 1));
            tt.play();
        });
    }
}