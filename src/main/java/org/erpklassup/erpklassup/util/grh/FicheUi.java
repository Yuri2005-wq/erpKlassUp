package org.erpklassup.erpklassup.util.grh;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;

/** Formatage et fabriques de cellules partagés par les onglets de la fiche employé. */
final class FicheUi {

    static final String VIDE = "-";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE);

    private static final Map<String, String> LIBELLES = Map.ofEntries(
            Map.entry("M", "Masculin"),
            Map.entry("F", "Féminin"),
            Map.entry("CELIBATAIRE", "Célibataire"),
            Map.entry("MARIE", "Marié(e)"),
            Map.entry("DIVORCE", "Divorcé(e)"),
            Map.entry("VEUF", "Veuf(ve)"),
            Map.entry("EN_CONGE", "En congé"),
            Map.entry("VALIDE", "Validé"),
            Map.entry("REJETE", "Rejeté"),
            Map.entry("ANNULE", "Annulé"),
            Map.entry("CLOTURE", "Clôturé"),
            Map.entry("PAYE", "Payé"),
            Map.entry("CALCULE", "Calculé"),
            Map.entry("EN_COURS", "En cours"),
            Map.entry("TERMINE", "Terminé"),
            Map.entry("SOUMIS", "Soumis"));

    private FicheUi() { }

    // ---------- Formatage ----------

    static String date(LocalDate d) {
        return d == null ? VIDE : DATE.format(d);
    }

    static String mois(YearMonth m) {
        if (m == null) {
            return VIDE;
        }
        String s = MOIS.format(m);
        return s.substring(0, 1).toUpperCase(Locale.FRANCE) + s.substring(1);
    }

    static String montant(BigDecimal m) {
        if (m == null) {
            return VIDE;
        }
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.FRANCE);
        return nf.format(m).replace('\u202F', '\u00A0') + "\u00A0FCFA";
    }

    static String jours(double j) {
        return j == Math.rint(j) ? Long.toString((long) j) : String.format(Locale.FRANCE, "%.1f", j);
    }

    static String anciennete(LocalDate embauche) {
        if (embauche == null || embauche.isAfter(LocalDate.now())) {
            return VIDE;
        }
        Period p = Period.between(embauche, LocalDate.now());
        int ans = p.getYears();
        int mois = p.getMonths();
        if (ans > 0) {
            String a = ans + (ans > 1 ? " ans" : " an");
            return mois > 0 ? a + " " + mois + " mois" : a;
        }
        return mois > 0 ? mois + " mois" : "< 1 mois";
    }

    /** Traduit un code (EN_CONGE) en libellé (« En congé »). Les libellés déjà lisibles passent tels quels. */
    static String libelle(String code) {
        if (code == null || code.isBlank()) {
            return VIDE;
        }
        String s = code.trim();
        String connu = LIBELLES.get(s.toUpperCase(Locale.ROOT));
        if (connu != null) {
            return connu;
        }
        if (s.length() <= 3) {
            return s;
        }
        s = s.replace('_', ' ');
        if (s.equals(s.toUpperCase(Locale.ROOT))) {
            s = s.toLowerCase(Locale.FRANCE);
        }
        return s.substring(0, 1).toUpperCase(Locale.FRANCE) + s.substring(1);
    }

    // ---------- Badges ----------

    static String variante(String code) {
        if (code == null) {
            return "neutre";
        }
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIF", "VALIDE", "PAYE", "EN_COURS" -> "succes";
            case "EN_CONGE", "SOUMIS", "CALCULE", "VACATAIRE" -> "alerte";
            case "SUSPENDU", "REJETE", "EXPIRE" -> "danger";
            case "CDI" -> "info";
            case "CDD" -> "violet";
            default -> "neutre";
        };
    }

    /** Applique le texte et la couleur d'un badge ; masque le label si le code est vide. */
    static void badge(Label label, String code) {
        boolean visible = code != null && !code.isBlank();
        label.setVisible(visible);
        label.setManaged(visible);
        label.getStyleClass().removeIf(c -> c.startsWith("badge"));
        if (!visible) {
            label.setText(null);
            return;
        }
        label.setText(libelle(code));
        label.getStyleClass().addAll("badge", "badge-" + variante(code));
    }

    // ---------- Colonnes de TableView ----------

    static <S> void colonneTexte(TableColumn<S, String> colonne, Function<S, String> valeur) {
        colonne.setCellValueFactory(d -> new ReadOnlyStringWrapper(valeur.apply(d.getValue())));
    }

    static <S> void colonneBadge(TableColumn<S, String> colonne, Function<S, String> code) {
        colonne.setCellValueFactory(d -> new ReadOnlyStringWrapper(code.apply(d.getValue())));
        colonne.setCellFactory(c -> new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                badge(badge, item);
                setGraphic(badge);
            }
        });
    }

    static <S> void colonneAction(TableColumn<S, Void> colonne, String icone, String infobulle,
                                  Consumer<S> action) {
        colonne.setCellFactory(c -> new TableCell<>() {
            private final Button bouton = new Button();

            {
                bouton.setGraphic(new FontIcon(icone));
                bouton.setTooltip(new Tooltip(infobulle));
                bouton.getStyleClass().add("btn-icone");
                bouton.setOnAction(e -> {
                    S ligne = getTableRow() == null ? null : getTableRow().getItem();
                    if (ligne != null) {
                        action.accept(ligne);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : bouton);
            }
        });
    }
}
