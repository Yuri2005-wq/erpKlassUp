package org.erpklassup.erpklassup.util;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Gestionnaire centralisé du fil d'Ariane affiché dans l'AppBar.
 *
 * <p>Trois niveaux :
 * <ul>
 *   <li><b>parent</b> : libellé du bouton principal du menu (ex: "Paramètres")</li>
 *   <li><b>section</b> : libellé du bouton principal de la sous-nav (ex: "Configuration Pédagogique")</li>
 *   <li><b>sousSection</b> : onglet actif dans la page (ex: "Salles")</li>
 * </ul>
 * </p>
 */
public class BreadcrumbManager {

    private static final BreadcrumbManager INSTANCE = new BreadcrumbManager();

    /** Niveau 1 : groupe racine (ex: "Paramètres") */
    private final StringProperty parent = new SimpleStringProperty("");

    /** Niveau 2 : section active (ex: "Configuration Pédagogique") */
    private final StringProperty section = new SimpleStringProperty("");

    /** Niveau 3 : sous-section active (ex: "Salles") */
    private final StringProperty sousSection = new SimpleStringProperty("");

    private BreadcrumbManager() {}

    public static BreadcrumbManager getInstance() {
        return INSTANCE;
    }

    /**
     * À appeler quand on change de section principale (clic sidebar).
     * Réinitialise automatiquement la sous-section.
     *
     * @param parent      Libellé du groupe (ex: "Paramètres")
     * @param section     Libellé de la section (ex: "Configuration Pédagogique")
     */
    public void definirSection(String parent, String section) {
        this.parent.set(parent == null ? "" : parent);
        this.section.set(section == null ? "" : section);
        this.sousSection.set("");
    }

    /**
     * Variante simple : ne change que la section, parent inchangé.
     */
    public void definirSection(String section) {
        this.section.set(section == null ? "" : section);
        this.sousSection.set("");
    }

    /**
     * À appeler quand on change d'onglet à l'intérieur d'une section.
     */
    public void definirSousSection(String sousSection) {
        this.sousSection.set(sousSection == null ? "" : sousSection);
    }

    /* ============================================================
       PROPRIÉTÉS (pour binding JavaFX)
       ============================================================ */
    public StringProperty parentProperty()      { return parent; }
    public StringProperty sectionProperty()     { return section; }
    public StringProperty sousSectionProperty() { return sousSection; }

    /* ============================================================
       GETTERS (accès direct)
       ============================================================ */
    public String getParent()      { return parent.get(); }
    public String getSection()     { return section.get(); }
    public String getSousSection() { return sousSection.get(); }
}