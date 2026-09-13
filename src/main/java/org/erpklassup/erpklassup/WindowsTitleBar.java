package org.erpklassup.erpklassup;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import javafx.stage.Stage;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Classe utilitaire pour changer la couleur de la barre de titre Windows.
 *
 * ✅ Le HWND de chaque Stage est capturé une seule fois et mis en cache dans
 * hwndCache. On évite ainsi de rappeler GetForegroundWindow() à chaque
 * changement de couleur, ce qui est fragile (mauvais HWND si le focus a
 * transitoirement quitté la fenêtre).
 *
 * ✅ resolveHwnd() vérifie en plus avec User32.IsWindow() que le HWND en
 * cache correspond toujours à une fenêtre native existante. Si un hide()/
 * show() (ou tout autre mécanisme) a recréé le HWND, le cache est
 * automatiquement invalidé et un nouveau HWND est capturé.
 */
public class WindowsTitleBar {

    // Interface JNA pour Dwmapi.dll
    private interface Dwmapi extends StdCallLibrary {
        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);

        int DwmSetWindowAttribute(
                WinDef.HWND hwnd,
                int dwAttribute,
                byte[] pvAttribute,
                int cbAttribute
        );
    }

    // Constantes DWM
    private static final int DWMWA_BORDER_COLOR = 34;
    private static final int DWMWA_CAPTION_COLOR = 35;
    private static final int DWMWA_TEXT_COLOR = 36;

    // ✅ Cache HWND par Stage (WeakHashMap pour ne pas retenir le Stage en mémoire)
    private static final Map<Stage, WinDef.HWND> hwndCache = new WeakHashMap<>();

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name").toLowerCase().contains("win");

    /**
     * Change la couleur de la barre de titre Windows (caption + bordure + texte).
     * @param stage Le stage JavaFX
     * @param hexColor La couleur en hexadécimal (ex: "#16213A")
     */
    public static void setTitleBarColor(Stage stage, String hexColor) {
        if (!IS_WINDOWS || stage == null) {
            return;
        }

        try {
            WinDef.HWND hwnd = resolveHwnd(stage);
            if (hwnd == null) {
                System.out.println("Impossible de résoudre le HWND pour ce stage");
                return;
            }

            int[] rgb = hexToRgb(hexColor);
            int r = rgb[0];
            int g = rgb[1];
            int b = rgb[2];

            applyCaptionColor(hwnd, r, g, b);
            applyBorderColor(hwnd, r, g, b);
            applyTextColor(hwnd, 255, 255, 255); // Texte blanc

        } catch (Exception e) {
            System.err.println("Erreur lors du changement de couleur : " + e.getMessage());
        }
    }

    /**
     * Change uniquement la couleur de la barre de titre (sans la bordure).
     */
    public static void setCaptionColorOnly(Stage stage, String hexColor) {
        if (!IS_WINDOWS || stage == null) {
            return;
        }

        try {
            WinDef.HWND hwnd = resolveHwnd(stage);
            if (hwnd == null) {
                return;
            }

            int[] rgb = hexToRgb(hexColor);
            applyCaptionColor(hwnd, rgb[0], rgb[1], rgb[2]);

        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    /**
     * ✅ À appeler explicitement juste après le tout premier stage.show()
     * (dans HelloApplication.start(), avant de rendre le stage visible via
     * setOpacity). Force la capture et la mise en cache du HWND pendant que
     * le stage est garanti d'être au premier plan.
     */
    public static void enregistrerStage(Stage stage) {
        if (!IS_WINDOWS || stage == null) {
            return;
        }
        resolveHwnd(stage);
    }

    /**
     * ✅ À appeler si le HWND doit être invalidé volontairement
     * (rare — utile seulement si vous savez qu'une recréation native a eu lieu).
     */
    public static void oublierStage(Stage stage) {
        hwndCache.remove(stage);
    }

    private static WinDef.HWND resolveHwnd(Stage stage) {
        WinDef.HWND cached = hwndCache.get(stage);
        if (cached != null && cached.getPointer() != null
                && User32.INSTANCE.IsWindow(cached)) {
            return cached;
        }

        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null || hwnd.getPointer() == null) {
            return null;
        }

        hwndCache.put(stage, hwnd);
        return hwnd;
    }

    private static int[] hexToRgb(String hexColor) {
        String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        return new int[]{r, g, b};
    }

    private static void applyCaptionColor(WinDef.HWND hwnd, int r, int g, int b) {
        byte[] colorBytes = createColorBytes(r, g, b);
        Dwmapi.INSTANCE.DwmSetWindowAttribute(
                hwnd,
                DWMWA_CAPTION_COLOR,
                colorBytes,
                colorBytes.length
        );
    }

    private static void applyBorderColor(WinDef.HWND hwnd, int r, int g, int b) {
        byte[] colorBytes = createColorBytes(r, g, b);
        Dwmapi.INSTANCE.DwmSetWindowAttribute(
                hwnd,
                DWMWA_BORDER_COLOR,
                colorBytes,
                colorBytes.length
        );
    }

    private static void applyTextColor(WinDef.HWND hwnd, int r, int g, int b) {
        byte[] colorBytes = createColorBytes(r, g, b);
        Dwmapi.INSTANCE.DwmSetWindowAttribute(
                hwnd,
                DWMWA_TEXT_COLOR,
                colorBytes,
                colorBytes.length
        );
    }

    private static byte[] createColorBytes(int r, int g, int b) {
        int colorValue = (b << 16) | (g << 8) | r;

        return new byte[] {
                (byte) (colorValue & 0xFF),
                (byte) ((colorValue >> 8) & 0xFF),
                (byte) ((colorValue >> 16) & 0xFF),
                (byte) ((colorValue >> 24) & 0xFF)
        };
    }
}

//v2
// package org.erpklassup.erpklassup;
//
//import com.sun.jna.Native;
//import com.sun.jna.platform.win32.User32;
//import com.sun.jna.platform.win32.WinDef;
//import com.sun.jna.win32.StdCallLibrary;
//import javafx.stage.Stage;
//
///**
// * Classe utilitaire pour changer la couleur de la barre de titre Windows
// */
//public class WindowsTitleBar {
//
//    // Interface JNA pour Dwmapi.dll
//    private interface Dwmapi extends StdCallLibrary {
//        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);
//
//        int DwmSetWindowAttribute(
//                WinDef.HWND hwnd,
//                int dwAttribute,
//                byte[] pvAttribute,
//                int cbAttribute
//        );
//    }
//
//    // Constantes DWM
//    private static final int DWMWA_BORDER_COLOR = 34;
//    private static final int DWMWA_CAPTION_COLOR = 35;
//    private static final int DWMWA_TEXT_COLOR = 36;
//
//    /**
//     * Change la couleur de la barre de titre Windows
//     * @param stage Le stage JavaFX
//     * @param hexColor La couleur en hexadécimal (ex: "#16213A")
//     */
//    public static void setTitleBarColor(Stage stage, String hexColor) {
//        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
//            return;
//        }
//
//        try {
//            // ✅ Récupérer la fenêtre active au premier plan au lieu de chercher par le titre
//            WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
//
//            if (hwnd == null || hwnd.getPointer() == null) {
//                System.out.println("Impossible de trouver la fenêtre active");
//                return;
//            }
//
//            // Convertir la couleur hex en RGB
//            int[] rgb = hexToRgb(hexColor);
//            int r = rgb[0];
//            int g = rgb[1];
//            int b = rgb[2];
//
//            // Appliquer les couleurs
//            applyCaptionColor(hwnd, r, g, b);
//            applyBorderColor(hwnd, r, g, b);
//            applyTextColor(hwnd, 255, 255, 255); // Texte blanc
//
//        } catch (Exception e) {
//            System.err.println("Erreur lors du changement de couleur : " + e.getMessage());
//        }
//    }
//
//    public static void setCaptionColorOnly(Stage stage, String hexColor) {
//        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
//            return;
//        }
//
//        try {
//            WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
//
//            if (hwnd == null || hwnd.getPointer() == null) {
//                return;
//            }
//
//            int[] rgb = hexToRgb(hexColor);
//            applyCaptionColor(hwnd, rgb[0], rgb[1], rgb[2]);
//
//        } catch (Exception e) {
//            System.err.println("Erreur : " + e.getMessage());
//        }
//    }
//
//    private static int[] hexToRgb(String hexColor) {
//        String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
//
//        int r = Integer.parseInt(hex.substring(0, 2), 16);
//        int g = Integer.parseInt(hex.substring(2, 4), 16);
//        int b = Integer.parseInt(hex.substring(4, 6), 16);
//
//        return new int[]{r, g, b};
//    }
//
//    private static void applyCaptionColor(WinDef.HWND hwnd, int r, int g, int b) {
//        byte[] colorBytes = createColorBytes(r, g, b);
//        Dwmapi.INSTANCE.DwmSetWindowAttribute(
//                hwnd,
//                DWMWA_CAPTION_COLOR,
//                colorBytes,
//                colorBytes.length
//        );
//    }
//
//    private static void applyBorderColor(WinDef.HWND hwnd, int r, int g, int b) {
//        byte[] colorBytes = createColorBytes(r, g, b);
//        Dwmapi.INSTANCE.DwmSetWindowAttribute(
//                hwnd,
//                DWMWA_BORDER_COLOR,
//                colorBytes,
//                colorBytes.length
//        );
//    }
//
//    private static void applyTextColor(WinDef.HWND hwnd, int r, int g, int b) {
//        byte[] colorBytes = createColorBytes(r, g, b);
//        Dwmapi.INSTANCE.DwmSetWindowAttribute(
//                hwnd,
//                DWMWA_TEXT_COLOR,
//                colorBytes,
//                colorBytes.length
//        );
//    }
//
//    private static byte[] createColorBytes(int r, int g, int b) {
//        int colorValue = (b << 16) | (g << 8) | r;
//
//        return new byte[] {
//                (byte)(colorValue & 0xFF),
//                (byte)((colorValue >> 8) & 0xFF),
//                (byte)((colorValue >> 16) & 0xFF),
//                (byte)((colorValue >> 24) & 0xFF)
//        };
//    }
//}


//V1
//
// package org.erpklassup.erpklassup;
//
//import com.sun.jna.Native;
//import com.sun.jna.platform.win32.User32;
//import com.sun.jna.platform.win32.WinDef;
//import com.sun.jna.win32.StdCallLibrary;
//import javafx.stage.Stage;
//
///**
// * Classe utilitaire pour changer la couleur de la barre de titre Windows
// */
//public class WindowsTitleBar {
//
//    // Interface JNA pour Dwmapi.dll
//    private interface Dwmapi extends StdCallLibrary {
//        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);
//
//        int DwmSetWindowAttribute(
//                WinDef.HWND hwnd,
//                int dwAttribute,
//                byte[] pvAttribute,
//                int cbAttribute
//        );
//    }
//
//    // Constantes DWM
//    private static final int DWMWA_BORDER_COLOR = 34;
//    private static final int DWMWA_CAPTION_COLOR = 35;
//    private static final int DWMWA_TEXT_COLOR = 36;
//
//    /**
//     * Change la couleur de la barre de titre Windows
//     * @param stage Le stage JavaFX
//     * @param hexColor La couleur en hexadécimal (ex: "#16213A")
//     */
//    public static void setTitleBarColor(Stage stage, String hexColor) {
//        // Vérifier si on est sur Windows
//        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
//            System.out.println("Cette fonctionnalité ne fonctionne que sur Windows");
//            return;
//        }
//
//        try {
//            // Obtenir le handle de la fenêtre Windows
//            WinDef.HWND hwnd = new WinDef.HWND();
//            hwnd.setPointer(User32.INSTANCE.FindWindow(null, stage.getTitle()).getPointer());
//
//            if (hwnd.getPointer() == null) {
//                System.out.println("Impossible de trouver la fenêtre");
//                return;
//            }
//
//            // Convertir la couleur hex en RGB
//            int[] rgb = hexToRgb(hexColor);
//            int r = rgb[0];
//            int g = rgb[1];
//            int b = rgb[2];
//
//            // Appliquer les couleurs
//            applyCaptionColor(hwnd, r, g, b);
//            applyBorderColor(hwnd, r, g, b);
//            applyTextColor(hwnd, 255, 255, 255); // Texte blanc
//
//            System.out.println("Couleur de la barre de titre changée avec succès");
//
//        } catch (Exception e) {
//            System.err.println("Erreur lors du changement de couleur : " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Change uniquement la couleur de la barre de titre (sans la bordure)
//     */
//    public static void setCaptionColorOnly(Stage stage, String hexColor) {
//        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
//            return;
//        }
//
//        try {
//            WinDef.HWND hwnd = new WinDef.HWND();
//            hwnd.setPointer(User32.INSTANCE.FindWindow(null, stage.getTitle()).getPointer());
//
//            if (hwnd.getPointer() == null) {
//                return;
//            }
//
//            int[] rgb = hexToRgb(hexColor);
//            applyCaptionColor(hwnd, rgb[0], rgb[1], rgb[2]);
//
//        } catch (Exception e) {
//            System.err.println("Erreur : " + e.getMessage());
//        }
//    }
//
//    /**
//     * Convertit une couleur hexadécimale en RGB
//     */
//    private static int[] hexToRgb(String hexColor) {
//        String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
//
//        int r = Integer.parseInt(hex.substring(0, 2), 16);
//        int g = Integer.parseInt(hex.substring(2, 4), 16);
//        int b = Integer.parseInt(hex.substring(4, 6), 16);
//
//        return new int[]{r, g, b};
//    }
//
//    /**
//     * Applique la couleur à la barre de titre
//     */
//    private static void applyCaptionColor(WinDef.HWND hwnd, int r, int g, int b) {
//        byte[] colorBytes = createColorBytes(r, g, b);
//        Dwmapi.INSTANCE.DwmSetWindowAttribute(
//                hwnd,
//                DWMWA_CAPTION_COLOR,
//                colorBytes,
//                colorBytes.length
//        );
//    }
//
//    /**
//     * Applique la couleur à la bordure
//     */
//    private static void applyBorderColor(WinDef.HWND hwnd, int r, int g, int b) {
//        byte[] colorBytes = createColorBytes(r, g, b);
//        Dwmapi.INSTANCE.DwmSetWindowAttribute(
//                hwnd,
//                DWMWA_BORDER_COLOR,
//                colorBytes,
//                colorBytes.length
//        );
//    }
//
//    /**
//     * Applique la couleur au texte
//     */
//    private static void applyTextColor(WinDef.HWND hwnd, int r, int g, int b) {
//        byte[] colorBytes = createColorBytes(r, g, b);
//        Dwmapi.INSTANCE.DwmSetWindowAttribute(
//                hwnd,
//                DWMWA_TEXT_COLOR,
//                colorBytes,
//                colorBytes.length
//        );
//    }
//
//    /**
//     * Crée les bytes de couleur au format Windows (0x00BBGGRR)
//     */
//    private static byte[] createColorBytes(int r, int g, int b) {
//        int colorValue = (b << 16) | (g << 8) | r;
//
//        return new byte[] {
//                (byte)(colorValue & 0xFF),           // Rouge
//                (byte)((colorValue >> 8) & 0xFF),    // Vert
//                (byte)((colorValue >> 16) & 0xFF),   // Bleu
//                (byte)((colorValue >> 24) & 0xFF)    // Alpha (0 = opaque)
//        };
//    }
//}