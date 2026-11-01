package org.erpklassup.erpklassup.util;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registre de vues avec mise en cache, gestion d'état et animation
 * de transition fluide (style Windows 11 Paramètres).
 *
 * <p>L'animation reproduit fidèlement le comportement de l'application
 * Paramètres de Windows 11 :
 * <ul>
 *   <li>La nouvelle page entre par le bas (+24px → 0)</li>
 *   <li>L'ancienne page sort par le haut (0 → -16px)</li>
 *   <li>Fade progressif qui démarre à 50% de la durée</li>
 *   <li>Courbe d'easing "fast in, slow out" (CubicBezier)</li>
 *   <li>Durée totale ~320 ms</li>
 * </ul>
 * </p>
 */
public class ViewRegistry {

    /* ============================================================
       CONFIGURATION DE L'ANIMATION — Style Windows 11 Paramètres
       ============================================================ */

    /** Durée totale de la transition. Windows ≈ 320ms. */
    private static final Duration DUREE_ANIMATION = Duration.millis(320);

    /** Distance verticale (px) que parcourt la nouvelle page. */
    private static final double DECALAGE_ENTREE = 28.0;

    /** Distance verticale (px) que parcourt l'ancienne page. */
    private static final double DECALAGE_SORTIE = 16.0;

    /**
     * Easing "Fast in, Slow out" — reproduit la courbe Windows.
     * Wolfram : cubic-bezier(0.33, 0, 0.1, 1).
     * Accélération rapide au début, décélération douce à la fin.
     */
    private static final Interpolator EASING_ENTREE = Interpolator.SPLINE(0.33, 0.0, 0.1, 1.0);

    /**
     * Easing pour l'ancienne page qui sort — plus rapide et plus droit.
     */
    private static final Interpolator EASING_SORTIE = Interpolator.SPLINE(0.4, 0.0, 0.6, 1.0);

    /* ============================================================
       ÉTAT
       ============================================================ */
    private final StackPane conteneur;
    private final int capaciteMax;

    private String cheminVueCourante = null;
    private Parent vueCouranteParent = null;

    /** Flag anti-réentrance : empêche 2 animations simultanées. */
    private boolean animationEnCours = false;

    private final Map<String, Parent> vueCache;
    private final Map<String, Object> controllerCache = new HashMap<>();
    private final Map<String, Object> stateCache = new HashMap<>();

    /* ============================================================
       CONSTRUCTEUR
       ============================================================ */
    public ViewRegistry(StackPane conteneur, int capaciteMax) {
        this.conteneur = conteneur;
        this.capaciteMax = capaciteMax;

        this.vueCache = new LinkedHashMap<>(capaciteMax, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Parent> eldest) {
                if (size() > capaciteMax) {
                    purgerVue(eldest.getKey(), eldest.getValue());
                    return true;
                }
                return false;
            }
        };
    }

    /* ============================================================
       AFFICHAGE D'UNE VUE
       ============================================================ */
    public void afficherVue(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isBlank()) return;

        // 1. Si on demande la même vue, ne rien faire
        if (fxmlPath.equals(cheminVueCourante) && vueCouranteParent != null) return;

        // 2. Anti-réentrance
        if (animationEnCours) return;

        // 3. Sauvegarder l'état de la vue sortante
        sauvegarderEtatCourant();

        Parent nouvelleVue;
        try {
            // 4. Récupérer depuis le cache OU charger
            if (vueCache.containsKey(fxmlPath)) {
                nouvelleVue = vueCache.get(fxmlPath);
            } else {
                FXMLLoader loader = I18nManager.getInstance().creerLoader(getClass(), fxmlPath);
                nouvelleVue = loader.load();

                Object controller = loader.getController();
                if (controller != null) {
                    controllerCache.put(fxmlPath, controller);
                }
                vueCache.put(fxmlPath, nouvelleVue);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur de chargement du FXML : " + fxmlPath);
            e.printStackTrace();
            afficherErreur(fxmlPath, e);
            return;
        }

        // 5. Restaurer l'état si disponible
        Object controller = controllerCache.get(fxmlPath);
        Object etatSauvegarde = stateCache.get(fxmlPath);
        if (controller instanceof StatefulController stateful && etatSauvegarde != null) {
            stateful.restaurerEtat(etatSauvegarde);
        }

        // 6. Lancer l'animation
        Parent ancienneVue = vueCouranteParent;

        Object crtl = controllerCache.get(fxmlPath);
        if (crtl instanceof BreadcrumbAware bc) {
            bc.definirBreadcrumb();
        }
        animerTransition(ancienneVue, nouvelleVue, fxmlPath);
    }

    /* ============================================================
       ANIMATION DE TRANSITION — Style Windows 11 Paramètres
       ============================================================ */
    private void animerTransition(Parent ancienneVue, Parent nouvelleVue, String fxmlPath) {
        animationEnCours = true;

        /* ---------------------------------------------------------
           1. PRÉPARATION DE LA NOUVELLE VUE
           --------------------------------------------------------- */
        nouvelleVue.setOpacity(0.0);
        nouvelleVue.setTranslateY(DECALAGE_ENTREE);

        // Optimisation : activer le cache pendant l'animation pour
        // éviter les saccades de rendu sur les vues complexes.
        nouvelleVue.setCache(true);
        nouvelleVue.setCacheHint(CacheHint.SPEED);

        if (!conteneur.getChildren().contains(nouvelleVue)) {
            conteneur.getChildren().add(nouvelleVue);
        }
        nouvelleVue.toFront();

        /* ---------------------------------------------------------
           2. TIMELINE D'ENTRÉE — une seule, fluide
           --------------------------------------------------------- */
        Timeline entree = new Timeline(
                // Position Y : entre par le bas
                new KeyFrame(Duration.ZERO,
                        new KeyValue(nouvelleVue.translateYProperty(),
                                DECALAGE_ENTREE, EASING_ENTREE)
                ),
                new KeyFrame(DUREE_ANIMATION,
                        new KeyValue(nouvelleVue.translateYProperty(),
                                0, EASING_ENTREE)
                ),
                // Opacité progressive : démarre en douceur
                new KeyFrame(Duration.ZERO,
                        new KeyValue(nouvelleVue.opacityProperty(),
                                0.0, Interpolator.LINEAR)
                ),
                new KeyFrame(DUREE_ANIMATION.multiply(0.5),
                        new KeyValue(nouvelleVue.opacityProperty(),
                                0.6, Interpolator.LINEAR)
                ),
                new KeyFrame(DUREE_ANIMATION,
                        new KeyValue(nouvelleVue.opacityProperty(),
                                1.0, Interpolator.LINEAR)
                )
        );

        /* ---------------------------------------------------------
           3. TIMELINE DE SORTIE — pour l'ancienne vue
           --------------------------------------------------------- */
        if (ancienneVue != null && ancienneVue != nouvelleVue) {

            // Activer aussi le cache sur l'ancienne
            ancienneVue.setCache(true);
            ancienneVue.setCacheHint(CacheHint.SPEED);

            Timeline sortie = new Timeline(
                    // Position Y : sort vers le haut
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(ancienneVue.translateYProperty(),
                                    0, EASING_SORTIE)
                    ),
                    new KeyFrame(DUREE_ANIMATION,
                            new KeyValue(ancienneVue.translateYProperty(),
                                    -DECALAGE_SORTIE, EASING_SORTIE)
                    ),
                    // Opacité : fade rapide, terminé à 70%
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(ancienneVue.opacityProperty(),
                                    1.0, Interpolator.LINEAR)
                    ),
                    new KeyFrame(DUREE_ANIMATION.multiply(0.7),
                            new KeyValue(ancienneVue.opacityProperty(),
                                    0.0, Interpolator.LINEAR)
                    )
            );

            final Parent ancienneFinale = ancienneVue;
            sortie.setOnFinished(e -> {
                if (conteneur.getChildren().contains(ancienneFinale)) {
                    conteneur.getChildren().remove(ancienneFinale);
                }
                // Réinitialiser pour un futur affichage
                ancienneFinale.setOpacity(1.0);
                ancienneFinale.setTranslateY(0);
                ancienneFinale.setCache(false);
            });

            sortie.play();
        }

        /* ---------------------------------------------------------
           4. FIN DE L'ANIMATION D'ENTRÉE
           --------------------------------------------------------- */
        entree.setOnFinished(e -> {
            nouvelleVue.setOpacity(1.0);
            nouvelleVue.setTranslateY(0);
            nouvelleVue.setCache(false);
            animationEnCours = false;
        });

        entree.play();

        /* ---------------------------------------------------------
           5. MISE À JOUR DE L'ÉTAT
           --------------------------------------------------------- */
        cheminVueCourante = fxmlPath;
        vueCouranteParent = nouvelleVue;
    }

    /* ============================================================
       GESTION D'ERREUR
       ============================================================ */
    private void afficherErreur(String fxmlPath, Exception e) {
        Label erreur = new Label("Erreur de chargement : " + fxmlPath
                + "\n" + e.getMessage());
        erreur.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-wrap-text: true;");
        conteneur.getChildren().clear();
        conteneur.getChildren().add(erreur);
        cheminVueCourante = null;
        vueCouranteParent = null;
        animationEnCours = false;
    }

    /* ============================================================
       SAUVEGARDE D'ÉTAT
       ============================================================ */
    @SuppressWarnings("unchecked")
    private void sauvegarderEtatCourant() {
        if (cheminVueCourante == null) return;

        Object controller = controllerCache.get(cheminVueCourante);
        if (controller instanceof StatefulController stateful) {
            stateCache.put(cheminVueCourante, stateful.sauvegarderEtat());
        }
    }

    /* ============================================================
       PURGE / RESET
       ============================================================ */
    private void purgerVue(String fxmlPath, Parent node) {
        Object controller = controllerCache.remove(fxmlPath);
        if (controller instanceof VueDisposable disposable) {
            disposable.disposer();
        }
    }

    /**
     * Vide complètement la mémoire et le conteneur visuel.
     * À appeler lors d'un logout.
     */
    public void toutReinitialiser() {
        // 1. Libérer les ressources des contrôleurs
        controllerCache.values().forEach(c -> {
            if (c instanceof VueDisposable d) d.disposer();
        });

        // 2. Vider le conteneur visuel
        if (conteneur != null) {
            conteneur.getChildren().clear();
        }

        // 3. Vider tous les caches
        vueCache.clear();
        controllerCache.clear();
        stateCache.clear();
        cheminVueCourante = null;
        vueCouranteParent = null;
        animationEnCours = false;
    }

    /**
     * Force le rechargement d'une vue (utile pour i18n).
     */
    public void invaliderCache(String fxmlPath) {
        vueCache.remove(fxmlPath);
        controllerCache.remove(fxmlPath);
        stateCache.remove(fxmlPath);
    }

    /**
     * Force le rechargement de la vue courante (i18n).
     */
    public void rechargerVueCourante() {
        if (cheminVueCourante == null) return;
        String chemin = cheminVueCourante;
        invaliderCache(chemin);
        cheminVueCourante = null;
        vueCouranteParent = null;
        afficherVue(chemin);
    }

    /* ============================================================
       ACCESSEURS
       ============================================================ */
    public String getCheminVueCourante() {
        return cheminVueCourante;
    }
}
//package org.erpklassup.erpklassup.util;
//
//import javafx.animation.FadeTransition;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.control.Label;
//import javafx.scene.layout.StackPane;
//import javafx.util.Duration;
//
//import java.io.IOException;
//import java.net.URL;
//import java.util.HashMap;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//public class ViewRegistry {
//
//    private final StackPane conteneur;
//    private final int capaciteMax;
//
//    private String cheminVueCourante = null;
//
//    private final Map<String, Parent> vueCache;
//    private final Map<String, Object> controllerCache = new HashMap<>();
//    private final Map<String, Object> stateCache = new HashMap<>();
//
//    public ViewRegistry(StackPane conteneur, int capaciteMax) {
//        this.conteneur = conteneur;
//        this.capaciteMax = capaciteMax;
//
//        this.vueCache = new LinkedHashMap<>(capaciteMax, 0.75f, true) {
//            @Override
//            protected boolean removeEldestEntry(Map.Entry<String, Parent> eldest) {
//                if (size() > capaciteMax) {
//                    purgerVue(eldest.getKey(), eldest.getValue());
//                    return true;
//                }
//                return false;
//            }
//        };
//    }
//
////    public void afficherVue(String fxmlPath) {
////        if (fxmlPath.equals(cheminVueCourante)) return;
////
////        sauvegarderEtatCourant();
////
////        try {
////            Parent vue;
////
////            if (vueCache.containsKey(fxmlPath)) {
////                vue = vueCache.get(fxmlPath);
////            } else {
////                URL ressource = getClass().getResource(fxmlPath);
////
////                // ✅ Vérification explicite : ressource introuvable
////                if (ressource == null) {
////                    throw new IOException("Ressource FXML introuvable : " + fxmlPath);
////                }
////
////                FXMLLoader loader = new FXMLLoader(ressource);
////                vue = loader.load();
////
////                Object controller = loader.getController();
////                if (controller != null) {
////                    controllerCache.put(fxmlPath, controller);
////                }
////                vueCache.put(fxmlPath, vue);
////            }
////
////            // Restaurer l'état si disponible
////            Object controller = controllerCache.get(fxmlPath);
////            Object etatSauvegarde = stateCache.get(fxmlPath);
////            if (controller instanceof StatefulController stateful && etatSauvegarde != null) {
////                stateful.restaurerEtat(etatSauvegarde);
////            }
////
////            // Injecter dans le conteneur avec animation
////            conteneur.getChildren().clear();
////
////            FadeTransition ft = new FadeTransition(Duration.millis(150), vue);
////            ft.setFromValue(0.0);
////            ft.setToValue(1.0);
////
////            conteneur.getChildren().add(vue);
////            ft.play();
////
////            cheminVueCourante = fxmlPath;
////
////        } catch (IOException e) {
////            System.err.println("❌ Erreur de chargement du FXML : " + fxmlPath);
////            e.printStackTrace();
////
////            // ✅ Fallback visuel : ne pas rester silencieux
////            Label erreur = new Label("Erreur de chargement : " + fxmlPath
////                    + "\n" + e.getMessage());
////            erreur.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-wrap-text: true;");
////            conteneur.getChildren().clear();
////            conteneur.getChildren().add(erreur);
////            cheminVueCourante = null;
////        }
////    }
//
//
//
//public void afficherVue(String fxmlPath) {
//    if (fxmlPath.equals(cheminVueCourante)) return;
//
//    sauvegarderEtatCourant();
//
//    try {
//        Parent vue;
//
//        if (vueCache.containsKey(fxmlPath)) {
//            vue = vueCache.get(fxmlPath);
//        } else {
//            // ✅ CHANGEMENT : création via I18nManager (bundle inclus)
//            FXMLLoader loader = I18nManager.getInstance().creerLoader(getClass(), fxmlPath);
//            vue = loader.load();
//
//            Object controller = loader.getController();
//            if (controller != null) {
//                controllerCache.put(fxmlPath, controller);
//            }
//            vueCache.put(fxmlPath, vue);
//        }
//
//        // Restaurer l'état si disponible
//        Object controller = controllerCache.get(fxmlPath);
//        Object etatSauvegarde = stateCache.get(fxmlPath);
//        if (controller instanceof StatefulController stateful && etatSauvegarde != null) {
//            stateful.restaurerEtat(etatSauvegarde);
//        }
//
//        // Injecter dans le conteneur avec animation
//        conteneur.getChildren().clear();
//
//        FadeTransition ft = new FadeTransition(Duration.millis(150), vue);
//        ft.setFromValue(0.0);
//        ft.setToValue(1.0);
//
//        conteneur.getChildren().add(vue);
//        ft.play();
//
//        cheminVueCourante = fxmlPath;
//
//    } catch (Exception e) {
//        System.err.println("❌ Erreur de chargement du FXML : " + fxmlPath);
//        e.printStackTrace();
//
//        Label erreur = new Label("Erreur de chargement : " + fxmlPath
//                + "\n" + e.getMessage());
//        erreur.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-wrap-text: true;");
//        conteneur.getChildren().clear();
//        conteneur.getChildren().add(erreur);
//        cheminVueCourante = null;
//    }
//}
//
//    @SuppressWarnings("unchecked")
//    private void sauvegarderEtatCourant() {
//        if (cheminVueCourante == null) return;
//
//        Object controller = controllerCache.get(cheminVueCourante);
//        if (controller instanceof StatefulController stateful) {
//            stateCache.put(cheminVueCourante, stateful.sauvegarderEtat());
//        }
//    }
//
//    private void purgerVue(String fxmlPath, Parent node) {
//        Object controller = controllerCache.remove(fxmlPath);
//        if (controller instanceof VueDisposable disposable) {
//            disposable.disposer();
//        }
//    }
//
//    /**
//     * ✅ CORRIGÉ : vide aussi le conteneur visuel
//     */
//    public void toutReinitialiser() {
//        // 1. Libérer les ressources des contrôleurs
//        controllerCache.values().forEach(c -> {
//            if (c instanceof VueDisposable d) d.disposer();
//        });
//
//        // 2. Vider le conteneur visuel (sinon l'ancienne vue reste affichée)
//        if (conteneur != null) {
//            conteneur.getChildren().clear();
//        }
//
//        // 3. Vider tous les caches
//        vueCache.clear();
//        controllerCache.clear();
//        stateCache.clear();
//        cheminVueCourante = null;
//    }
//
//    /**
//     * ✅ Force le rechargement d'une vue (utile pour i18n).
//     * Supprime la vue du cache pour qu'elle soit rechargée avec le nouveau bundle.
//     */
//    public void invaliderCache(String fxmlPath) {
//        vueCache.remove(fxmlPath);
//        controllerCache.remove(fxmlPath);
//        stateCache.remove(fxmlPath);
//    }
//
//    /**
//     * ✅ Force le rechargement de la vue courante (i18n).
//     */
//    public void rechargerVueCourante() {
//        if (cheminVueCourante == null) return;
//        String chemin = cheminVueCourante;
//        invaliderCache(chemin);
//        cheminVueCourante = null;   // Force le rechargement
//        afficherVue(chemin);
//    }
//
//    public String getCheminVueCourante() {
//        return cheminVueCourante;
//    }
//}
////package org.erpklassup.erpklassup.util;
////
////import javafx.animation.FadeTransition;
////import javafx.fxml.FXMLLoader;
////import javafx.scene.Parent;
////import javafx.scene.layout.StackPane;
////import javafx.util.Duration;
////
////import java.io.IOException;
////import java.util.HashMap;
////import java.util.LinkedHashMap;
////import java.util.Map;
////
////public class ViewRegistry {
////
////    private final StackPane conteneur;
////    private final int capaciteMax;
////
////    private String cheminVueCourante = null;
////
////    // Cache LRU des composants graphiques (Parent)
////    private final Map<String, Parent> vueCache;
////
////    // Cache des contrôleurs associés
////    private final Map<String, Object> controllerCache = new HashMap<>();
////
////    // Cache persistant des états (conservé même si la vue FXML est purgée de la RAM)
////    private final Map<String, Object> stateCache = new HashMap<>();
////
////    public ViewRegistry(StackPane conteneur, int capaciteMax) {
////        this.conteneur = conteneur;
////        this.capaciteMax = capaciteMax;
////
////        this.vueCache = new LinkedHashMap<>(capaciteMax, 0.75f, true) {
////            @Override
////            protected boolean removeEldestEntry(Map.Entry<String, Parent> eldest) {
////                if (size() > capaciteMax) {
////                    purgerVue(eldest.getKey(), eldest.getValue());
////                    return true;
////                }
////                return false;
////            }
////        };
////    }
////
////    /**
////     * Charge et affiche une vue par son chemin FXML avec gestion automatique d'état et de mémoire.
////     */
////    public void afficherVue(String fxmlPath) {
////        if (fxmlPath.equals(cheminVueCourante)) return;
////
////        // 1. Sauvegarder l'état de la vue sortante
////        sauvegarderEtatCourant();
////
////        try {
////            Parent vue;
////
////            // 2. Récupérer depuis le cache ou charger le FXML
////            if (vueCache.containsKey(fxmlPath)) {
////                vue = vueCache.get(fxmlPath);
////            } else {
////                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
////                vue = loader.load();
////
////                Object controller = loader.getController();
////                if (controller != null) {
////                    controllerCache.put(fxmlPath, controller);
////                }
////                vueCache.put(fxmlPath, vue);
////            }
////
////            // 3. Restaurer l'état si disponible
////            Object controller = controllerCache.get(fxmlPath);
////            Object etatSauvegarde = stateCache.get(fxmlPath);
////            if (controller instanceof StatefulController stateful && etatSauvegarde != null) {
////                stateful.restaurerEtat(etatSauvegarde);
////            }
////
////            // 4. Injecter dans le conteneur avec animation
////            conteneur.getChildren().clear();
////
////            FadeTransition ft = new FadeTransition(Duration.millis(150), vue);
////            ft.setFromValue(0.0);
////            ft.setToValue(1.0);
////
////            conteneur.getChildren().add(vue);
////            ft.play();
////
////            cheminVueCourante = fxmlPath;
////
////        } catch (IOException e) {
////            System.err.println("Erreur de chargement du FXML : " + fxmlPath);
////            e.printStackTrace();
////        }
////    }
////
////    @SuppressWarnings("unchecked")
////    private void sauvegarderEtatCourant() {
////        if (cheminVueCourante == null) return;
////
////        Object controller = controllerCache.get(cheminVueCourante);
////        if (controller instanceof StatefulController stateful) {
////            stateCache.put(cheminVueCourante, stateful.sauvegarderEtat());
////        }
////    }
////
////    private void purgerVue(String fxmlPath, Parent node) {
////        Object controller = controllerCache.remove(fxmlPath);
////        if (controller instanceof VueDisposable disposable) {
////            disposable.disposer();
////        }
////    }
////
////    /**
////     * Vide complètement la mémoire (utile lors d'un logout)
////     */
////    public void toutReinitialiser() {
////        // 1. Libérer les ressources des contrôleurs
////        controllerCache.values().forEach(c -> {
////            if (c instanceof VueDisposable d) d.disposer();
////        });
////
////        // 2. ✅ Vider le conteneur visuel
////        if (conteneur != null) {
////            conteneur.getChildren().clear();
////        }
////
////        // 3. Vider tous les caches
////        vueCache.clear();
////        controllerCache.clear();
////        stateCache.clear();
////        cheminVueCourante = null;
////    }
////}