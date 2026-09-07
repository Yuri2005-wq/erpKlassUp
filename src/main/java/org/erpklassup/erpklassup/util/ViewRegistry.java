package org.erpklassup.erpklassup.util;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ViewRegistry {

    private final StackPane conteneur;
    private final int capaciteMax;

    private String cheminVueCourante = null;

    // Cache LRU des composants graphiques (Parent)
    private final Map<String, Parent> vueCache;

    // Cache des contrôleurs associés
    private final Map<String, Object> controllerCache = new HashMap<>();

    // Cache persistant des états (conservé même si la vue FXML est purgée de la RAM)
    private final Map<String, Object> stateCache = new HashMap<>();

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

    /**
     * Charge et affiche une vue par son chemin FXML avec gestion automatique d'état et de mémoire.
     */
    public void afficherVue(String fxmlPath) {
        if (fxmlPath.equals(cheminVueCourante)) return;

        // 1. Sauvegarder l'état de la vue sortante
        sauvegarderEtatCourant();

        try {
            Parent vue;

            // 2. Récupérer depuis le cache ou charger le FXML
            if (vueCache.containsKey(fxmlPath)) {
                vue = vueCache.get(fxmlPath);
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                vue = loader.load();

                Object controller = loader.getController();
                if (controller != null) {
                    controllerCache.put(fxmlPath, controller);
                }
                vueCache.put(fxmlPath, vue);
            }

            // 3. Restaurer l'état si disponible
            Object controller = controllerCache.get(fxmlPath);
            Object etatSauvegarde = stateCache.get(fxmlPath);
            if (controller instanceof StatefulController stateful && etatSauvegarde != null) {
                stateful.restaurerEtat(etatSauvegarde);
            }

            // 4. Injecter dans le conteneur avec animation
            conteneur.getChildren().clear();

            FadeTransition ft = new FadeTransition(Duration.millis(150), vue);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);

            conteneur.getChildren().add(vue);
            ft.play();

            cheminVueCourante = fxmlPath;

        } catch (IOException e) {
            System.err.println("Erreur de chargement du FXML : " + fxmlPath);
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void sauvegarderEtatCourant() {
        if (cheminVueCourante == null) return;

        Object controller = controllerCache.get(cheminVueCourante);
        if (controller instanceof StatefulController stateful) {
            stateCache.put(cheminVueCourante, stateful.sauvegarderEtat());
        }
    }

    private void purgerVue(String fxmlPath, Parent node) {
        Object controller = controllerCache.remove(fxmlPath);
        if (controller instanceof VueDisposable disposable) {
            disposable.disposer();
        }
    }

    /**
     * Vide complètement la mémoire (utile lors d'un logout)
     */
    public void toutRéinitialiser() {
        controllerCache.values().forEach(c -> {
            if (c instanceof VueDisposable d) d.disposer();
        });
        vueCache.clear();
        controllerCache.clear();
        stateCache.clear();
        cheminVueCourante = null;
    }
}