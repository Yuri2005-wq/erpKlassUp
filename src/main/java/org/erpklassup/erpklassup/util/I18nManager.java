package org.erpklassup.erpklassup.util;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;

import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gestionnaire centralisé de l'internationalisation (i18n).
 *
 * Fonctionnalités :
 *   - Traduction via ResourceBundle (messages.properties / _fr / _en)
 *   - Changement de langue en temps réel (liste d'écouteurs)
 *   - Création de FXMLLoader avec bundle (résolution automatique des %clés)
 *   - Notification automatique des vues abonnées
 *
 * Thread-safe : utilise CopyOnWriteArrayList et Platform.runLater.
 */
public final class I18nManager {

    // ==========================================
    // CONSTANTES
    // ==========================================

    /** Chemin du bundle i18n (sans le suffixe de locale). */
    private static final String BUNDLE_NAME = "org.erpklassup.erpklassup.i18n.messages";

    /** Langue par défaut. */
    private static final Locale LOCALE_DEFAUT = Locale.FRENCH;

    // ==========================================
    // SINGLETON
    // ==========================================

    private static final I18nManager INSTANCE = new I18nManager();

    private I18nManager() {
        // Charge le bundle initial
        this.bundle = chargerBundle(LOCALE_DEFAUT);
        this.locale.set(LOCALE_DEFAUT);

        // Notifie les écouteurs à chaque changement de locale
        this.locale.addListener((obs, ancienne, nouvelle) -> {
            if (nouvelle == null) return;
            this.bundle = chargerBundle(nouvelle);
            notifierEcouteurs();
        });
    }

    public static I18nManager getInstance() {
        return INSTANCE;
    }

    // ==========================================
    // ÉTAT
    // ==========================================

    /** Locale courante (observable). */
    private final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(LOCALE_DEFAUT);

    /** Bundle courant (rechargé à chaque changement de locale). */
    private ResourceBundle bundle;

    /** Écouteurs notifiés lors d'un changement de langue. */
    private final List<Runnable> ecouteurs = new CopyOnWriteArrayList<>();

    // ==========================================
    // GETTERS / SETTERS
    // ==========================================

    public Locale getLocale() {
        return locale.get();
    }

    public ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    /**
     * Change la langue courante.
     * Déclenche automatiquement la notification des écouteurs.
     */
    public void setLocale(Locale nouvelleLocale) {
        if (nouvelleLocale == null) return;
        if (nouvelleLocale.equals(locale.get())) return;   // Pas de changement inutile
        locale.set(nouvelleLocale);
    }

    /**
     * Bascule entre FR et EN.
     */
    public void toggleLangue() {
        setLocale(getLocale().equals(Locale.FRENCH) ? Locale.ENGLISH : Locale.FRENCH);
    }

    // ==========================================
    // TRADUCTION
    // ==========================================

    /**
     * Traduit une clé.
     * Retourne {@code !clé!} si la clé est introuvable (facilite le debug).
     */
    public String t(String cle) {
        if (cle == null || cle.isBlank()) return "";
        try {
            return bundle.getString(cle);
        } catch (MissingResourceException e) {
            System.err.println("⚠️ [i18n] Clé manquante : " + cle);
            return "!" + cle + "!";
        }
    }

    /**
     * Traduit une clé avec paramètres.
     * Exemple : {@code t("message.bienvenue", "Lucas")} pour "Bienvenue {0}".
     */
    public String t(String cle, Object... parametres) {
        if (cle == null || cle.isBlank()) return "";
        try {
            String template = bundle.getString(cle);
            return MessageFormat.format(template, parametres);
        } catch (MissingResourceException e) {
            System.err.println("⚠️ [i18n] Clé manquante : " + cle);
            return "!" + cle + "!";
        }
    }

    // ==========================================
    // ÉCOUTEURS
    // ==========================================

    /**
     * Enregistre un écouteur appelé à chaque changement de langue.
     * L'écouteur est exécuté sur le thread JavaFX (via Platform.runLater).
     */
    public void ecouterChangement(Runnable ecouteur) {
        if (ecouteur != null && !ecouteurs.contains(ecouteur)) {
            ecouteurs.add(ecouteur);
        }
    }

    /**
     * Retire un écouteur précédemment enregistré.
     */
    public void arreterEcoute(Runnable ecouteur) {
        ecouteurs.remove(ecouteur);
    }

    private void notifierEcouteurs() {
        for (Runnable ecouteur : ecouteurs) {
            Platform.runLater(() -> {
                try {
                    ecouteur.run();
                } catch (Exception e) {
                    System.err.println("❌ Erreur écouteur i18n : " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    // ==========================================
    // CHARGEMENT FXML
    // ==========================================

    /**
     * Crée un {@link FXMLLoader} avec le bundle i18n.
     * Résout automatiquement les {@code %clés} du FXML.
     *
     * @param contexte   Classe de contexte (souvent {@code getClass()})
     * @param cheminFxml Chemin absolu du FXML (ex: "/org/.../view/login.fxml")
     */
    public FXMLLoader creerLoader(Class<?> contexte, String cheminFxml) {
        if (contexte == null || cheminFxml == null || cheminFxml.isBlank()) {
            throw new IllegalArgumentException("Contexte et cheminFxml sont obligatoires");
        }

        URL url = contexte.getResource(cheminFxml);
        if (url == null) {
            throw new IllegalStateException("FXML introuvable : " + cheminFxml
                    + " (contexte : " + contexte.getName() + ")");
        }

        FXMLLoader loader = new FXMLLoader(url);
        loader.setResources(bundle);   // ✅ Résout les %clés du FXML
        return loader;
    }

    // ==========================================
    // HELPERS INTERNES
    // ==========================================

    /**
     * Charge le bundle pour une locale donnée.
     * Retourne un bundle vide si introuvable (évite un crash).
     */
    private ResourceBundle chargerBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle(BUNDLE_NAME, locale);
        } catch (MissingResourceException e) {
            System.err.println("❌ [i18n] Bundle introuvable pour " + locale
                    + " — utilisation du bundle par défaut");
            return ResourceBundle.getBundle(BUNDLE_NAME, LOCALE_DEFAUT);
        }
    }

    /**
     * Applique la langue de l'utilisateur.
     * - Si le code est null ou vide → garde la langue par défaut (FR)
     * - Sinon → applique la langue correspondante
     */
    public void appliquerLangueUtilisateur(String codeLangue) {
        if (codeLangue == null || codeLangue.isBlank()) {
            System.out.println("🌍 Aucune langue préférée → langue par défaut conservée");
            return;
        }

        Locale locale = switch (codeLangue.trim().toUpperCase()) {
            case "EN" -> Locale.ENGLISH;
            case "FR" -> Locale.FRENCH;
            default -> null;
        };

        if (locale == null) {
            System.out.println("🌍 Code langue inconnu : " + codeLangue + " → langue par défaut");
            return;
        }

        setLocale(locale);
        System.out.println("🌍 Langue appliquée : " + locale);
    }
}