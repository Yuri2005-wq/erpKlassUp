package org.erpklassup.erpklassup.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Animations réutilisables pour la navigation Accueil ↔ Catégorie dans les
 * Paramètres. Une seule règle : rester discret et ne jamais gêner la lecture.
 */
public final class AnimationsUtil {

    private AnimationsUtil() {}

    /**
     * Feedback de double-clic sur une carte : léger rebond (scale down puis up)
     * avant de déclencher l'action de navigation. Donne au clic une réponse
     * tactile avant que le contenu ne change.
     */
    public static void rebondClic(Node carte, Runnable apresRebond) {
        ScaleTransition presse = new ScaleTransition(Duration.millis(90), carte);
        presse.setToX(0.96);
        presse.setToY(0.96);
        presse.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition relache = new ScaleTransition(Duration.millis(120), carte);
        relache.setToX(1.0);
        relache.setToY(1.0);
        relache.setInterpolator(Interpolator.EASE_OUT);

        SequentialTransition rebond = new SequentialTransition(presse, relache);
        rebond.setOnFinished(e -> apresRebond.run());
        rebond.play();
    }

    /**
     * Transition "drill-in" : accueil -> détail d'une catégorie.
     * Le nouveau contenu apparaît en fondu avec un léger zoom depuis 0.97 —
     * sensation qu'on s'approche de la carte cliquée. L'ancien contenu est
     * retiré une fois la transition terminée.
     */
    public static void entreeCategorie(StackPane container, Node nouveauContenu) {
        nouveauContenu.setOpacity(0);
        nouveauContenu.setScaleX(0.97);
        nouveauContenu.setScaleY(0.97);
        container.getChildren().add(nouveauContenu);

        FadeTransition fondu = new FadeTransition(Duration.millis(220), nouveauContenu);
        fondu.setFromValue(0);
        fondu.setToValue(1);

        ScaleTransition zoom = new ScaleTransition(Duration.millis(220), nouveauContenu);
        zoom.setToX(1);
        zoom.setToY(1);

        ParallelTransition transition = new ParallelTransition(fondu, zoom);
        transition.setOnFinished(e -> container.getChildren().retainAll(nouveauContenu));
        transition.play();
    }

    /**
     * Transition "retour" : catégorie -> accueil. Fondu simple, plus rapide,
     * sans zoom — signale qu'on recule plutôt qu'on avance.
     */
    public static void retourAccueil(StackPane container, Node accueil) {
        accueil.setOpacity(0);
        container.getChildren().add(accueil);

        FadeTransition fondu = new FadeTransition(Duration.millis(160), accueil);
        fondu.setFromValue(0);
        fondu.setToValue(1);
        fondu.setOnFinished(e -> container.getChildren().retainAll(accueil));
        fondu.play();
    }
}
